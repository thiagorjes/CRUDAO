package com.crudao.kanban.tarefa;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaAuditoria;
import com.crudao.kanban.domain.tarefa.TarefaAuditoriaRepository;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistorico;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Criação de card pelo board (RF-018, RN-CB-001 a RN-CB-005 — {@code contracts/tarefas.md},
 * TASK-04.1) e engine de movimentação/edição/detalhe (RF-002, RF-003, RF-006, RF-012, TASK-04.2).
 *
 * <p>Criação exige {@code tarefa:gerenciar} e projeto {@code ATIVO} (RN-CB-003). Etapa inicial é
 * sempre a de menor {@code ordem} do workflow do projeto; sem {@code raiaId} informado, usa a
 * primeira raia do projeto (menor {@code ordem}) ou a raia default global (RN-CB-005); sem {@code
 * responsavelId} informado, card fica sem responsável (RN-CB-004).
 *
 * <p>Mover exige apenas vínculo com o projeto (qualquer membro pode arrastar o card), mas exige
 * adicionalmente {@code tarefa:finalizar} quando o destino ou a origem é etapa final (RN-004,
 * RN-011). Editar campos estruturais ({@code titulo}/{@code descricaoEscopo}) exige {@code
 * tarefa:gerenciar} e é bloqueado quando a tarefa já {@code iniciada} (congelamento). Troca de
 * responsável segue RN-012: sem {@code tarefa:gerenciar}, só autoatribuição; com a permissão,
 * atribuição livre a qualquer usuário vinculado ao projeto.
 */
@Service
public class TarefaService {

    private static final String PERMISSAO_GERENCIAR = "tarefa:gerenciar";
    private static final String PERMISSAO_FINALIZAR = "tarefa:finalizar";

    private final TarefaRepository tarefaRepository;
    private final TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;
    private final TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;
    private final TarefaAuditoriaRepository tarefaAuditoriaRepository;
    private final ProjetoRepository projetoRepository;
    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
    private final TransicaoRepository transicaoRepository;
    private final RaiaRepository raiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    private final PermissaoGuard permissaoGuard;

    public TarefaService(
            TarefaRepository tarefaRepository,
            TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository,
            TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository,
            TarefaAuditoriaRepository tarefaAuditoriaRepository,
            ProjetoRepository projetoRepository,
            WorkflowRepository workflowRepository,
            EtapaRepository etapaRepository,
            TransicaoRepository transicaoRepository,
            RaiaRepository raiaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioProjetoPapelRepository usuarioProjetoPapelRepository,
            PermissaoGuard permissaoGuard) {
        this.tarefaRepository = tarefaRepository;
        this.tarefaEtapaHistoricoRepository = tarefaEtapaHistoricoRepository;
        this.tarefaImpedimentoHistoricoRepository = tarefaImpedimentoHistoricoRepository;
        this.tarefaAuditoriaRepository = tarefaAuditoriaRepository;
        this.projetoRepository = projetoRepository;
        this.workflowRepository = workflowRepository;
        this.etapaRepository = etapaRepository;
        this.transicaoRepository = transicaoRepository;
        this.raiaRepository = raiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioProjetoPapelRepository = usuarioProjetoPapelRepository;
        this.permissaoGuard = permissaoGuard;
    }

