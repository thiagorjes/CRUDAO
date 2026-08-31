package com.crudao.kanban.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.crudao.kanban.dashboard.DashboardResponse.EtapaLeadTime;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistorico;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistoricoRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock ProjetoRepository projetoRepository;
    @Mock WorkflowRepository workflowRepository;
    @Mock EtapaRepository etapaRepository;
    @Mock TarefaEtapaHistoricoRepository etapaHistoricoRepository;
    @Mock TarefaImpedimentoHistoricoRepository impedimentoHistoricoRepository;
    @Mock PermissaoGuard permissaoGuard;
    @InjectMocks DashboardService service;

    private final UUID projetoId = UUID.randomUUID();
    private final Instant t0 = Instant.parse("2026-08-01T00:00:00Z");
    private Etapa e1;
    private Etapa e2;
    private Tarefa t1;
    private Tarefa t2;

    @BeforeEach
    void setUp() {
        Workflow wf = new Workflow();
        wf.setId(UUID.randomUUID());

        e1 = new Etapa();
        e1.setId(UUID.randomUUID());
        e1.setNome("Fazendo");
        e1.setOrdem(1);
        e2 = new Etapa();
        e2.setId(UUID.randomUUID());
        e2.setNome("Revisão");
        e2.setOrdem(2);

        t1 = Tarefa.builder().id(UUID.randomUUID()).build();
        t2 = Tarefa.builder().id(UUID.randomUUID()).build();

        lenient().when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(wf));
        lenient().when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(wf.getId())).thenReturn(List.of(e1, e2));
        lenient().when(permissaoGuard.membro(projetoId)).thenReturn(true);
    }

    private Projeto projeto(Projeto.Status status) {
        Projeto p = new Projeto();
        p.setId(projetoId);
        p.setStatus(status);
        return p;
    }

    private TarefaEtapaHistorico hist(Tarefa tarefa, Etapa etapa, long entradaOffset, Long saidaOffset) {
        TarefaEtapaHistorico h = new TarefaEtapaHistorico();
        h.setTarefa(tarefa);
        h.setEtapa(etapa);
        h.setEntradaEm(t0.plusSeconds(entradaOffset));
        h.setSaidaEm(saidaOffset == null ? null : t0.plusSeconds(saidaOffset));
        return h;
    }

    private TarefaImpedimentoHistorico imped(Tarefa tarefa, long marcadoOffset, Long desmarcadoOffset) {
        TarefaImpedimentoHistorico i = new TarefaImpedimentoHistorico();
        i.setTarefa(tarefa);
        i.setMarcadoEm(t0.plusSeconds(marcadoOffset));
        i.setDesmarcadoEm(desmarcadoOffset == null ? null : t0.plusSeconds(desmarcadoOffset));
        return i;
    }

    @Test
    void agregaLeadTimeMedioPorEtapaEImpedimentoPorSobreposicao() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto(Projeto.Status.ATIVO)));
        when(etapaHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                hist(t1, e1, 0, 100L),      // T1 em E1: 100s
                hist(t1, e2, 100, 300L),    // T1 em E2: 200s
                hist(t2, e1, 0, 200L),      // T2 em E1: 200s
                hist(t2, e2, 200, null)     // T2 em E2: intervalo aberto -> ignorado
        ));
        when(impedimentoHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                imped(t1, 20, 50L),         // 30s, todo dentro da janela de T1/E1
                imped(t2, 100, 250L)        // overlap com T2/E1 [0,200] = 100s
        ));

        DashboardResponse r = service.obterDashboard(projetoId);

        assertEquals(2, r.totalTarefasConsideradas());
        EtapaLeadTime etapa1 = r.leadTimeMedioPorEtapa().get(0);
        EtapaLeadTime etapa2 = r.leadTimeMedioPorEtapa().get(1);

        assertEquals(e1.getId(), etapa1.etapaId());
        assertEquals(150L, etapa1.leadTimeMedioSegundos());       // (100 + 200) / 2
        assertEquals(65L, etapa1.tempoImpedimentoMedioSegundos()); // (30 + 100) / 2
        assertEquals(200L, etapa2.leadTimeMedioSegundos());        // apenas T1 (200) / 1
        assertEquals(0L, etapa2.tempoImpedimentoMedioSegundos());  // nenhum impedimento na janela de E2
    }

    @Test
    void etapaSemHistoricoRetornaZeros() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto(Projeto.Status.ATIVO)));
        when(etapaHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of());
        when(impedimentoHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of());

        DashboardResponse r = service.obterDashboard(projetoId);

        assertEquals(0, r.totalTarefasConsideradas());
        assertEquals(0L, r.leadTimeMedioPorEtapa().get(0).leadTimeMedioSegundos());
        assertEquals(0L, r.leadTimeMedioPorEtapa().get(0).tempoImpedimentoMedioSegundos());
    }

    @Test
    void acessivelComProjetoFinalizado() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto(Projeto.Status.FINALIZADO)));
        when(etapaHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(hist(t1, e1, 0, 100L)));
        when(impedimentoHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of());

        DashboardResponse r = service.obterDashboard(projetoId);

        assertEquals(100L, r.leadTimeMedioPorEtapa().get(0).leadTimeMedioSegundos());
    }

    @Test
    void semVinculoAoProjetoRetorna403() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto(Projeto.Status.ATIVO)));
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obterDashboard(projetoId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void projetoInexistenteRetorna404() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.obterDashboard(projetoId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void impedimentoAbertoUsaFallbackSaidaDaEtapa() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto(Projeto.Status.ATIVO)));
        when(etapaHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                hist(t1, e1, 0, 100L)));
        when(impedimentoHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                imped(t1, 40, null))); // aberto -> fim = saidaEm da etapa (100) -> overlap 60s

        DashboardResponse r = service.obterDashboard(projetoId);

        assertEquals(60L, r.leadTimeMedioPorEtapa().get(0).tempoImpedimentoMedioSegundos());
    }

    @Test
    void impedimentoForaDaJanelaDaEtapaNaoConta() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto(Projeto.Status.ATIVO)));
        when(etapaHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                hist(t1, e1, 0, 100L)));
        when(impedimentoHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                imped(t1, 200, 300L))); // inteiramente após a saída da etapa

        DashboardResponse r = service.obterDashboard(projetoId);

        assertEquals(0L, r.leadTimeMedioPorEtapa().get(0).tempoImpedimentoMedioSegundos());
    }

    @Test
    void impedimentoIniciadoAntesDaEntradaSofreClippingInferior() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto(Projeto.Status.ATIVO)));
        when(etapaHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                hist(t1, e1, 100, 300L))); // janela [100, 300]
        when(impedimentoHistoricoRepository.findByTarefa_Projeto_Id(projetoId)).thenReturn(List.of(
                imped(t1, 50, 180L))); // começa antes da entrada -> overlap [100, 180] = 80s

        DashboardResponse r = service.obterDashboard(projetoId);

        assertEquals(80L, r.leadTimeMedioPorEtapa().get(0).tempoImpedimentoMedioSegundos());
    }
}
