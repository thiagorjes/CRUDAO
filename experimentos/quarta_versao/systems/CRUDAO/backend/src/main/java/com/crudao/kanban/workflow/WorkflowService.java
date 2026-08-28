package com.crudao.kanban.workflow;

import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.workflow.*;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.workflow.dto.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
    private final TransicaoRepository transicaoRepository;
    private final ProjetoRepository projetoRepository;
    private final PermissaoGuard permissaoGuard;

    @Transactional(readOnly = true)
    public List<WorkflowResponse> listarWorkflows(UUID projetoId) {
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        List<Workflow> workflows = workflowRepository.findByProjetoId(projetoId);
        return workflows.stream().map(this::mapToWorkflowResponse).collect(Collectors.toList());
    }

    @Transactional
    public WorkflowResponse criarWorkflow(UUID projetoId, CriarWorkflowRequest request) {
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        Workflow workflow = new Workflow();
        workflow.setProjeto(projeto);
        workflow.setNome(request.getNome());
        workflow = workflowRepository.save(workflow);

        return new WorkflowResponse(workflow.getId(), workflow.getNome(), Collections.emptyList());
    }

    @Transactional
    public void excluirWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow não encontrado"));

        UUID projetoId = workflow.getProjeto().getId();
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        if (temTarefasAtivasNoWorkflow(workflowId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exclusão bloqueada: existem tarefas ativas vinculadas ao workflow");
        }

        workflowRepository.delete(workflow);
    }

    @Transactional
    public EtapaResponse criarEtapa(UUID workflowId, CriarEtapaRequest request) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow não encontrado"));

        UUID projetoId = workflow.getProjeto().getId();
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        Etapa etapa = new Etapa();
        etapa.setWorkflow(workflow);
        etapa.setNome(request.getNome());
        etapa.setOrdem(request.getOrdem());
        etapa.setEtapaFinal(Boolean.TRUE.equals(request.getEtapaFinal()));

        etapa = etapaRepository.save(etapa);

        return new EtapaResponse(etapa.getId(), etapa.getNome(), etapa.getOrdem(), etapa.getEtapaFinal(), Collections.emptyList());
    }

    @Transactional
    public EtapaResponse atualizarEtapa(UUID etapaId, AtualizarEtapaRequest request) {
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada"));

        UUID projetoId = etapa.getWorkflow().getProjeto().getId();
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        etapa.setNome(request.getNome());
        etapa.setOrdem(request.getOrdem());
        etapa.setEtapaFinal(Boolean.TRUE.equals(request.getEtapaFinal()));

        etapa = etapaRepository.save(etapa);

        List<Transicao> transicoes = transicaoRepository.findByEtapaOrigemId(etapaId);
        List<UUID> destinos = transicoes.stream().map(t -> t.getEtapaDestino().getId()).collect(Collectors.toList());

        return new EtapaResponse(etapa.getId(), etapa.getNome(), etapa.getOrdem(), etapa.getEtapaFinal(), destinos);
    }

    @Transactional
    public void excluirEtapa(UUID etapaId) {
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada"));

        UUID projetoId = etapa.getWorkflow().getProjeto().getId();
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        if (temTarefasAtivasNaEtapa(etapaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exclusão bloqueada: existem tarefas ativas vinculadas à etapa");
        }

        etapaRepository.delete(etapa);
    }

    @Transactional
    public TransicoesResponse atualizarTransicoes(UUID etapaId, AtualizarTransicoesRequest request) {
        Etapa etapaOrigem = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada"));

        UUID projetoId = etapaOrigem.getWorkflow().getProjeto().getId();
        permissaoGuard.exigirProjetoAtivo(projetoId);
        permissaoGuard.exigir(projetoId, "workflow:administrar");

        List<UUID> destinosIds = request.getEtapasDestinoIds() != null ? request.getEtapasDestinoIds() : Collections.emptyList();

        // RN-003: Validação de transições em etapas não-finais
        if (Boolean.FALSE.equals(etapaOrigem.getEtapaFinal()) && destinosIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Etapa não-final exige ao menos uma transição de saída (RN-003)");
        }

        transicaoRepository.deleteByEtapaOrigemId(etapaId);

        for (UUID destinoId : destinosIds) {
            Etapa etapaDestino = etapaRepository.findById(destinoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa destino " + destinoId + " não encontrada"));
            Transicao transicao = new Transicao();
            transicao.setEtapaOrigem(etapaOrigem);
            transicao.setEtapaDestino(etapaDestino);
            transicaoRepository.save(transicao);
        }

        return new TransicoesResponse(etapaId, destinosIds);
    }

    private WorkflowResponse mapToWorkflowResponse(Workflow workflow) {
        List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId());
        List<Transicao> transicoes = transicaoRepository.findByEtapaOrigemWorkflowId(workflow.getId());

        Map<UUID, List<UUID>> transicoesPorOrigem = transicoes.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getEtapaOrigem().getId(),
                        Collectors.mapping(t -> t.getEtapaDestino().getId(), Collectors.toList())
                ));

        List<EtapaResponse> etapaResponses = etapas.stream().map(e -> new EtapaResponse(
                e.getId(),
                e.getNome(),
                e.getOrdem(),
                e.getEtapaFinal(),
                transicoesPorOrigem.getOrDefault(e.getId(), Collections.emptyList())
        )).collect(Collectors.toList());

        return new WorkflowResponse(workflow.getId(), workflow.getNome(), etapaResponses);
    }

    private final TarefaRepository tarefaRepository;

    private boolean temTarefasAtivasNoWorkflow(UUID workflowId) {
        return tarefaRepository.existsByWorkflowId(workflowId);
    }

    private boolean temTarefasAtivasNaEtapa(UUID etapaId) {
        return tarefaRepository.existsByEtapaAtualId(etapaId);
    }
}
