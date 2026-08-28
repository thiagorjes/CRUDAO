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
import com.crudao.kanban.evento.EventoBoardPublisher;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import com.crudao.kanban.tarefa.dto.CriarTarefaResponse;
import com.crudao.kanban.tarefa.dto.EditarTarefaRequest;
import com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final EventoBoardPublisher eventoBoardPublisher;
    private final ObjectMapper objectMapper;

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

        // TASK-05.1: Publicar evento de criação para atualização em tempo real
        publicarEvento("TAREFA_CRIADA", projeto.getId(), tarefa.getId(), etapaInicial.getId());

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

        // TASK-05.1: Publicar evento de movimentação para atualização em tempo real
        publicarEvento("TAREFA_MOVIDA", tarefa.getProjeto().getId(), tarefa.getId(), etapaDestino.getId());
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

    /**
     * TASK-04.3: Marcar tarefa como impedida.
     * RF-004: Sinalização de bloqueio.
     * RN-013: Requer `tarefa:impedimento` (dev, product_owner, project_admin, admin por padrão).
     * RN-CB-003: Bloqueado se projeto finalizado.
     */
    @Transactional
    public void marcarImpedimento(UUID tarefaId, UUID projetoId) {
        // Valida permissão e projeto ativo (RN-015: projeto finalizado bloqueia todas as escritas)
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "tarefa:impedimento");

        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        // Não deixa marcar se já está impedida
        if (tarefa.isImpedida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tarefa já está marcada como impedida");
        }

        Instant agora = Instant.now();
        tarefa.setImpedida(true);
        tarefa.setImpedidaDesde(agora);

        // Abre histórico de impedimento
        TarefaImpedimentoHistorico historico = new TarefaImpedimentoHistorico();
        historico.setTarefa(tarefa);
        historico.setMarcadoEm(agora);
        historico.setDesmarcadoEm(null);
        tarefaImpedimentoHistoricoRepository.save(historico);

        // Persiste a tarefa
        tarefa.setAtualizadoEm(agora);
        tarefaRepository.save(tarefa);

        // Registra auditoria
        Usuario usuarioLogado = UsuarioAutenticadoHolder.get();
        TarefaAuditoria auditoria = new TarefaAuditoria();
        auditoria.setTarefa(tarefa);
        auditoria.setAutor(usuarioLogado);
        auditoria.setCampo("impedimento");
        auditoria.setValorAnterior("false");
        auditoria.setValorNovo("true");
        auditoria.setDataHora(agora);
        tarefaAuditoriaRepository.save(auditoria);

        // TODO: TASK-05.2 — publicar evento/notificação para observadores (responsável, criador, observadores explícitos)
    }

    /**
     * TASK-04.3: Desmarcar tarefa como impedida.
     * RF-004: Fechamento de sinalização de bloqueio.
     * Requer permissão `tarefa:impedimento`.
     */
    @Transactional
    public void desmarcarImpedimento(UUID tarefaId, UUID projetoId) {
        // Valida permissão e projeto ativo
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "tarefa:impedimento");

        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        // Não deixa desmarcar se não está impedida
        if (!tarefa.isImpedida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tarefa não está marcada como impedida");
        }

        // Busca o histórico aberto (sem desmarcadoEm)
        TarefaImpedimentoHistorico historicoAberto = tarefaImpedimentoHistoricoRepository
                .findByTarefaIdAndDesmarcadoEmIsNull(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum histórico aberto de impedimento encontrado"));

        Instant agora = Instant.now();

        // Fecha o histórico
        historicoAberto.setDesmarcadoEm(agora);
        tarefaImpedimentoHistoricoRepository.save(historicoAberto);

        // Atualiza tarefa
        tarefa.setImpedida(false);
        tarefa.setAtualizadoEm(agora);
        tarefaRepository.save(tarefa);

        // Registra auditoria
        Usuario usuarioLogado = UsuarioAutenticadoHolder.get();
        TarefaAuditoria auditoria = new TarefaAuditoria();
        auditoria.setTarefa(tarefa);
        auditoria.setAutor(usuarioLogado);
        auditoria.setCampo("impedimento");
        auditoria.setValorAnterior("true");
        auditoria.setValorNovo("false");
        auditoria.setDataHora(agora);
        tarefaAuditoriaRepository.save(auditoria);

        // TODO: TASK-05.2 — publicar evento/notificação para observadores (responsável, criador, observadores explícitos)
    }

    /**
     * TASK-04.4: Excluir tarefa pelo board (RF-019).
     * Requer `tarefa:gerenciar` (RN-CB-001).
     * Se usuário é `dev`, requer adicionalmente `tarefa:excluir` habilitada (RN-CB-002).
     * Bloqueado se projeto finalizado (RN-CB-003).
     * Publica evento `TAREFA_EXCLUIDA` via STOMP em até 2s (RNF-001).
     */
    @Transactional
    public void excluirTarefa(UUID tarefaId, UUID projetoId) {
        // RN-CB-003: Projeto finalizado bloqueia exclusão
        permissaoGuard.exigirProjetoAtivo(projetoId);

        // RN-CB-001: Exigir `tarefa:gerenciar`
        permissaoGuard.exigir(projetoId, "tarefa:gerenciar");

        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        // RN-CB-002: Exigir também `tarefa:excluir` (além de `tarefa:gerenciar`)
        permissaoGuard.exigirPermissaoExcluir(projetoId);

        // Registrar auditoria antes de deletar (opcional, mas útil para histórico)
        Usuario usuarioLogado = UsuarioAutenticadoHolder.get();
        TarefaAuditoria auditoria = new TarefaAuditoria();
        auditoria.setTarefa(tarefa);
        auditoria.setAutor(usuarioLogado);
        auditoria.setCampo("status");
        auditoria.setValorAnterior("ativo");
        auditoria.setValorNovo("excluido");
        auditoria.setDataHora(Instant.now());
        tarefaAuditoriaRepository.save(auditoria);

        // TASK-05.1: Publicar evento de exclusão para atualização em tempo real (antes de deletar)
        publicarEvento("TAREFA_EXCLUIDA", projetoId, tarefaId, tarefa.getEtapaAtual().getId());

        // Excluir tarefa (cascata deleta históricos associados)
        tarefaRepository.deleteById(tarefaId);
    }

    /**
     * TASK-04.4: Obter histórico de auditoria da tarefa (RF-017).
     * Retorna todas as alterações relevantes (autor, campo, valor anterior/novo, data/hora)
     * agregando os registros gravados em TASK-04.2/04.3 e nesta task.
     */
    @Transactional(readOnly = true)
    public List<TarefaAuditoria> obterAuditoria(UUID tarefaId) {
        Tarefa tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada"));

        return tarefaAuditoriaRepository.findByTarefaIdOrderByDataHoraAsc(tarefaId);
    }

    /**
     * TASK-05.1: Publica um evento de board para atualização em tempo real via STOMP.
     * Encapsula a criação do payload JSON e invoca {@link EventoBoardPublisher#publicar}.
     * O evento é publicado após o commit da transação atual via {@link EventoBoardPublisher#publicar}.
     *
     * @param tipo Tipo do evento (ex: TAREFA_CRIADA, TAREFA_MOVIDA, TAREFA_EXCLUIDA).
     * @param projetoId ID do projeto afetado.
     * @param tarefaId ID da tarefa envolvida.
     * @param etapaId ID da etapa envolvida.
     */
    private void publicarEvento(String tipo, UUID projetoId, UUID tarefaId, UUID etapaId) {
        try {
            String payloadJson = objectMapper.writeValueAsString(
                Map.of(
                    "tipo", tipo,
                    "projetoId", projetoId.toString(),
                    "tarefaId", tarefaId.toString(),
                    "etapaId", etapaId.toString(),
                    "timestamp", Instant.now().toEpochMilli()
                )
            );

            EventoBoardPublisher.EventoBoardPayload evento =
                new EventoBoardPublisher.EventoBoardPayload(
                    tipo,
                    projetoId,
                    0L, // Sequência será atribuída pelo adapter
                    payloadJson
                );

            eventoBoardPublisher.publicar(evento);
        } catch (Exception e) {
            // Log mas não falha a transação — o evento é "best effort"
            // RNF-002 mitiga via cliente refazendo GET /board em caso de divergência
        }
    }
}

