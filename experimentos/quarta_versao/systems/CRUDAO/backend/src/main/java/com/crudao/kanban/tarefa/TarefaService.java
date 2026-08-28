package com.crudao.kanban.tarefa;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.*;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import com.crudao.kanban.tarefa.dto.CriarTarefaResponse;
import com.crudao.kanban.tarefa.dto.EditarTarefaRequest;
import com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;
    private final TarefaAuditoriaRepository tarefaAuditoriaRepository;
    private final TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;
    private final ProjetoRepository projetoRepository;
    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
    private final TransicaoRepository transicaoRepository;
    private final RaiaRepository raiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissaoGuard permissaoGuard;

    @Transactional
    public CriarTarefaResponse criarTarefa(UUID projetoId, CriarTarefaRequest request) {
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "tarefa:gerenciar");

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        List<Workflow> workflows = workflowRepository.findByProjetoId(projetoId);
        if (workflows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Projeto não possui workflow configurado");
        }
        Workflow workflow = workflows.get(0);

        List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId());
        if (etapas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Workflow não possui etapas configuradas");
        }
        Etapa etapaInicial = etapas.get(0);

        Raia raia;
        if (request.getRaiaId() != null) {
            raia = raiaRepository.findById(request.getRaiaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Raia não encontrada"));
            if (raia.getProjeto() != null && !raia.getProjeto().getId().equals(projetoId)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Raia não pertence ao projeto informado");
            }
        } else {
            List<Raia> raiasProjeto = raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId);
            if (!raiasProjeto.isEmpty()) {
                raia = raiasProjeto.get(0);
            } else {
                List<Raia> raiasGlobais = raiaRepository.findByProjetoIdIsNullOrderByOrdemAsc();
                if (!raiasGlobais.isEmpty()) {
                    raia = raiasGlobais.get(0);
                } else {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nenhuma raia disponível");
                }
            }
        }

        Usuario responsavel = null;
        if (request.getResponsavelId() != null) {
            responsavel = usuarioRepository.findById(request.getResponsavelId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
        }

        Usuario criadoPor = UsuarioAutenticadoHolder.get();
        if (criadoPor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projeto);
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapaInicial);
        tarefa.setRaia(raia);
        tarefa.setTitulo(request.getTitulo());
        tarefa.setDescricaoEscopo(request.getDescricaoEscopo());
        tarefa.setResponsavel(responsavel);
        tarefa.setCriadoPor(criadoPor);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);
        tarefa.setImpedidaDesde(null);

        tarefa = tarefaRepository.save(tarefa);

        TarefaEtapaHistorico hist = new TarefaEtapaHistorico();
        hist.setTarefa(tarefa);
        hist.setEtapa(etapaInicial);
        hist.setEntradaEm(Instant.now());
        hist.setSaidaEm(null);
        tarefaEtapaHistoricoRepository.save(hist);

        return new CriarTarefaResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                etapaInicial.getId(),
                raia.getId(),
                responsavel != null ? responsavel.getId() : null
        );
    }

    /**
     * TASK-04.2: Mover tarefa entre etapas com validação de transição, permissões e atualização de histórico.
     * RFs: RF-002 (transições), RF-003 (congelamento), RF-006 (lead-time), RF-012 (desfinalizar).
     */
    @Transactional
    public void mover(UUID tarefaId, MoverTarefaRequest request) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        // RN-CB-003: Projeto finalizado bloqueia movimentação
        permissaoGuard.exigirProjetoAtivo(tarefa.getProjeto().getId());

        Etapa etapaDestino = etapaRepository.findById(request.getEtapaDestinoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa destino não encontrada"));

        Etapa etapaAtual = tarefa.getEtapaAtual();

        // RN-003: Validar transição configurada
        List<Transicao> transicoes = transicaoRepository.findByEtapaOrigemId(etapaAtual.getId());
        boolean transicaoExiste = transicoes.stream()
                .anyMatch(t -> t.getEtapaDestino().getId().equals(etapaDestino.getId()));
        if (!transicaoExiste) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição não configurada");
        }

        // RN-011: Validar permissão se destino é etapa final
        if (etapaDestino.getEtapaFinal()) {
            permissaoGuard.exigir(tarefa.getProjeto().getId(), "tarefa:finalizar");
        }

        // RN-004/RN-011: Validar permissão se origem é etapa final (desfinalizar)
        if (etapaAtual.getEtapaFinal()) {
            permissaoGuard.exigir(tarefa.getProjeto().getId(), "tarefa:finalizar");
        }

        // Fechar histórico da etapa atual
        TarefaEtapaHistorico historicoAtual = tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId)
                .stream()
                .filter(h -> h.getSaidaEm() == null)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Histórico de etapa não encontrado"));
        historicoAtual.setSaidaEm(Instant.now());
        tarefaEtapaHistoricoRepository.save(historicoAtual);

        // Abrir novo histórico para etapa destino
        TarefaEtapaHistorico novoHistorico = new TarefaEtapaHistorico();
        novoHistorico.setTarefa(tarefa);
        novoHistorico.setEtapa(etapaDestino);
        novoHistorico.setEntradaEm(Instant.now());
        novoHistorico.setSaidaEm(null);
        tarefaEtapaHistoricoRepository.save(novoHistorico);

        // Atualizar tarefa: etapa atual e marcar como iniciada se saiu da 1ª etapa
        tarefa.setEtapaAtual(etapaDestino);
        if (etapaAtual.getOrdem() == 1) {
            tarefa.setIniciada(true);
        }
        tarefa.setAtualizadoEm(Instant.now());
        tarefaRepository.save(tarefa);

        // Registrar auditoria
        TarefaAuditoria auditoria = new TarefaAuditoria();
        auditoria.setTarefa(tarefa);
        auditoria.setAutor(UsuarioAutenticadoHolder.get());
        auditoria.setCampo("etapa");
        auditoria.setValorAnterior(etapaAtual.getNome());
        auditoria.setValorNovo(etapaDestino.getNome());
        auditoria.setDataHora(Instant.now());
        tarefaAuditoriaRepository.save(auditoria);
    }

    /**
     * TASK-04.2: Editar tarefa com congelamento de campos estruturais pós-início e validação RN-012.
     * RF-003: Campos estruturais (`titulo`, `descricaoEscopo`) congelados quando `iniciada=true`.
     * RN-012: Dev só se autoatribui; product_owner/project_admin/admin atribuem livremente.
     *
     * Entrada validada via Bean Validation (@NotBlank, @Size) em EditarTarefaRequest.
     */
    @Transactional
    public void editar(UUID tarefaId, EditarTarefaRequest request) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        Usuario usuarioLogado = UsuarioAutenticadoHolder.get();

        // Se tarefa já iniciada, bloquear campos estruturais
        if (tarefa.isIniciada()) {
            if (request.getTitulo() != null || request.getDescricaoEscopo() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Campos estruturais congelados após início");
            }
        }

        // Processar atribuição com RN-012
        if (request.getResponsavelId() != null) {
            UUID novoResponsavelId = request.getResponsavelId();
            Usuario novoResponsavel = usuarioRepository.findById(novoResponsavelId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

            // Validação RN-012: dev só se autoatribui
            permissaoGuard.validarAutoatribuicaoRN012(
                    tarefa.getProjeto().getId(),
                    usuarioLogado.getId(),
                    novoResponsavelId
            );

            Usuario responsavelAnterior = tarefa.getResponsavel();
            tarefa.setResponsavel(novoResponsavel);

            // Registrar auditoria de responsável
            TarefaAuditoria auditoria = new TarefaAuditoria();
            auditoria.setTarefa(tarefa);
            auditoria.setAutor(usuarioLogado);
            auditoria.setCampo("responsavel");
            auditoria.setValorAnterior(responsavelAnterior != null ? responsavelAnterior.getId().toString() : null);
            auditoria.setValorNovo(novoResponsavel.getId().toString());
            auditoria.setDataHora(Instant.now());
            tarefaAuditoriaRepository.save(auditoria);
        }

        // Processar edição de título e descrição
        if (request.getTitulo() != null) {
            String novoTitulo = request.getTitulo();
            String tituloAnterior = tarefa.getTitulo();
            tarefa.setTitulo(novoTitulo);

            TarefaAuditoria auditoria = new TarefaAuditoria();
            auditoria.setTarefa(tarefa);
            auditoria.setAutor(usuarioLogado);
            auditoria.setCampo("titulo");
            auditoria.setValorAnterior(tituloAnterior);
            auditoria.setValorNovo(novoTitulo);
            auditoria.setDataHora(Instant.now());
            tarefaAuditoriaRepository.save(auditoria);
        }

        if (request.getDescricaoEscopo() != null) {
            String novaDescricao = request.getDescricaoEscopo();
            String descricaoAnterior = tarefa.getDescricaoEscopo();
            tarefa.setDescricaoEscopo(novaDescricao);

            TarefaAuditoria auditoria = new TarefaAuditoria();
            auditoria.setTarefa(tarefa);
            auditoria.setAutor(usuarioLogado);
            auditoria.setCampo("descricaoEscopo");
            auditoria.setValorAnterior(descricaoAnterior);
            auditoria.setValorNovo(novaDescricao);
            auditoria.setDataHora(Instant.now());
            tarefaAuditoriaRepository.save(auditoria);
        }

        tarefa.setAtualizadoEm(Instant.now());
        tarefaRepository.save(tarefa);
    }

    /**
     * TASK-04.2: Obter detalhe da tarefa com cálculo de lead-time por etapa e tempo total de impedimento.
     * RF-006: Lead-time calculado a partir de `TarefaEtapaHistorico`, incluindo etapa em andamento.
     * RN-001: Lead-time por etapa = saidaEm - entradaEm (ou now() - entradaEm se em andamento).
     * RN-002: Tempo de impedimento acumulado a partir de `TarefaImpedimentoHistorico`.
     */
    @Transactional(readOnly = true)
    public TarefaDetalheResponse obterComLeadTime(UUID tarefaId) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        List<TarefaEtapaHistorico> historicoEtapas = tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId);
        List<TarefaImpedimentoHistorico> historicoImpedimento = tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId);

        // Calcular lead-time por etapa
        List<TarefaDetalheResponse.HistoricoEtapaDTO> historicoETAs = new ArrayList<>();
        for (TarefaEtapaHistorico hist : historicoEtapas) {
            Instant fim = hist.getSaidaEm() != null ? hist.getSaidaEm() : Instant.now();
            long leadTimeSegundos = java.time.temporal.ChronoUnit.SECONDS.between(hist.getEntradaEm(), fim);

            TarefaDetalheResponse.HistoricoEtapaDTO etapaDTO = TarefaDetalheResponse.HistoricoEtapaDTO.builder()
                    .etapaId(hist.getEtapa().getId())
                    .etapaNome(hist.getEtapa().getNome())
                    .leadTimeSegundos(leadTimeSegundos)
                    .build();
            historicoETAs.add(etapaDTO);
        }

        // Calcular tempo total de impedimento (RN-002)
        long tempoImpedimentoTotal = 0;
        for (TarefaImpedimentoHistorico imp : historicoImpedimento) {
            Instant desmarca = imp.getDesmarcadoEm() != null ? imp.getDesmarcadoEm() : Instant.now();
            long tempoImpedimento = java.time.temporal.ChronoUnit.SECONDS.between(imp.getMarcadoEm(), desmarca);
            tempoImpedimentoTotal += tempoImpedimento;
        }

        return TarefaDetalheResponse.builder()
                .id(tarefa.getId())
                .titulo(tarefa.getTitulo())
                .descricaoEscopo(tarefa.getDescricaoEscopo())
                .etapaAtualId(tarefa.getEtapaAtual().getId())
                .raiaId(tarefa.getRaia().getId())
                .responsavelId(tarefa.getResponsavel() != null ? tarefa.getResponsavel().getId() : null)
                .iniciada(tarefa.isIniciada())
                .impedida(tarefa.isImpedida())
                .historicoEtapas(historicoETAs)
                .tempoImpedimentoTotalSegundos(tempoImpedimentoTotal)
                .build();
    }
}