    @Transactional
    public TarefaResponse criar(UUID projetoId, CriarTarefaRequest request) {
        permissaoGuard.exigir(projetoId, PERMISSAO_GERENCIAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);

        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "título é obrigatório");
        }

        Workflow workflow =
                workflowRepository.findByProjetoId(projetoId).stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNPROCESSABLE_ENTITY, "projeto sem workflow configurado"));

        Etapa etapaInicial = etapaMenorOrdem(workflow.getId());
        Raia raia = resolverRaia(projetoId, request.raiaId());
        Usuario responsavel = resolverResponsavelVinculado(projetoId, request.responsavelId());
        Usuario criadoPor = UsuarioAutenticadoHolder.get();

        OffsetDateTime agora = OffsetDateTime.now();
        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projetoRepository.getReferenceById(projetoId));
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapaInicial);
        tarefa.setRaia(raia);
        tarefa.setTitulo(request.titulo());
        tarefa.setDescricaoEscopo(request.descricaoEscopo());
        tarefa.setResponsavel(responsavel);
        tarefa.setCriadoPor(criadoPor);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);
        tarefa.setCriadoEm(agora);
        tarefa.setAtualizadoEm(agora);
        tarefa = tarefaRepository.save(tarefa);

        TarefaEtapaHistorico historico = new TarefaEtapaHistorico();
        historico.setTarefa(tarefa);
        historico.setEtapa(etapaInicial);
        historico.setEntradaEm(agora);
        historico.setSaidaEm(null);
        tarefaEtapaHistoricoRepository.save(historico);

        return toResponse(tarefa);
    }

    /**
     * Move a tarefa entre etapas (drag-and-drop do board, RF-002). Validações na ordem do
     * contrato: (1) transição configurada; (2) {@code tarefa:finalizar} se destino é etapa final
     * (RN-011); (3) {@code tarefa:finalizar} se origem é etapa final — "desfinalizar" (RN-004,
     * RN-011, RF-012).
     */
    @Transactional
    public TarefaResponse mover(UUID tarefaId, MoverTarefaRequest request) {
        Tarefa tarefa = buscarTarefa(tarefaId);
        UUID projetoId = tarefa.getProjeto().getId();
        exigirProjetoAtivoParaTarefa(projetoId);
        exigirMembro(projetoId);

        if (request.etapaDestinoId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "etapa de destino é obrigatória");
        }

        Etapa etapaAtual = tarefa.getEtapaAtual();
        Etapa etapaDestino = buscarEtapa(request.etapaDestinoId());
        if (!etapaDestino.getWorkflow().getId().equals(etapaAtual.getWorkflow().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "etapa de destino pertence a outro workflow");
        }

        boolean transicaoConfigurada =
                transicaoRepository.findByEtapaOrigemId(etapaAtual.getId()).stream()
                        .anyMatch(t -> t.getEtapaDestino().getId().equals(etapaDestino.getId()));
        if (!transicaoConfigurada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "transição não configurada");
        }

        if (etapaDestino.isEtapaFinal()) {
            permissaoGuard.exigir(projetoId, PERMISSAO_FINALIZAR);
        }
        if (etapaAtual.isEtapaFinal()) {
            permissaoGuard.exigir(projetoId, PERMISSAO_FINALIZAR);
        }

        OffsetDateTime agora = OffsetDateTime.now();

        TarefaEtapaHistorico historicoAtual =
                tarefaEtapaHistoricoRepository
                        .findByTarefaIdAndSaidaEmIsNull(tarefaId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.CONFLICT, "tarefa sem histórico de etapa em andamento"));
        historicoAtual.setSaidaEm(agora);
        tarefaEtapaHistoricoRepository.save(historicoAtual);

        TarefaEtapaHistorico novoHistorico = new TarefaEtapaHistorico();
        novoHistorico.setTarefa(tarefa);
        novoHistorico.setEtapa(etapaDestino);
        novoHistorico.setEntradaEm(agora);
        tarefaEtapaHistoricoRepository.save(novoHistorico);

        Etapa primeiraEtapa = etapaMenorOrdem(etapaAtual.getWorkflow().getId());
        if (!tarefa.isIniciada() && etapaAtual.getId().equals(primeiraEtapa.getId())) {
            tarefa.setIniciada(true);
        }

        String etapaAnteriorId = etapaAtual.getId().toString();
        tarefa.setEtapaAtual(etapaDestino);
        tarefa.setAtualizadoEm(agora);
        tarefa = tarefaRepository.save(tarefa);

        registrarAuditoria(
                tarefa, UsuarioAutenticadoHolder.get(), "etapa", etapaAnteriorId, etapaDestino.getId().toString(), agora);

        return toResponse(tarefa);
    }

    /**
     * Edição de tarefa (RF-003). Com a tarefa {@code iniciada}, {@code titulo}/{@code
     * descricaoEscopo} ficam congelados (409) — apenas {@code responsavelId} é aceito, sujeito à
     * RN-012.
     */
    @Transactional
    public TarefaResponse editar(UUID tarefaId, EditarTarefaRequest request) {
        Tarefa tarefa = buscarTarefa(tarefaId);
        UUID projetoId = tarefa.getProjeto().getId();
        exigirProjetoAtivoParaTarefa(projetoId);
        exigirMembro(projetoId);

        Usuario autor = UsuarioAutenticadoHolder.get();
        OffsetDateTime agora = OffsetDateTime.now();

        boolean editandoCampoEstrutural = request.titulo() != null || request.descricaoEscopo() != null;
        if (editandoCampoEstrutural) {
            if (tarefa.isIniciada()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "tarefa iniciada — título/descrição congelados");
            }
            permissaoGuard.exigir(projetoId, PERMISSAO_GERENCIAR);

            if (request.titulo() != null) {
                if (request.titulo().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "título é obrigatório");
                }
                if (!request.titulo().equals(tarefa.getTitulo())) {
                    registrarAuditoria(tarefa, autor, "titulo", tarefa.getTitulo(), request.titulo(), agora);
                    tarefa.setTitulo(request.titulo());
                }
            }
            if (request.descricaoEscopo() != null) {
                tarefa.setDescricaoEscopo(request.descricaoEscopo());
            }
        }

        if (request.removerResponsavel()) {
            // Desatribuir é ação de gestão — dev pode "puxar" para si (RN-012), mas não esvaziar
            // o campo (achado de code review, agent QA — antes indistinguível de "não enviado").
            permissaoGuard.exigir(projetoId, PERMISSAO_GERENCIAR);
            UUID responsavelAnteriorId = tarefa.getResponsavel() == null ? null : tarefa.getResponsavel().getId();
            if (responsavelAnteriorId != null) {
                registrarAuditoria(tarefa, autor, "responsavel", responsavelAnteriorId, null, agora);
                tarefa.setResponsavel(null);
            }
        } else if (request.responsavelId() != null) {
            boolean podeGerenciar = permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR);
            if (!podeGerenciar && !request.responsavelId().equals(autor.getId())) {
                throw new AccessDeniedException("Acesso negado");
            }

            Usuario novoResponsavel = resolverResponsavelVinculado(projetoId, request.responsavelId());
            UUID responsavelAnteriorId = tarefa.getResponsavel() == null ? null : tarefa.getResponsavel().getId();
            if (!request.responsavelId().equals(responsavelAnteriorId)) {
                registrarAuditoria(tarefa, autor, "responsavel", responsavelAnteriorId, request.responsavelId(), agora);
                tarefa.setResponsavel(novoResponsavel);
            }
        }

        tarefa.setAtualizadoEm(agora);
        tarefa = tarefaRepository.save(tarefa);
        return toResponse(tarefa);
    }

    /** Detalhe da tarefa (RF-003, TL-04), com lead-time por etapa (RF-006, RN-001). */
    public TarefaDetalheResponse detalhe(UUID tarefaId) {
        Tarefa tarefa = buscarTarefa(tarefaId);
        UUID projetoId = tarefa.getProjeto().getId();
        exigirMembro(projetoId);

        OffsetDateTime agora = OffsetDateTime.now();

        List<HistoricoEtapaResponse> historicoEtapas =
                tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEm(tarefaId).stream()
                        .map(
                                h -> {
                                    OffsetDateTime fim = h.getSaidaEm() != null ? h.getSaidaEm() : agora;
                                    long leadTimeSegundos = Duration.between(h.getEntradaEm(), fim).getSeconds();
                                    return new HistoricoEtapaResponse(
                                            h.getEtapa().getId(), h.getEntradaEm(), h.getSaidaEm(), leadTimeSegundos);
                                })
                        .toList();

        long tempoImpedimentoTotalSegundos =
                tarefaImpedimentoHistoricoRepository.findByTarefaId(tarefaId).stream()
                        .mapToLong(i -> tempoImpedimento(i, agora))
                        .sum();

        UUID responsavelId = tarefa.getResponsavel() == null ? null : tarefa.getResponsavel().getId();
        return new TarefaDetalheResponse(
                tarefa.getId(),
                tarefa.getTitulo(),
                tarefa.getDescricaoEscopo(),
                tarefa.getEtapaAtual().getId(),
                tarefa.getRaia().getId(),
                responsavelId,
                tarefa.isIniciada(),
                tarefa.isImpedida(),
                tarefa.getImpedidaDesde(),
                historicoEtapas,
                tempoImpedimentoTotalSegundos);
    }

    private long tempoImpedimento(TarefaImpedimentoHistorico impedimento, OffsetDateTime agora) {
        OffsetDateTime fim = impedimento.getDesmarcadoEm() != null ? impedimento.getDesmarcadoEm() : agora;
        return Duration.between(impedimento.getMarcadoEm(), fim).getSeconds();
    }

    private void registrarAuditoria(
            Tarefa tarefa, Usuario autor, String campo, Object valorAnterior, Object valorNovo, OffsetDateTime agora) {
        TarefaAuditoria auditoria = new TarefaAuditoria();
        auditoria.setTarefa(tarefa);
        auditoria.setAutor(autor);
        auditoria.setCampo(campo);
        auditoria.setValorAnterior(valorAnterior == null ? null : valorAnterior.toString());
        auditoria.setValorNovo(valorNovo == null ? null : valorNovo.toString());
        auditoria.setDataHora(agora);
        tarefaAuditoriaRepository.save(auditoria);
    }

    private void exigirMembro(UUID projetoId) {
        if (!permissaoGuard.membro(projetoId)) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    /**
     * RN-015 (projeto finalizado = somente leitura), mas traduzido para {@code 409} — não {@code
     * 403} como o guard genérico ({@link PermissaoGuard#exigirProjetoAtivo}) devolve — porque
     * {@code contracts/tarefas.md} documenta explicitamente esse caso como "estado incompatível"
     * (409) para os endpoints de tarefa (achado de code review, agent QA, TASK-04.2).
     */
    private void exigirProjetoAtivoParaTarefa(UUID projetoId) {
        try {
            permissaoGuard.exigirProjetoAtivo(projetoId);
        } catch (AccessDeniedException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "projeto finalizado — somente leitura");
        }
    }

    private Tarefa buscarTarefa(UUID id) {
        return tarefaRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tarefa não encontrada"));
    }

    private Etapa buscarEtapa(UUID id) {
        return etapaRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "etapa não encontrada"));
    }

    private Etapa etapaMenorOrdem(UUID workflowId) {
        List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdem(workflowId);
        if (etapas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "workflow sem etapas configuradas");
        }
        return etapas.get(0);
    }

    private Raia resolverRaia(UUID projetoId, UUID raiaId) {
        if (raiaId != null) {
            Raia raia =
                    raiaRepository
                            .findById(raiaId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "raia não encontrada"));
            boolean pertenceAoProjeto = raia.getProjeto() == null || raia.getProjeto().getId().equals(projetoId);
            if (!pertenceAoProjeto) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "raia pertence a outro projeto");
            }
            return raia;
        }

        List<Raia> raiasDoProjeto = raiaRepository.findByProjetoIdOrderByOrdem(projetoId);
        if (!raiasDoProjeto.isEmpty()) {
            return raiasDoProjeto.get(0);
        }
        return raiaRepository.findByProjetoIdIsNull().stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.UNPROCESSABLE_ENTITY, "nenhuma raia default configurada"));
    }

    /**
     * Valida que o usuário existe e está vinculado ao projeto (RN-012, gap de integridade
     * registrado em TASK-04.1 e fechado aqui) — usado tanto na criação quanto na troca de
     * responsável via {@link #editar}.
     */
    private Usuario resolverResponsavelVinculado(UUID projetoId, UUID responsavelId) {
        if (responsavelId == null) {
            return null;
        }
        Usuario usuario =
                usuarioRepository
                        .findById(responsavelId)
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "responsável inválido"));
        boolean vinculado =
                !usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(responsavelId, projetoId).isEmpty();
        if (!vinculado) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "responsável não vinculado ao projeto");
        }
        return usuario;
    }

    private TarefaResponse toResponse(Tarefa tarefa) {
        UUID responsavelId = tarefa.getResponsavel() == null ? null : tarefa.getResponsavel().getId();
        return new TarefaResponse(
                tarefa.getId(), tarefa.getTitulo(), tarefa.getEtapaAtual().getId(), tarefa.getRaia().getId(), responsavelId);
    }
}
