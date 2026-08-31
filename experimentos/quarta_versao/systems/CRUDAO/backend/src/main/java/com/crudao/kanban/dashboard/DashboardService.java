package com.crudao.kanban.dashboard;

import com.crudao.kanban.dashboard.DashboardResponse.EtapaLeadTime;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistorico;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistoricoRepository;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * TASK-06.1 / RF-007: Agregação de lead-time médio para o dashboard de gestão.
 *
 * <p>RN-001: o lead-time de uma etapa conta da entrada até a saída da tarefa naquela etapa
 * ({@link TarefaEtapaHistorico}); intervalos ainda abertos (sem {@code saidaEm}) são ignorados.
 *
 * <p>RN-002: o tempo de impedimento é somado ao lead-time de impedimento da etapa. Como
 * {@link TarefaImpedimentoHistorico} não referencia etapa, o tempo é atribuído por sobreposição
 * (overlap) entre cada intervalo de impedimento e a janela em que a tarefa esteve na etapa.
 *
 * <p>RN-015: dashboard é somente leitura e permanece acessível com projeto finalizado — não há
 * chamada a {@code exigirProjetoAtivo}; apenas vínculo ao projeto é exigido.
 *
 * <p>Sem pré-cálculo/materialização (contrato dashboard-notificacoes.md): 4 queries fixas.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ProjetoRepository projetoRepository;
    private final WorkflowRepository workflowRepository;
    private final EtapaRepository etapaRepository;
    private final TarefaEtapaHistoricoRepository etapaHistoricoRepository;
    private final TarefaImpedimentoHistoricoRepository impedimentoHistoricoRepository;
    private final PermissaoGuard permissaoGuard;

    @Transactional(readOnly = true)
    public DashboardResponse obterDashboard(UUID projetoId) {
        projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projeto não encontrado"));

        if (!permissaoGuard.membro(projetoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        List<Workflow> workflows = workflowRepository.findByProjetoId(projetoId);
        List<Etapa> etapas = workflows.isEmpty()
                ? List.of()
                : etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflows.get(0).getId());

        List<TarefaEtapaHistorico> historicos = etapaHistoricoRepository.findByTarefa_Projeto_Id(projetoId);
        Map<UUID, List<TarefaImpedimentoHistorico>> impedimentosPorTarefa =
                impedimentoHistoricoRepository.findByTarefa_Projeto_Id(projetoId).stream()
                        .collect(Collectors.groupingBy(i -> i.getTarefa().getId()));

        List<EtapaLeadTime> porEtapa = new ArrayList<>();
        Set<UUID> tarefasConsideradas = new HashSet<>();

        for (Etapa etapa : etapas) {
            List<TarefaEtapaHistorico> registros = historicos.stream()
                    .filter(h -> h.getEtapa() != null && etapa.getId().equals(h.getEtapa().getId()))
                    .filter(h -> h.getSaidaEm() != null)
                    .toList();

            long leadMedio = 0L;
            long impedimentoMedio = 0L;
            if (!registros.isEmpty()) {
                long somaLead = 0L;
                long somaImpedimento = 0L;
                for (TarefaEtapaHistorico r : registros) {
                    somaLead += r.getSaidaEm().getEpochSecond() - r.getEntradaEm().getEpochSecond();
                    somaImpedimento += impedimentoNaJanela(
                            r.getEntradaEm(), r.getSaidaEm(),
                            impedimentosPorTarefa.getOrDefault(r.getTarefa().getId(), List.of()));
                    tarefasConsideradas.add(r.getTarefa().getId());
                }
                leadMedio = somaLead / registros.size();
                impedimentoMedio = somaImpedimento / registros.size();
            }

            porEtapa.add(new EtapaLeadTime(etapa.getId(), etapa.getNome(), leadMedio, impedimentoMedio));
        }

        log.info("Dashboard agregado: projetoId={}, etapas={}, tarefasConsideradas={}",
                projetoId, porEtapa.size(), tarefasConsideradas.size());

        return new DashboardResponse(porEtapa, tarefasConsideradas.size());
    }

    /** Soma, em segundos, a sobreposição dos intervalos de impedimento com a janela [inicio, fim] da etapa. */
    private long impedimentoNaJanela(Instant inicio, Instant fim, List<TarefaImpedimentoHistorico> impedimentos) {
        long total = 0L;
        for (TarefaImpedimentoHistorico imp : impedimentos) {
            Instant impInicio = imp.getMarcadoEm();
            Instant impFim = imp.getDesmarcadoEm() != null ? imp.getDesmarcadoEm() : fim;
            long overlapInicio = Math.max(inicio.getEpochSecond(), impInicio.getEpochSecond());
            long overlapFim = Math.min(fim.getEpochSecond(), impFim.getEpochSecond());
            if (overlapFim > overlapInicio) {
                total += overlapFim - overlapInicio;
            }
        }
        return total;
    }
}
