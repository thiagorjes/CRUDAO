package com.crudao.kanban.workflow;

import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD de {@link Workflow}, {@link Etapa} (incl. reordenação) e {@link Transicao} (RF-002, RF-009,
 * RF-010).
 *
 * <p>Todos os endpoints exigem {@code workflow:administrar} no projeto e projeto {@code ATIVO}
 * (RN-015), conforme {@code contracts/workflows.md}.
 */
@Service
public class WorkflowService {

    private static final String PERMISSAO_ADMINISTRAR = "workflow:administrar";

    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
    private final TransicaoRepository transicaoRepository;
    private final ProjetoRepository projetoRepository;
    private final PermissaoGuard permissaoGuard;

    public WorkflowService(
            WorkflowRepository workflowRepository,
            EtapaRepository etapaRepository,
            TransicaoRepository transicaoRepository,
            ProjetoRepository projetoRepository,
            PermissaoGuard permissaoGuard) {
        this.workflowRepository = workflowRepository;
        this.etapaRepository = etapaRepository;
        this.transicaoRepository = transicaoRepository;
        this.projetoRepository = projetoRepository;
        this.permissaoGuard = permissaoGuard;
    }

    public List<WorkflowComEtapasResponse> listar(UUID projetoId) {
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);

        return workflowRepository.findByProjetoId(projetoId).stream().map(this::toResponseComEtapas).toList();
    }

    @Transactional
    public WorkflowResponse criar(UUID projetoId, CriarWorkflowRequest request) {
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);
        exigirNomeValido(request.nome());

        Workflow workflow = new Workflow();
        workflow.setProjeto(projetoRepository.getReferenceById(projetoId));
        workflow.setNome(request.nome());
        workflow = workflowRepository.save(workflow);
        return new WorkflowResponse(workflow.getId(), workflow.getNome());
    }

    @Transactional
    public void excluirWorkflow(UUID id) {
        Workflow workflow = buscarWorkflow(id);
        UUID projetoId = workflow.getProjeto().getId();
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);

        if (possuiTarefasAtivasNoWorkflow(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "workflow possui tarefas ativas vinculadas");
        }

        List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdem(id);
        for (Etapa etapa : etapas) {
            transicaoRepository.deleteByEtapaOrigemId(etapa.getId());
        }
        etapaRepository.deleteAll(etapas);
        workflowRepository.delete(workflow);
    }

    @Transactional
    public EtapaResponse criarEtapa(UUID workflowId, CriarEtapaRequest request) {
        Workflow workflow = buscarWorkflow(workflowId);
        UUID projetoId = workflow.getProjeto().getId();
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);
        exigirNomeValido(request.nome());
        exigirOrdemValida(request.ordem());

        Etapa etapa = new Etapa();
        etapa.setWorkflow(workflow);
        etapa.setNome(request.nome());
        etapa.setOrdem(request.ordem());
        etapa.setEtapaFinal(request.etapaFinal());
        try {
            etapa = etapaRepository.save(etapa);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe etapa com essa ordem no workflow");
        }
        return toEtapaResponse(etapa);
    }

    @Transactional
    public EtapaResponse editarEtapa(UUID id, EditarEtapaRequest request) {
        Etapa etapa = buscarEtapa(id);
        UUID projetoId = etapa.getWorkflow().getProjeto().getId();
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);
        exigirNomeValido(request.nome());
        exigirOrdemValida(request.ordem());

        if (!request.etapaFinal() && transicaoRepository.findByEtapaOrigemId(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "etapa não-final exige ao menos uma transição de saída");
        }

        etapa.setNome(request.nome());
        etapa.setEtapaFinal(request.etapaFinal());
        if (etapa.getOrdem() != request.ordem()) {
            reordenar(etapa, request.ordem());
        } else {
            etapa = etapaRepository.save(etapa);
        }
        return toEtapaResponse(etapa);
    }

    @Transactional
    public void excluirEtapa(UUID id) {
        Etapa etapa = buscarEtapa(id);
        UUID projetoId = etapa.getWorkflow().getProjeto().getId();
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);

        if (possuiTarefasAtivasNaEtapa(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "etapa possui tarefas ativas vinculadas");
        }

        transicaoRepository.deleteByEtapaOrigemId(id);
        etapaRepository.delete(etapa);
    }

    @Transactional
    public TransicoesResponse atualizarTransicoes(UUID etapaId, AtualizarTransicoesRequest request) {
        Etapa etapa = buscarEtapa(etapaId);
        UUID projetoId = etapa.getWorkflow().getProjeto().getId();
        permissaoGuard.exigir(projetoId, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(projetoId);

        List<UUID> destinos = request.etapasDestinoIds() == null ? List.of() : request.etapasDestinoIds();
        if (!etapa.isEtapaFinal() && destinos.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "etapa não-final exige ao menos uma transição de saída");
        }

        UUID workflowId = etapa.getWorkflow().getId();
        for (UUID destinoId : destinos) {
            if (destinoId.equals(etapaId)) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "etapa não pode ter transição de saída para si mesma");
            }
            Etapa destino = buscarEtapa(destinoId);
            if (!destino.getWorkflow().getId().equals(workflowId)) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "etapa de destino pertence a outro workflow");
            }
        }

        transicaoRepository.deleteByEtapaOrigemId(etapaId);
        List<UUID> salvos = new ArrayList<>();
        for (UUID destinoId : destinos) {
            Etapa destino = buscarEtapa(destinoId);
            Transicao transicao = new Transicao();
            transicao.setEtapaOrigem(etapa);
            transicao.setEtapaDestino(destino);
            try {
                transicaoRepository.save(transicao);
            } catch (DataIntegrityViolationException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "transição duplicada");
            }
            salvos.add(destinoId);
        }
        return new TransicoesResponse(etapaId, salvos);
    }

    /**
     * Stub RN-005 — `Tarefa` só existe a partir de TASK-04.1; até lá, nenhum workflow tem tarefas
     * ativas. Substituído obrigatoriamente pela checagem real em TASK-04.1 (decisão fechada pelo
     * Comitê de Análise, não é opcional).
     */
    private boolean possuiTarefasAtivasNoWorkflow(UUID workflowId) {
        return false;
    }

    /** Stub RN-005 — ver {@link #possuiTarefasAtivasNoWorkflow(UUID)}. */
    private boolean possuiTarefasAtivasNaEtapa(UUID etapaId) {
        return false;
    }

    private void reordenar(Etapa etapaMovida, int novaOrdem) {
        List<Etapa> etapas = new ArrayList<>(etapaRepository.findByWorkflowIdOrderByOrdem(etapaMovida.getWorkflow().getId()));
        etapas.removeIf(e -> e.getId().equals(etapaMovida.getId()));
        etapas.sort(Comparator.comparingInt(Etapa::getOrdem));

        int posicao = Math.max(0, Math.min(novaOrdem, etapas.size()));
        etapas.add(posicao, etapaMovida);

        for (int i = 0; i < etapas.size(); i++) {
            etapas.get(i).setOrdem(i);
        }
        etapaRepository.saveAll(etapas);
    }

    private void exigirNomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "nome é obrigatório");
        }
    }

    private void exigirOrdemValida(int ordem) {
        if (ordem < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "ordem não pode ser negativa");
        }
    }

    private Workflow buscarWorkflow(UUID id) {
        return workflowRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "workflow não encontrado"));
    }

    private Etapa buscarEtapa(UUID id) {
        return etapaRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "etapa não encontrada"));
    }

    private WorkflowComEtapasResponse toResponseComEtapas(Workflow workflow) {
        List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdem(workflow.getId());
        Map<UUID, List<UUID>> transicoesPorOrigem =
                transicaoRepository
                        .findByEtapaOrigemIdIn(etapas.stream().map(Etapa::getId).toList())
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        t -> t.getEtapaOrigem().getId(),
                                        Collectors.mapping(t -> t.getEtapaDestino().getId(), Collectors.toList())));

        List<EtapaResponse> etapasResponse =
                etapas.stream().map(etapa -> toEtapaResponse(etapa, transicoesPorOrigem)).toList();
        return new WorkflowComEtapasResponse(workflow.getId(), workflow.getNome(), etapasResponse);
    }

    private EtapaResponse toEtapaResponse(Etapa etapa) {
        List<UUID> transicoesSaida =
                transicaoRepository.findByEtapaOrigemId(etapa.getId()).stream()
                        .map(t -> t.getEtapaDestino().getId())
                        .toList();
        return new EtapaResponse(etapa.getId(), etapa.getNome(), etapa.getOrdem(), etapa.isEtapaFinal(), transicoesSaida);
    }

    private EtapaResponse toEtapaResponse(Etapa etapa, Map<UUID, List<UUID>> transicoesPorOrigem) {
        List<UUID> transicoesSaida = transicoesPorOrigem.getOrDefault(etapa.getId(), List.of());
        return new EtapaResponse(etapa.getId(), etapa.getNome(), etapa.getOrdem(), etapa.isEtapaFinal(), transicoesSaida);
    }
}
