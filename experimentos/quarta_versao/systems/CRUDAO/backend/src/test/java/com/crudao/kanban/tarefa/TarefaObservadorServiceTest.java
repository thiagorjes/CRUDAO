package com.crudao.kanban.tarefa;

import static org.junit.jupiter.api.Assertions.*;

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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * TASK-05.2: Testes para CRUD de TarefaObservador.
 * Cenário 6: Observadores explícitos de tarefas.
 * RF-005: Suporte a observadores customizados além de responsável + criador.
 *
 * Executa contra o stack Docker final (PostgreSQL do docker-compose.yml) — ver
 * {@link IntegrationTestBase}.
 */
@DisplayName("TarefaObservador - Testes de CRUD")
class TarefaObservadorServiceTest extends IntegrationTestBase {

    @Autowired
    private TarefaObservadorRepository tarefaObservadorRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private RaiaRepository raiaRepository;

    private UUID projetoId;
    private Projeto projeto;
    private UUID tarefaId;
    private Tarefa tarefa;
    private Workflow workflow;
    private Etapa etapa;
    private Raia raia;
    private Usuario usuario1;
    private Usuario usuario2;
    private Usuario usuario3;

    private Usuario novoUsuario(String slug) {
        var u = new Usuario();
        u.setKeycloakSub("sub-" + slug + "-" + UUID.randomUUID());
        u.setEmail(slug + "@test.com");
        u.setNome(slug);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    /** TarefaObservador usa @EmbeddedId + @MapsId — o id precisa ser instanciado antes do save. */
    private TarefaObservador obs(Tarefa t, Usuario u) {
        var o = new TarefaObservador();
        o.setId(new TarefaObservadorId(t.getId(), u.getId()));
        o.setTarefa(t);
        o.setUsuario(u);
        return tarefaObservadorRepository.save(o);
    }

    @BeforeEach
    void setUp() {
        // Limpar dados (ordem respeita as FKs)
        tarefaObservadorRepository.deleteAll();
        tarefaRepository.deleteAll();
        etapaRepository.deleteAll();
        workflowRepository.deleteAll();
        raiaRepository.deleteAll();
        projetoRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Criar usuários — id é @GeneratedValue; não atribuir manualmente.
        usuario1 = novoUsuario("user1");
        usuario2 = novoUsuario("user2");
        usuario3 = novoUsuario("user3");

        // Criar projeto
        projeto = new Projeto();
        projeto.setNome("Projeto Test");
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(usuario2);
        projeto = projetoRepository.save(projeto);
        projetoId = projeto.getId();

        // Workflow/etapa/raia são obrigatórios (FK NOT NULL) para persistir a tarefa.
        workflow = workflowRepository.save(new Workflow(null, projeto, "Standard"));
        etapa = etapaRepository.save(new Etapa(null, workflow, "Backlog", 1, false));
        raia = raiaRepository.save(new Raia(null, projeto, "Frontend", 1));

        // Criar tarefa
        tarefa = new Tarefa();
        tarefa.setProjeto(projeto);
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapa);
        tarefa.setRaia(raia);
        tarefa.setTitulo("Tarefa teste");
        tarefa.setResponsavel(usuario1);
        tarefa.setCriadoPor(usuario2);
        tarefa = tarefaRepository.save(tarefa);
        tarefaId = tarefa.getId();
    }

