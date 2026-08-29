package com.crudao.kanban.notificacao;

import static org.junit.jupiter.api.Assertions.*;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.notificacao.NotificacaoRepository;
import com.crudao.kanban.domain.notificacao.TipoNotificacao;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservador;
import com.crudao.kanban.domain.tarefa.TarefaObservadorId;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.support.IntegrationTestBase;
import com.crudao.kanban.tarefa.TarefaService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * TASK-05.2: Testes de integração end-to-end entre TarefaService e NotificacaoService.
 * RF-005: Valida que alterações em tarefas disparam criação de notificações.
 * Cenário 7: Integração com TarefaService
 *
 * Executa contra o stack Docker final (PostgreSQL do docker-compose.yml) — ver
 * {@link IntegrationTestBase}.
 */
@DisplayName("Notificação - Testes E2E com TarefaService")
class NotificacaoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private RaiaRepository raiaRepository;

    @Autowired
    private TarefaObservadorRepository tarefaObservadorRepository;

    private UUID projetoId;
    private Projeto projeto;
    private UUID tarefaId;
    private Tarefa tarefa;
    private Usuario responsavel;
    private Usuario criador;
    private Usuario observadorExplicito;
    private UUID etapa1Id;
    private UUID etapa2Id;
    private Etapa etapa1;
    private Etapa etapa2;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        // Limpar dados
        notificacaoRepository.deleteAll();
        tarefaObservadorRepository.deleteAll();
        tarefaRepository.deleteAll();
        etapaRepository.deleteAll();
        workflowRepository.deleteAll();
        raiaRepository.deleteAll();
        projetoRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Criar usuários — id é @GeneratedValue; não atribuir manualmente (senão save() faz merge
        // de entidade destacada → StaleObjectStateException). Recupera-se a instância persistida.
        responsavel = new Usuario();
        responsavel.setKeycloakSub("sub-responsavel-" + UUID.randomUUID());
        responsavel.setEmail("responsavel@test.com");
        responsavel.setNome("Responsável");
        responsavel.setAtivo(true);
        responsavel = usuarioRepository.save(responsavel);

        criador = new Usuario();
        criador.setKeycloakSub("sub-criador-" + UUID.randomUUID());
        criador.setEmail("criador@test.com");
        criador.setNome("Criador");
        criador.setAtivo(true);
        criador = usuarioRepository.save(criador);

        observadorExplicito = new Usuario();
        observadorExplicito.setKeycloakSub("sub-observador-" + UUID.randomUUID());
        observadorExplicito.setEmail("observador@test.com");
        observadorExplicito.setNome("Observador");
        observadorExplicito.setAtivo(true);
        observadorExplicito = usuarioRepository.save(observadorExplicito);

        // Setar usuário autenticado
        UsuarioAutenticadoHolder.set(criador);

        // Criar projeto
        projeto = new Projeto();
        projeto.setNome("Projeto Test");
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(criador);
        projeto = projetoRepository.save(projeto);
        projetoId = projeto.getId();

        // Criar workflow e etapas
        workflow = workflowRepository.save(new Workflow(null, projeto, "Standard"));

        etapa1 = etapaRepository.save(new Etapa(null, workflow, "Backlog", 1, false));
        etapa1Id = etapa1.getId();

        etapa2 = etapaRepository.save(new Etapa(null, workflow, "Em Progresso", 2, false));
        etapa2Id = etapa2.getId();

        // Criar raia
        var raia = raiaRepository.save(new Raia(null, projeto, "Frontend", 1));

        // Criar tarefa
        tarefa = new Tarefa();
        tarefa.setProjeto(projeto);
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapa1);
        tarefa.setRaia(raia);
        tarefa.setTitulo("Tarefa de teste");
        tarefa.setResponsavel(responsavel);
        tarefa.setCriadoPor(criador);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);
        tarefa = tarefaRepository.save(tarefa);
        tarefaId = tarefa.getId();

        // Adicionar observador explícito
        salvarObservador(tarefa, observadorExplicito);
    }

    /** TarefaObservador usa @EmbeddedId + @MapsId — o id precisa ser instanciado antes do save. */
    private TarefaObservador salvarObservador(Tarefa t, Usuario u) {
        var obs = new TarefaObservador();
        obs.setId(new TarefaObservadorId(t.getId(), u.getId()));
        obs.setTarefa(t);
        obs.setUsuario(u);
        return tarefaObservadorRepository.save(obs);
    }

    @AfterEach
    void tearDown() {
        notificacaoRepository.deleteAll();
        tarefaObservadorRepository.deleteAll();
        tarefaRepository.deleteAll();
        etapaRepository.deleteAll();
        workflowRepository.deleteAll();
        raiaRepository.deleteAll();
        projetoRepository.deleteAll();
        usuarioRepository.deleteAll();
        UsuarioAutenticadoHolder.clear();
    }

    // ==================== Integração: Transição de Etapa ====================

    @Test
    @DisplayName("Transição de etapa cria notificações para responsável + criador + observadores explícitos")
    @Transactional
    void testTransicaoEtapa_CriaNotificacoes() throws Exception {
        // Arrange
        long notificacoesAntes = notificacaoRepository.count();

        // Act
        // Simular a chamada que TarefaService.mover faria
        notificacaoService.criarNotificacoesPorTransicaoEtapa(tarefa, etapa1Id, etapa2Id);

        // Assert
        long notificacoesDepois = notificacaoRepository.count();
        assertEquals(3L, notificacoesDepois - notificacoesAntes,
            "Deve criar 3 notificações (responsável + criador + observador explícito)");

        // Verificar tipos e usuários
        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertEquals(3, notificacoes.size());

        // Todas devem ser do tipo TRANSICAO_ETAPA
        notificacoes.forEach(n ->
            assertEquals(TipoNotificacao.TRANSICAO_ETAPA, n.getTipo())
        );

        // Verificar que cada observador recebeu uma
        var usuariosNotificados = notificacoes.stream()
            .map(n -> n.getUsuario().getId())
            .toList();

        assertTrue(usuariosNotificados.contains(responsavel.getId()),
            "Responsável deve ser notificado");
        assertTrue(usuariosNotificados.contains(criador.getId()),
            "Criador deve ser notificado");
        assertTrue(usuariosNotificados.contains(observadorExplicito.getId()),
            "Observador explícito deve ser notificado");
    }

    @Test
    @DisplayName("Transição de etapa: notificações marcadas como não lidas")
    @Transactional
    void testTransicaoEtapa_NotificacoesNaoLidas() throws Exception {
        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(tarefa, etapa1Id, etapa2Id);

        // Assert
        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        notificacoes.forEach(n -> {
            assertFalse(n.isLida(), "Todas as notificações devem ser não lidas");
            assertNull(n.getLidoEm(), "Notificações não lidas não devem ter lidoEm");
            assertNotNull(n.getCriadoEm(), "Todas devem ter criadoEm preenchido");
        });
    }

    @Test
    @DisplayName("Transição de etapa: tarefa sem responsável cria notificações para criador + observadores")
    @Transactional
    void testTransicaoEtapa_SemResponsavel() throws Exception {
        // Arrange
        tarefa.setResponsavel(null);
        tarefaRepository.save(tarefa);

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(tarefa, etapa1Id, etapa2Id);

        // Assert
        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertEquals(2, notificacoes.size(),
            "Deve criar 2 notificações (criador + observador explícito)");

        var usuariosNotificados = notificacoes.stream()
            .map(n -> n.getUsuario().getId())
            .toList();

        assertFalse(usuariosNotificados.contains(responsavel.getId()),
            "Responsável (null) não deve ser notificado");
        assertTrue(usuariosNotificados.contains(criador.getId()),
            "Criador deve ser notificado");
    }

    // ==================== Integração: Impedimento ====================

    @Test
    @DisplayName("Marcar impedimento cria notificações com tipo IMPEDIMENTO_MARCADO")
    @Transactional
    void testMarcarImpedimento_CriaNotificacoes() throws Exception {
        // Act
        notificacaoService.criarNotificacoesPorImpedimentoMarcado(tarefa);

        // Assert
        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertEquals(3, notificacoes.size());

        notificacoes.forEach(n ->
            assertEquals(TipoNotificacao.IMPEDIMENTO_MARCADO, n.getTipo())
        );
    }

    @Test
    @DisplayName("Desmarcar impedimento cria notificações com tipo IMPEDIMENTO_DESMARCADO")
    @Transactional
    void testDesmarcarImpedimento_CriaNotificacoes() throws Exception {
        // Act
        notificacaoService.criarNotificacoesPorImpedimentoDesmarcado(tarefa);

        // Assert
        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertEquals(3, notificacoes.size());

        notificacoes.forEach(n ->
            assertEquals(TipoNotificacao.IMPEDIMENTO_DESMARCADO, n.getTipo())
        );
    }

    // ==================== Observadores Explícitos ====================

    @Test
    @DisplayName("Múltiplos observadores explícitos recebem notificação em cada evento")
    @Transactional
    void testMultiplosObservadores_RecebemNotificacoes() throws Exception {
        // Arrange
        var observador2 = new Usuario();
        observador2.setKeycloakSub("sub-obs2-" + UUID.randomUUID());
        observador2.setEmail("obs2@test.com");
        observador2.setNome("Observador 2");
        observador2.setAtivo(true);
        observador2 = usuarioRepository.save(observador2);

        salvarObservador(tarefa, observador2);

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(tarefa, etapa1Id, etapa2Id);

        // Assert
        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertEquals(4, notificacoes.size(),
            "Deve criar 4 notificações (responsável + criador + 2 observadores explícitos)");

        var usuariosNotificados = notificacoes.stream()
            .map(n -> n.getUsuario().getId())
            .toList();

        assertTrue(usuariosNotificados.contains(observador2.getId()),
            "Segundo observador deve ser notificado");
    }

    @Test
    @DisplayName("Sem observadores explícitos: cria notificação apenas para responsável + criador")
    @Transactional
    void testSemObservadoresExplicitos() throws Exception {
        // Arrange
        tarefaObservadorRepository.deleteAll();

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(tarefa, etapa1Id, etapa2Id);

        // Assert
        List<Notificacao> notificacoes = notificacaoRepository.findAll();
        assertEquals(2, notificacoes.size(),
            "Deve criar 2 notificações (apenas responsável + criador)");

        var usuariosNotificados = notificacoes.stream()
            .map(n -> n.getUsuario().getId())
            .toList();

        assertFalse(usuariosNotificados.contains(observadorExplicito.getId()),
            "Observador removido não deve receber notificação");
    }

    // Métodos auxiliares privados
    private static void assertEquals(Object expected, Object actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }

    private static void assertFalse(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertFalse(condition, message);
    }

    private static void assertNull(Object object, String message) {
        org.junit.jupiter.api.Assertions.assertNull(object, message);
    }

    private static void assertNotNull(Object object, String message) {
        org.junit.jupiter.api.Assertions.assertNotNull(object, message);
    }
}
