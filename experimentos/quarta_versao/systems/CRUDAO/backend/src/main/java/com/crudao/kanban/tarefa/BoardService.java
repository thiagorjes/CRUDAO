package com.crudao.kanban.tarefa;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.tarefa.dto.BoardResponse;
import com.crudao.kanban.tarefa.dto.BoardResponse.EtapaCardDTO;
import com.crudao.kanban.tarefa.dto.BoardResponse.RaiaCardDTO;
import com.crudao.kanban.tarefa.dto.BoardResponse.TarefaCardDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * TASK-04.5: Serviço de construção do board sem N+1.
 * RF-001, RF-006: Retorna etapas (na ordem configurada), raias e tarefas.
 * Estratégia: queries separadas para cada entidade (evita LEFT JOIN N+1).
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private static final Logger log = LoggerFactory.getLogger(BoardService.class);

    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
    private final RaiaRepository raiaRepository;
    private final TarefaRepository tarefaRepository;
    private final TransicaoRepository transicaoRepository;
    private final ProjetoRepository projetoRepository;
    private final PermissaoGuard permissaoGuard;

    /**
     * GET /api/projetos/{projetoId}/board
     * Retorna o estado completo do board (etapas × raias × cards).
     *
     * Estratégia sem N+1:
     * 1. Query 1: Buscar etapas do workflow do projeto (ordenadas por ordem)
     * 2. Query 2: Buscar raias do projeto + raia global
     * 3. Query 3: Buscar todas as tarefas do projeto com etapaAtual, raia, responsável
     * 4. Query 4: Buscar transições para montar a lista de saídas por etapa
     *
     * Total: 4 queries fixas, sem escalar com o número de tarefas.
     */
    @Transactional(readOnly = true)
    public BoardResponse obterBoard(UUID projetoId) {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        UUID usuarioId = usuario != null ? usuario.getId() : null;

        log.info("Acesso ao board iniciado: projetoId={}, usuarioId={}", projetoId, usuarioId);

        try {
            // Verificar que o projeto existe
            projetoRepository.findById(projetoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

            // TASK-04.5 Correção I1: Validar que o usuário autenticado tem vínculo ao projeto (leitura exige acesso)
            if (!permissaoGuard.membro(projetoId)) {
                log.warn("Acesso negado ao board: projetoId={}, usuarioId={} (sem vínculo ao projeto)", projetoId, usuarioId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
            }

        // Query 1: Etapas do workflow do projeto
        List<Workflow> workflows = workflowRepository.findByProjetoId(projetoId);
        if (workflows.isEmpty()) {
            // Projeto sem workflow — retornar board vazio
            return BoardResponse.builder()
                    .etapas(List.of())
                    .raias(List.of())
                    .tarefas(List.of())
                    .build();
        }

        Workflow workflow = workflows.get(0);
        List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId());

        // Query 2: Raias do projeto + raia global
        List<Raia> raiasProjeto = raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId);
        List<Raia> raiasGlobais = raiaRepository.findByProjetoIdIsNullOrderByOrdemAsc();
        List<Raia> todasRaias = new ArrayList<>(raiasProjeto);
        todasRaias.addAll(raiasGlobais);

        // Query 3: Todas as tarefas do projeto
        List<Tarefa> tarefas = tarefaRepository.findByProjetoIdOrderByAtualizadoEmDesc(projetoId);

        // Query 4: Transições para cada etapa (para construir lista de saídas)
        Map<UUID, List<UUID>> transicoesPorEtapa = new java.util.HashMap<>();
        for (Etapa etapa : etapas) {
            List<Transicao> transicoes = transicaoRepository.findByEtapaOrigemId(etapa.getId());
            List<UUID> destinos = transicoes.stream()
                    .map(t -> t.getEtapaDestino().getId())
                    .collect(Collectors.toList());
            transicoesPorEtapa.put(etapa.getId(), destinos);
        }

        // Montar DTOs
        List<EtapaCardDTO> etapasDTO = etapas.stream()
                .map(e -> EtapaCardDTO.builder()
                        .id(e.getId())
                        .nome(e.getNome())
                        .ordem(e.getOrdem())
                        .transicoesSaida(transicoesPorEtapa.getOrDefault(e.getId(), List.of()))
                        .build())
                .collect(Collectors.toList());

        List<RaiaCardDTO> raiasDTO = todasRaias.stream()
                .map(r -> RaiaCardDTO.builder()
                        .id(r.getId())
                        .nome(r.getNome())
                        .ordem(r.getOrdem())
                        .global(r.getProjeto() == null)
                        .build())
                .collect(Collectors.toList());

        List<TarefaCardDTO> tarefasDTO = tarefas.stream()
                .map(t -> TarefaCardDTO.builder()
                        .id(t.getId())
                        .titulo(t.getTitulo())
                        .etapaAtualId(t.getEtapaAtual() != null ? t.getEtapaAtual().getId() : null)
                        .raiaId(t.getRaia() != null ? t.getRaia().getId() : null)
                        .responsavelId(t.getResponsavel() != null ? t.getResponsavel().getId() : null)
                        .impedida(t.isImpedida())
                        .impedidaDesdeMs(t.getImpedidaDesde() != null ? t.getImpedidaDesde().toEpochMilli() : 0L)
                        .iniciada(t.isIniciada())
                        .build())
                .collect(Collectors.toList());

        BoardResponse board = BoardResponse.builder()
                .etapas(etapasDTO)
                .raias(raiasDTO)
                .tarefas(tarefasDTO)
                .build();

            log.debug("Board retornado com sucesso: projetoId={}, etapas={}, tarefas={}, raias={}", projetoId, board.getEtapas().size(), board.getTarefas().size(), board.getRaias().size());
            return board;
        } catch (ResponseStatusException e) {
            log.warn("Erro ao obter board: projetoId={}, usuarioId={}, status={}, mensagem={}", projetoId, usuarioId, e.getStatusCode(), e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao obter board: projetoId={}, usuarioId={}, erro={}", projetoId, usuarioId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao obter board");
        }
    }
}