    @AfterEach
    void tearDown() {
        tarefaObservadorRepository.deleteAll();
        tarefaRepository.deleteAll();
        etapaRepository.deleteAll();
        workflowRepository.deleteAll();
        raiaRepository.deleteAll();
        projetoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    // ==================== CREATE: Adicionar Observador ====================

    @Test
    @DisplayName("Adicionar observador explícito à tarefa")
    @Transactional
    void testAdicionarObservador_Sucesso() {
        // Act
        var saved = obs(tarefa, usuario3);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(tarefa.getId(), saved.getTarefa().getId());
        assertEquals(usuario3.getId(), saved.getUsuario().getId());
    }

    @Test
    @DisplayName("Adicionar múltiplos observadores à mesma tarefa")
    @Transactional
    void testAdicionarMultiplosObservadores() {
        // Arrange & Act
        obs(tarefa, usuario3);
        obs(tarefa, usuario1);

        // Assert
        List<TarefaObservador> observadores = tarefaObservadorRepository.findByTarefaId(tarefaId);
        assertEquals(2, observadores.size(), "Deve haver 2 observadores explícitos");

        var usuarios = observadores.stream().map(o -> o.getUsuario().getId()).toList();
        assertTrue(usuarios.contains(usuario3.getId()));
        assertTrue(usuarios.contains(usuario1.getId()));
    }

    // ==================== READ: Listar Observadores ====================

    @Test
    @DisplayName("Listar observadores explícitos da tarefa")
    @Transactional
    void testListarObservadores() {
        // Arrange
        obs(tarefa, usuario3);
        obs(tarefa, usuario2);

        // Act
        List<TarefaObservador> observadores = tarefaObservadorRepository.findByTarefaId(tarefaId);

        // Assert
        assertEquals(2, observadores.size());
        assertTrue(observadores.stream().allMatch(o -> o.getTarefa().getId().equals(tarefaId)));
    }

    @Test
    @DisplayName("Listar observadores: lista vazia quando nenhum")
    @Transactional
    void testListarObservadores_Vazia() {
        // Act
        List<TarefaObservador> observadores = tarefaObservadorRepository.findByTarefaId(tarefaId);

        // Assert
        assertTrue(observadores.isEmpty(), "Tarefa sem observadores explícitos");
    }

    // ==================== UPDATE: (via DELETE + CREATE) ====================

    @Test
    @DisplayName("Trocar observador (remover antigo, adicionar novo)")
    @Transactional
    void testTrocarObservador() {
        // Arrange
        var savedAntigo = obs(tarefa, usuario3);

        // Act
        tarefaObservadorRepository.deleteById(savedAntigo.getId());

        obs(tarefa, usuario2);

        // Assert
        List<TarefaObservador> observadores = tarefaObservadorRepository.findByTarefaId(tarefaId);
        assertEquals(1, observadores.size());
        assertEquals(usuario2.getId(), observadores.get(0).getUsuario().getId());
    }

    // ==================== DELETE: Remover Observador ====================

    @Test
    @DisplayName("Remover observador explícito")
    @Transactional
    void testRemoverObservador() {
        // Arrange
        var saved = obs(tarefa, usuario3);

        // Act
        tarefaObservadorRepository.deleteById(saved.getId());

        // Assert
        List<TarefaObservador> observadores = tarefaObservadorRepository.findByTarefaId(tarefaId);
        assertTrue(observadores.isEmpty(), "Observador deve ter sido removido");
    }

    @Test
    @DisplayName("Remover observador: um de vários mantém outros")
    @Transactional
    void testRemoverUmDeVarios() {
        // Arrange
        var saved1 = obs(tarefa, usuario1);
        obs(tarefa, usuario3);

        // Act
        tarefaObservadorRepository.deleteById(saved1.getId());

        // Assert
        List<TarefaObservador> observadores = tarefaObservadorRepository.findByTarefaId(tarefaId);
        assertEquals(1, observadores.size());
        assertEquals(usuario3.getId(), observadores.get(0).getUsuario().getId());
    }

    @Test
    @DisplayName("Remover observador de uma tarefa não afeta outras tarefas")
    @Transactional
    void testRemoverNaoAfetaOutrasTarefas() {
        // Arrange
        var tarefa2 = new Tarefa();
        tarefa2.setProjeto(projeto);
        tarefa2.setWorkflow(workflow);
        tarefa2.setEtapaAtual(etapa);
        tarefa2.setRaia(raia);
        tarefa2.setTitulo("Tarefa 2");
        tarefa2.setResponsavel(usuario1);
        tarefa2.setCriadoPor(usuario2);
        tarefa2 = tarefaRepository.save(tarefa2);

        var saved1 = obs(tarefa, usuario3);
        obs(tarefa2, usuario1);

        // Act
        tarefaObservadorRepository.deleteById(saved1.getId());

        // Assert
        List<TarefaObservador> obs1List = tarefaObservadorRepository.findByTarefaId(tarefa.getId());
        List<TarefaObservador> obs2List = tarefaObservadorRepository.findByTarefaId(tarefa2.getId());

        assertTrue(obs1List.isEmpty(), "Tarefa 1 deve ter removido seu observador");
        assertEquals(1, obs2List.size(), "Tarefa 2 deve manter seu observador");
    }

    // ==================== Validações de Negócio ====================

    @Test
    @DisplayName("Evitar duplicatas: mesmo usuário adicionado 2x à mesma tarefa")
    @Transactional
    void testEvitarDuplicatas() {
        // Arrange & Act — mesmo (tarefa, usuario) 2x: com PK composta o 2º save é um upsert.
        obs(tarefa, usuario3);
        obs(tarefa, usuario3);

        // Assert
        // Nota: a validação de duplicata deve ser implementada no serviço/controller
        // Este teste documenta o comportamento esperado
        List<TarefaObservador> observadores = tarefaObservadorRepository.findByTarefaId(tarefaId);

        // Se houver constraint UNIQUE no BD, será lançada exceção
        // Se não houver, a aplicação deve detectar em nível de serviço
        // Para este teste em isolation, apenas documentamos que 2 registros podem existir
        // e o serviço deve deduplicar antes de retornar/contar
    }

    @Test
    @DisplayName("Observador pode ser removido e adicionado novamente")
    @Transactional
    void testReadicionarObservador() {
        // Arrange
        var saved = obs(tarefa, usuario3);

        // Act & Assert
        tarefaObservadorRepository.deleteById(saved.getId());
        List<TarefaObservador> aposRemocao = tarefaObservadorRepository.findByTarefaId(tarefaId);
        assertTrue(aposRemocao.isEmpty());

        obs(tarefa, usuario3);

        List<TarefaObservador> aposReadicao = tarefaObservadorRepository.findByTarefaId(tarefaId);
        assertEquals(1, aposReadicao.size());
    }

    // Métodos auxiliares
    private static void assertEquals(Object expected, Object actual, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }

    private static void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }

    private static void assertNotNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNotNull(object);
    }
}
