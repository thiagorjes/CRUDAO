package com.crudao.kanban.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistorico;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistoricoRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Agregação de lead-time e impedimento por etapa (RF-007, TASK-06.1) — dataset controlado com
 * múltiplas tarefas/etapas, conforme critério de aceite da task.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;
    @Mock private TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;
    @Mock private PermissaoGuard permissaoGuard;

    private DashboardService service;

    private final UUID projetoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new DashboardService(
                        tarefaEtapaHistoricoRepository, tarefaImpedimentoHistoricoRepository, permissaoGuard);
    }

    @Test
    void naoMembroDoProjeto_lancaAccessDenied() {
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        assertThatThrownBy(() -> service.dashboard(projetoId)).isInstanceOf(AccessDeniedException.class);

        verify(tarefaEtapaHistoricoRepository, never()).findByTarefaProjetoIdAndSaidaEmIsNotNull(projetoId);
    }

    @Test
    void agregaLeadTimeETempoImpedimentoPorEtapaComMultiplasTarefas() {
        Etapa etapaA = etapa("Em andamento");
        Etapa etapaB = etapa("Revisão");
        Tarefa tarefa1 = tarefa();
        Tarefa tarefa2 = tarefa();
        OffsetDateTime t0 = OffsetDateTime.parse("2026-08-01T10:00:00Z");

        // etapaA: tarefa1 fica 100s, tarefa2 fica 200s -> média 150s
        TarefaEtapaHistorico h1 = historicoEtapa(tarefa1, etapaA, t0, t0.plusSeconds(100));
        TarefaEtapaHistorico h2 = historicoEtapa(tarefa2, etapaA, t0, t0.plusSeconds(200));
        // etapaB: só tarefa1, 300s
        TarefaEtapaHistorico h3 = historicoEtapa(tarefa1, etapaB, t0, t0.plusSeconds(300));

        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(tarefaEtapaHistoricoRepository.findByTarefaProjetoIdAndSaidaEmIsNotNull(projetoId))
                .thenReturn(List.of(h1, h2, h3));

        // impedimento na etapaA: tarefa1 impedida 40s, tarefa2 impedida 20s -> total 60s / 2 passagens = 30s
        TarefaImpedimentoHistorico i1 = historicoImpedimento(tarefa1, etapaA, t0, t0.plusSeconds(40));
        TarefaImpedimentoHistorico i2 = historicoImpedimento(tarefa2, etapaA, t0, t0.plusSeconds(20));
        when(tarefaImpedimentoHistoricoRepository
                        .findByTarefaProjetoIdAndEtapaIsNotNullAndDesmarcadoEmIsNotNull(projetoId))
                .thenReturn(List.of(i1, i2));

        DashboardResponse dashboard = service.dashboard(projetoId);

        assertThat(dashboard.totalTarefasConsideradas()).isEqualTo(2);
        assertThat(dashboard.leadTimeMedioPorEtapa()).hasSize(2);

        DashboardEtapaResponse respostaA =
                dashboard.leadTimeMedioPorEtapa().stream()
                        .filter(r -> r.etapaId().equals(etapaA.getId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(respostaA.leadTimeMedioSegundos()).isEqualTo(150);
        assertThat(respostaA.tempoImpedimentoMedioSegundos()).isEqualTo(30);

        DashboardEtapaResponse respostaB =
                dashboard.leadTimeMedioPorEtapa().stream()
                        .filter(r -> r.etapaId().equals(etapaB.getId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(respostaB.leadTimeMedioSegundos()).isEqualTo(300);
        assertThat(respostaB.tempoImpedimentoMedioSegundos()).isZero();
    }

    @Test
    void semHistorico_retornaListaVaziaETotalZero() {
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(tarefaEtapaHistoricoRepository.findByTarefaProjetoIdAndSaidaEmIsNotNull(projetoId))
                .thenReturn(List.of());
        when(tarefaImpedimentoHistoricoRepository
                        .findByTarefaProjetoIdAndEtapaIsNotNullAndDesmarcadoEmIsNotNull(projetoId))
                .thenReturn(List.of());

        DashboardResponse dashboard = service.dashboard(projetoId);

        assertThat(dashboard.leadTimeMedioPorEtapa()).isEmpty();
        assertThat(dashboard.totalTarefasConsideradas()).isZero();
    }

    private Tarefa tarefa() {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());
        return tarefa;
    }

    private Etapa etapa(String nome) {
        Etapa etapa = new Etapa();
        etapa.setId(UUID.randomUUID());
        etapa.setNome(nome);
        return etapa;
    }

    private TarefaEtapaHistorico historicoEtapa(
            Tarefa tarefa, Etapa etapa, OffsetDateTime entrada, OffsetDateTime saida) {
        TarefaEtapaHistorico h = new TarefaEtapaHistorico();
        h.setTarefa(tarefa);
        h.setEtapa(etapa);
        h.setEntradaEm(entrada);
        h.setSaidaEm(saida);
        return h;
    }

    private TarefaImpedimentoHistorico historicoImpedimento(
            Tarefa tarefa, Etapa etapa, OffsetDateTime marcado, OffsetDateTime desmarcado) {
        TarefaImpedimentoHistorico h = new TarefaImpedimentoHistorico();
        h.setTarefa(tarefa);
        h.setEtapa(etapa);
        h.setMarcadoEm(marcado);
        h.setDesmarcadoEm(desmarcado);
        return h;
    }
}
