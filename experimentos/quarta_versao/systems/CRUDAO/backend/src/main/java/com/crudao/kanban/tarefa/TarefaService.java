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
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import com.crudao.kanban.tarefa.dto.CriarTarefaResponse;
import java.time.Instant;
import java.util.List;
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
    private final ProjetoRepository projetoRepository;
    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
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
}

