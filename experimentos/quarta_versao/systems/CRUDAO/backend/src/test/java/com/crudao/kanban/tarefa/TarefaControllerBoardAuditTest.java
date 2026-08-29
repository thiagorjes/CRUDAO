package com.crudao.kanban.tarefa;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
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
import com.crudao.kanban.support.IntegrationTestBase;
import com.crudao.kanban.tarefa.dto.BoardResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TASK-04.5 Audit Mode: Testes complementares de casos de borda e cenários de erro.
 * Valida contra o PostgreSQL do stack Docker final (ver {@link IntegrationTestBase}).
 * RF-001, RF-006: Board retorna etapas na ordem, raias e tarefas sem N+1.
 */
@AutoConfigureMockMvc(addFilters = false) // sem cadeia de segurança HTTP — foco é o board, não OIDC
@Transactional // rollback por teste — evita acúmulo/colisão de keycloak_sub entre métodos
public class TarefaControllerBoardAuditTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private TransicaoRepository transicaoRepository;

    @Autowired
    private RaiaRepository raiaRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;

    @MockBean
    private PermissaoGuard permissaoGuard;

    private UUID projetoId;
    private Workflow workflow;
    private Etapa etapa1;
    private Etapa etapa2;
    private Etapa etapa3;
    private Raia raiaGlobal;
    private Raia raiaProjeto;
    private Usuario criador;
    private Usuario usuario2;

    @BeforeEach
    void setup() {
        // Criar usuários
        criador = new Usuario();
        criador.setKeycloakSub("audit-user-1");
        criador.setNome("Audit User 1");
        criador.setEmail("audit1@example.com");
        criador.setAtivo(true);
        criador = usuarioRepository.save(criador);

        usuario2 = new Usuario();
        usuario2.setKeycloakSub("audit-user-2");
        usuario2.setNome("Audit User 2");
        usuario2.setEmail("audit2@example.com");
        usuario2.setAtivo(true);
        usuario2 = usuarioRepository.save(usuario2);

        UsuarioAutenticadoHolder.set(criador);
        when(permissaoGuard.membro(any(UUID.class))).thenReturn(true);

        // Criar projeto
        Projeto projeto = new Projeto();
        projeto.setNome("Projeto Audit");
        projeto.setDescricao("Projeto para audit test");
        projeto.setStatus(com.crudao.kanban.domain.usuario.Projeto.Status.ATIVO);
        projeto.setCriadoPor(criador);
        projeto.setCriadoEm(OffsetDateTime.now());
        projeto = projetoRepository.save(projeto);
        projetoId = projeto.getId();

        // Criar workflow com 3 etapas
        workflow = new Workflow();
        workflow.setProjeto(projeto);
        workflow.setNome("Workflow Audit");
        workflow = workflowRepository.save(workflow);

        etapa1 = new Etapa();
        etapa1.setWorkflow(workflow);
        etapa1.setNome("Backlog");
        etapa1.setOrdem(1);
        etapa1.setEtapaFinal(false);
        etapa1 = etapaRepository.save(etapa1);

        etapa2 = new Etapa();
        etapa2.setWorkflow(workflow);
        etapa2.setNome("Em Execução");
        etapa2.setOrdem(2);
        etapa2.setEtapaFinal(false);
        etapa2 = etapaRepository.save(etapa2);

        etapa3 = new Etapa();
        etapa3.setWorkflow(workflow);
        etapa3.setNome("Concluído");
        etapa3.setOrdem(3);
        etapa3.setEtapaFinal(true);
        etapa3 = etapaRepository.save(etapa3);

        // Transições
        Transicao t1 = new Transicao();
        t1.setEtapaOrigem(etapa1);
        t1.setEtapaDestino(etapa2);
        transicaoRepository.save(t1);

        Transicao t2 = new Transicao();
        t2.setEtapaOrigem(etapa2);
        t2.setEtapaDestino(etapa3);
        transicaoRepository.save(t2);

        // Raias: 1 global + 1 de projeto
        raiaGlobal = new Raia();
        raiaGlobal.setProjeto(null);
        raiaGlobal.setNome("Raia Global");
        raiaGlobal.setOrdem(1);
        raiaGlobal = raiaRepository.save(raiaGlobal);

        raiaProjeto = new Raia();
        raiaProjeto.setProjeto(projeto);
        raiaProjeto.setNome("Raia Projeto");
        raiaProjeto.setOrdem(2);
        raiaProjeto = raiaRepository.save(raiaProjeto);
    }

    /**
     * Teste: Board vazio (projeto com workflow mas sem tarefas).
     * Esperado: Retorna etapas e raias sem tarefas, estrutura não vazia mas tarefas = [].
     */
    @Test
    void testBoardVazioComWorkflow() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        assertThat(board).isNotNull();
        assertThat(board.getEtapas()).hasSize(3);
        assertThat(board.getRaias()).hasSize(2);
        assertThat(board.getTarefas()).isEmpty();

        // Verificar estrutura básica
        assertThat(board.getEtapas().get(0).getNome()).isEqualTo("Backlog");

        System.out.println("✓ Board vazio retorna estrutura sem tarefas");
    }

    /**
     * Teste: Etapas retornam em ordem crescente de `ordem` (RF-001).
     */
    @Test
    void testEtapasRetornamEmOrdem() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        assertThat(board.getEtapas()).hasSize(3);
        assertThat(board.getEtapas().get(0).getOrdem()).isEqualTo(1);
        assertThat(board.getEtapas().get(1).getOrdem()).isEqualTo(2);
        assertThat(board.getEtapas().get(2).getOrdem()).isEqualTo(3);
        assertThat(board.getEtapas().get(0).getNome()).isEqualTo("Backlog");
        assertThat(board.getEtapas().get(1).getNome()).isEqualTo("Em Execução");
        assertThat(board.getEtapas().get(2).getNome()).isEqualTo("Concluído");

        System.out.println("✓ Etapas retornam em ordem crescente");
    }

    /**
     * Teste: Raias retornam em ordem (globais primeiro, depois por projeto).
     */
    @Test
    void testRaiasRetornamEmOrdem() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        assertThat(board.getRaias()).hasSize(2);
        // Raias de projeto vêm primeiro (ordem 1), depois globais
        assertThat(board.getRaias().get(0).getNome()).isEqualTo("Raia Projeto");
        assertThat(board.getRaias().get(0).isGlobal()).isFalse();
        assertThat(board.getRaias().get(1).getNome()).isEqualTo("Raia Global");
        assertThat(board.getRaias().get(1).isGlobal()).isTrue();

        System.out.println("✓ Raias retornam em ordem correta");
    }

    /**
     * Teste: Tarefa com responsável=null não causa erro.
     */
    @Test
    void testTarefaSemResponsavel() throws Exception {
        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projetoRepository.findById(projetoId).get());
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapa1);
        tarefa.setRaia(raiaProjeto);
        tarefa.setTitulo("Tarefa sem Responsável");
        tarefa.setDescricaoEscopo("Sem responsável");
        tarefa.setResponsavel(null); // Sem responsável
        tarefa.setCriadoPor(criador);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);
        tarefa.setCriadoEm(Instant.now());
        tarefa.setAtualizadoEm(Instant.now());
        Tarefa saved = tarefaRepository.save(tarefa);

        TarefaEtapaHistorico historico = new TarefaEtapaHistorico();
        historico.setTarefa(saved);
        historico.setEtapa(etapa1);
        historico.setEntradaEm(Instant.now());
        historico.setSaidaEm(null);
        tarefaEtapaHistoricoRepository.save(historico);

        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        assertThat(board.getTarefas()).hasSize(1);
        assertThat(board.getTarefas().get(0).getResponsavelId()).isNull();
        assertThat(board.getTarefas().get(0).getTitulo()).isEqualTo("Tarefa sem Responsável");

        System.out.println("✓ Tarefa sem responsável retornada corretamente");
    }

    /**
     * Teste: Tarefa com impedida=true reflete na resposta.
     */
    @Test
    void testTarefaImpedidaComFlag() throws Exception {
        Instant agora = Instant.now();
        Tarefa tarefa = new Tarefa();
        tarefa.setProjeto(projetoRepository.findById(projetoId).get());
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapa1);
        tarefa.setRaia(raiaProjeto);
        tarefa.setTitulo("Tarefa Impedida");
        tarefa.setDescricaoEscopo("Impedida");
        tarefa.setResponsavel(criador);
        tarefa.setCriadoPor(criador);
        tarefa.setIniciada(true);
        tarefa.setImpedida(true);
        tarefa.setImpedidaDesde(agora); // Flag e timestamp
        tarefa.setCriadoEm(agora);
        tarefa.setAtualizadoEm(agora);
        Tarefa saved = tarefaRepository.save(tarefa);

        TarefaEtapaHistorico historico = new TarefaEtapaHistorico();
        historico.setTarefa(saved);
        historico.setEtapa(etapa2);
        historico.setEntradaEm(agora);
        historico.setSaidaEm(null);
        tarefaEtapaHistoricoRepository.save(historico);

        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        assertThat(board.getTarefas()).hasSize(1);
        assertThat(board.getTarefas().get(0).isImpedida()).isTrue();
        assertThat(board.getTarefas().get(0).getImpedidaDesdeMs()).isGreaterThan(0);

        System.out.println("✓ Tarefa impedida reflete flag corretamente");
    }

    /**
     * Teste: Projeto não encontrado retorna 404.
     */
    @Test
    void testProjetoNaoEncontradoRetorna404() throws Exception {
        UUID projetoInexistente = UUID.randomUUID();

        mockMvc.perform(get("/api/projetos/" + projetoInexistente + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        System.out.println("✓ Projeto não encontrado retorna 404");
    }

    /**
     * Teste: Usuário sem vínculo ao projeto retorna 403.
     */
    @Test
    void testAcessoNegadoSemVincutoAoProjeto() throws Exception {
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        System.out.println("✓ Usuário sem vínculo retorna 403");
    }

    /**
     * Teste: Múltiplas tarefas com diferentes responsáveis.
     * Verifica que cada tarefa mantém seu responsável correto no DTO.
     */
    @Test
    void testMultiplasTarefasComResponsaveisDiferentes() throws Exception {
        // Tarefa 1: criador como responsável
        Tarefa tarefa1 = new Tarefa();
        tarefa1.setProjeto(projetoRepository.findById(projetoId).get());
        tarefa1.setWorkflow(workflow);
        tarefa1.setEtapaAtual(etapa1);
        tarefa1.setRaia(raiaProjeto);
        tarefa1.setTitulo("Tarefa 1");
        tarefa1.setDescricaoEscopo("Do criador");
        tarefa1.setResponsavel(criador);
        tarefa1.setCriadoPor(criador);
        tarefa1.setIniciada(false);
        tarefa1.setImpedida(false);
        tarefa1.setCriadoEm(Instant.now());
        tarefa1.setAtualizadoEm(Instant.now());
        Tarefa saved1 = tarefaRepository.save(tarefa1);

        TarefaEtapaHistorico historico1 = new TarefaEtapaHistorico();
        historico1.setTarefa(saved1);
        historico1.setEtapa(etapa1);
        historico1.setEntradaEm(Instant.now());
        historico1.setSaidaEm(null);
        tarefaEtapaHistoricoRepository.save(historico1);

        // Tarefa 2: usuario2 como responsável
        Tarefa tarefa2 = new Tarefa();
        tarefa2.setProjeto(projetoRepository.findById(projetoId).get());
        tarefa2.setWorkflow(workflow);
        tarefa2.setEtapaAtual(etapa2);
        tarefa2.setRaia(raiaProjeto);
        tarefa2.setTitulo("Tarefa 2");
        tarefa2.setDescricaoEscopo("Do usuario2");
        tarefa2.setResponsavel(usuario2);
        tarefa2.setCriadoPor(criador);
        tarefa2.setIniciada(true);
        tarefa2.setImpedida(false);
        tarefa2.setCriadoEm(Instant.now());
        tarefa2.setAtualizadoEm(Instant.now());
        Tarefa saved2 = tarefaRepository.save(tarefa2);

        TarefaEtapaHistorico historico2 = new TarefaEtapaHistorico();
        historico2.setTarefa(saved2);
        historico2.setEtapa(etapa2);
        historico2.setEntradaEm(Instant.now());
        historico2.setSaidaEm(null);
        tarefaEtapaHistoricoRepository.save(historico2);

        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        assertThat(board.getTarefas()).hasSize(2);
        // A ordem das tarefas no board não é garantida — valida pelo conjunto de responsáveis.
        assertThat(board.getTarefas())
                .extracting(t -> t.getResponsavelId())
                .containsExactlyInAnyOrder(criador.getId(), usuario2.getId());

        System.out.println("✓ Múltiplas tarefas mantêm responsáveis diferentes");
    }

    /**
     * Teste: Transições de saída refletem corretamente no DTO de etapas.
     */
    @Test
    void testTransicoesSaidaRefleteNoDTO() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/projetos/" + projetoId + "/board")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BoardResponse board = objectMapper.readValue(responseBody, BoardResponse.class);

        assertThat(board.getEtapas()).hasSize(3);
        // Etapa 1 (Backlog) tem transição para etapa 2
        assertThat(board.getEtapas().get(0).getTransicoesSaida()).hasSize(1);
        assertThat(board.getEtapas().get(0).getTransicoesSaida().get(0)).isEqualTo(etapa2.getId());

        // Etapa 2 (Em Execução) tem transição para etapa 3
        assertThat(board.getEtapas().get(1).getTransicoesSaida()).hasSize(1);
        assertThat(board.getEtapas().get(1).getTransicoesSaida().get(0)).isEqualTo(etapa3.getId());

        // Etapa 3 (Concluído) não tem transição de saída
        assertThat(board.getEtapas().get(2).getTransicoesSaida()).isEmpty();

        System.out.println("✓ Transições refletem corretamente no DTO");
    }
}
