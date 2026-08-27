package com.crudao.kanban.dashboard;

import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistorico;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistoricoRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Lead-time médio e tempo médio de impedimento agregados por etapa (RF-007, RN-001, RN-002).
 *
 * <p>Acessível com o projeto finalizado (RN-015 — leitura sempre permitida, sem {@code
 * exigirProjetoAtivo}). Agregação feita em Java sobre os registros de histórico já carregados
 * (mesmo padrão de {@code TarefaService.detalhe}), não em SQL — volume por projeto não justifica
 * agregação no banco.
 *
 * <p>Tempo médio de impedimento por etapa (decisão do usuário, TASK-06.1) = soma absoluta do tempo
 * de impedimento de todos os ciclos ocorridos na etapa, dividida pelo total de tarefas que
 * passaram por ela (mesmo conjunto usado para o lead-time). Ciclos de impedimento sem {@code
 * etapa} associada (anteriores à migration V11) ficam de fora da agregação por etapa.
 */
@Service
public class DashboardService {

    private final TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;
    private final TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;
    private final PermissaoGuard permissaoGuard;

    public DashboardService(
            TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository,
            TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository,
            PermissaoGuard permissaoGuard) {
        this.tarefaEtapaHistoricoRepository = tarefaEtapaHistoricoRepository;
        this.tarefaImpedimentoHistoricoRepository = tarefaImpedimentoHistoricoRepository;
        this.permissaoGuard = permissaoGuard;
    }

    public DashboardResponse dashboard(UUID projetoId) {
        if (!permissaoGuard.membro(projetoId)) {
            throw new AccessDeniedException("Acesso negado");
        }

        List<TarefaEtapaHistorico> etapaHistoricos =
                tarefaEtapaHistoricoRepository.findByTarefaProjetoIdAndSaidaEmIsNotNull(projetoId);
        List<TarefaImpedimentoHistorico> impedimentoHistoricos =
                tarefaImpedimentoHistoricoRepository
                        .findByTarefaProjetoIdAndEtapaIsNotNullAndDesmarcadoEmIsNotNull(projetoId);

        Map<UUID, List<TarefaEtapaHistorico>> leadTimesPorEtapa =
                etapaHistoricos.stream().collect(Collectors.groupingBy(h -> h.getEtapa().getId()));
        Map<UUID, List<TarefaImpedimentoHistorico>> impedimentosPorEtapa =
                impedimentoHistoricos.stream().collect(Collectors.groupingBy(h -> h.getEtapa().getId()));

        Map<UUID, String> nomesPorEtapa = new HashMap<>();
        etapaHistoricos.forEach(h -> nomesPorEtapa.put(h.getEtapa().getId(), h.getEtapa().getNome()));
        impedimentoHistoricos.forEach(
                h -> nomesPorEtapa.putIfAbsent(h.getEtapa().getId(), h.getEtapa().getNome()));

        List<DashboardEtapaResponse> agregados =
                nomesPorEtapa.entrySet().stream()
                        .map(entry -> agregarEtapa(entry.getKey(), entry.getValue(), leadTimesPorEtapa, impedimentosPorEtapa))
                        .sorted(Comparator.comparing(DashboardEtapaResponse::etapaNome))
                        .toList();

        int totalTarefasConsideradas =
                (int) etapaHistoricos.stream().map(h -> h.getTarefa().getId()).distinct().count();

        return new DashboardResponse(agregados, totalTarefasConsideradas);
    }

    private DashboardEtapaResponse agregarEtapa(
            UUID etapaId,
            String etapaNome,
            Map<UUID, List<TarefaEtapaHistorico>> leadTimesPorEtapa,
            Map<UUID, List<TarefaImpedimentoHistorico>> impedimentosPorEtapa) {
        List<TarefaEtapaHistorico> leadTimes = leadTimesPorEtapa.getOrDefault(etapaId, List.of());
        long leadTimeMedio =
                (long)
                        leadTimes.stream()
                                .mapToLong(h -> Duration.between(h.getEntradaEm(), h.getSaidaEm()).getSeconds())
                                .average()
                                .orElse(0);

        List<TarefaImpedimentoHistorico> impedimentos = impedimentosPorEtapa.getOrDefault(etapaId, List.of());
        long tempoImpedimentoTotal =
                impedimentos.stream()
                        .mapToLong(h -> Duration.between(h.getMarcadoEm(), h.getDesmarcadoEm()).getSeconds())
                        .sum();
        // União com as tarefas de "leadTimes": uma tarefa pode ter fechado um ciclo de impedimento
        // na etapa sem ainda ter saído dela (achado de code review — agent QA, TASK-06.1), então o
        // conjunto de "passagens" não pode se basear só no lead-time fechado.
        long passagens =
                Stream.concat(
                                leadTimes.stream().map(h -> h.getTarefa().getId()),
                                impedimentos.stream().map(h -> h.getTarefa().getId()))
                        .distinct()
                        .count();
        long tempoImpedimentoMedio = passagens == 0 ? 0 : tempoImpedimentoTotal / passagens;

        return new DashboardEtapaResponse(etapaId, etapaNome, leadTimeMedio, tempoImpedimentoMedio);
    }
}
