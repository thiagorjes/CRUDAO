package com.crudao.kanban.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistorico;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * TASK-06.1 / RF-007: Teste de integração do endpoint GET /api/projetos/{projetoId}/dashboard
 * contra o PostgreSQL do stack Docker final (ver {@link IntegrationTestBase}).
 *
 * <p>Cobre o que o teste unitário (Mockito) não alcança:
 * <ul>
 *   <li>resolução em runtime dos <em>derived queries</em> de path aninhado
 *       {@code findByTarefa_Projeto_Id} sobre o schema real;</li>
 *   <li>serialização do {@link DashboardResponse} pelo controller (200);</li>
 *   <li>fluxos de erro 403 (sem vínculo) e 404 (projeto inexistente);</li>
 *   <li>acessibilidade com projeto FINALIZADO (RN-015).</li>
 * </ul>
 *
 * <p>401 (não autenticado) não é exercitado aqui — mesmo padrão do
 * {@code TarefaControllerBoardIntegrationTest}: a cadeia de filtros é desabilitada
 * ({@code addFilters = false}); a exigência de autenticação é garantida globalmente por
 * {@code SecurityConfig.anyRequest().authenticated()}.
 */
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class DashboardControllerIntegrationTest extends IntegrationTestBase {

    private static final Instant T0 = Instant.parse("2026-08-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private PermissaoGuard permissaoGuard;

    @Autowired private ProjetoRepository projetoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private EtapaRepository etapaRepository;
    @Autowired private RaiaRepository raiaRepository;
    @Autowired private TarefaRepository tarefaRepository;
    @Autowired private TarefaEtapaHistoricoRepository etapaHistoricoRepository;
    @Autowired private TarefaImpedimentoHistoricoRepository impedimentoHistoricoRepository;

    private UUID projetoId;
    private Projeto projeto;
    private Usuario criador;
    private Workflow workflow;
    private Etapa e1;
    private Etapa e2;
    private Raia raia;

    @BeforeEach
    void setup() {
        when(permissaoGuard.membro(any(UUID.class))).thenReturn(true);

        criador = new Usuario();
        criador.setKeycloakSub("test-user-dashboard");
        criador.setNome("Test User Dashboard");
        criador.setEmail("test-dashboard@example.com");
        criador.setAtivo(true);
        criador = usuarioRepository.save(criador);
        UsuarioAutenticadoHolder.set(criador);

        projeto = new Projeto();
        projeto.setNome("Projeto Dashboard Test");
        projeto.setDescricao("Projeto para teste de dashboard");
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(criador);
        projeto.setCriadoEm(OffsetDateTime.now());
        projeto = projetoRepository.save(projeto);
        projetoId = projeto.getId();

        workflow = new Workflow();
        workflow.setProjeto(projeto);
        workflow.setNome("Workflow Dashboard");
        workflow = workflowRepository.save(workflow);

        e1 = novaEtapa("Fazendo", 1, false);
        e2 = novaEtapa("Revisão", 2, true);

        raia = new Raia();
        raia.setProjeto(projeto);
        raia.setNome("Única");
        raia.setOrdem(1);
        raia = raiaRepository.save(raia);
    }

    @AfterEach
    void clear() {
        UsuarioAutenticadoHolder.clear();
    }

    private Etapa novaEtapa(String nome, int ordem, boolean finalEtapa) {
        Etapa e = new Etapa();
        e.setWorkflow(workflow);
        e.setNome(nome);
        e.setOrdem(ordem);
        e.setEtapaFinal(finalEtapa);
        return etapaRepository.save(e);
    }

    private Tarefa novaTarefa(String titulo, Etapa etapaAtual) {
        Tarefa t = new Tarefa();
        t.setProjeto(projeto);
        t.setWorkflow(workflow);
        t.setEtapaAtual(etapaAtual);
        t.setRaia(raia);
        t.setTitulo(titulo);
        t.setCriadoPor(criador);
        t.setIniciada(true);
        t.setImpedida(false);
        t.setCriadoEm(Instant.now());
        t.setAtualizadoEm(Instant.now());
        return tarefaRepository.save(t);
    }

    private void hist(Tarefa tarefa, Etapa etapa, long entradaOffset, Long saidaOffset) {
        TarefaEtapaHistorico h = new TarefaEtapaHistorico();
        h.setTarefa(tarefa);
        h.setEtapa(etapa);
        h.setEntradaEm(T0.plusSeconds(entradaOffset));
        h.setSaidaEm(saidaOffset == null ? null : T0.plusSeconds(saidaOffset));
        etapaHistoricoRepository.save(h);
    }

    private void imped(Tarefa tarefa, long marcadoOffset, Long desmarcadoOffset) {
        TarefaImpedimentoHistorico i = new TarefaImpedimentoHistorico();
        i.setTarefa(tarefa);
        i.setMarcadoEm(T0.plusSeconds(marcadoOffset));
        i.setDesmarcadoEm(desmarcadoOffset == null ? null : T0.plusSeconds(desmarcadoOffset));
        impedimentoHistoricoRepository.save(i);
    }

    @Test
    void retorna200EAgregaLeadTimeEImpedimentoPorEtapa() throws Exception {
        Tarefa t1 = novaTarefa("T1", e2);
        Tarefa t2 = novaTarefa("T2", e2);
        hist(t1, e1, 0, 100L);      // T1/E1: 100s
        hist(t1, e2, 100, 300L);    // T1/E2: 200s
        hist(t2, e1, 0, 200L);      // T2/E1: 200s
        hist(t2, e2, 200, null);    // aberto -> ignorado
        imped(t1, 20, 50L);         // 30s dentro de T1/E1
        imped(t2, 100, 250L);       // overlap com T2/E1 [0,200] = 100s

        String body = mockMvc.perform(get("/api/projetos/" + projetoId + "/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        DashboardResponse r = objectMapper.readValue(body, DashboardResponse.class);
        assertThat(r.totalTarefasConsideradas()).isEqualTo(2);
        assertThat(r.leadTimeMedioPorEtapa()).hasSize(2);

        DashboardResponse.EtapaLeadTime etapa1 = r.leadTimeMedioPorEtapa().get(0);
        assertThat(etapa1.etapaNome()).isEqualTo("Fazendo");
        assertThat(etapa1.leadTimeMedioSegundos()).isEqualTo(150L);        // (100 + 200) / 2
        assertThat(etapa1.tempoImpedimentoMedioSegundos()).isEqualTo(65L); // (30 + 100) / 2

        DashboardResponse.EtapaLeadTime etapa2 = r.leadTimeMedioPorEtapa().get(1);
        assertThat(etapa2.leadTimeMedioSegundos()).isEqualTo(200L);
        assertThat(etapa2.tempoImpedimentoMedioSegundos()).isEqualTo(0L);
    }

    @Test
    void acessivelComProjetoFinalizado() throws Exception {
        projeto.setStatus(Projeto.Status.FINALIZADO);
        projetoRepository.save(projeto);
        Tarefa t1 = novaTarefa("T1", e1);
        hist(t1, e1, 0, 100L);

        String body = mockMvc.perform(get("/api/projetos/" + projetoId + "/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        DashboardResponse r = objectMapper.readValue(body, DashboardResponse.class);
        assertThat(r.leadTimeMedioPorEtapa().get(0).leadTimeMedioSegundos()).isEqualTo(100L);
    }

    @Test
    void semVinculoRetorna403() throws Exception {
        when(permissaoGuard.membro(any(UUID.class))).thenReturn(false);

        mockMvc.perform(get("/api/projetos/" + projetoId + "/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void projetoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/projetos/" + UUID.randomUUID() + "/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
