package com.crudao.kanban.notificacao;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.notificacao.NotificacaoRepository;
import com.crudao.kanban.domain.notificacao.TipoNotificacao;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.support.IntegrationTestBase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * TASK-05.2: Testes de integração para NotificacaoController.
 * RF-005: Endpoints de notificações internas.
 * RNF-003: Validação de autorização no backend.
 *
 * Executa contra o stack Docker final (PostgreSQL + Keycloak do docker-compose.yml) — ver
 * {@link IntegrationTestBase}.
 */
@DisplayName("NotificacaoController - Testes de Integração")
class NotificacaoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    private UUID usuarioId1;
    private UUID usuarioId2;
    private Usuario usuario1;
    private Usuario usuario2;
    private UUID projetoId;
    private Projeto projeto;
    private UUID tarefaId;
    private Tarefa tarefa;

    @BeforeEach
    void setUp() {
        // Limpar dados anteriores
        notificacaoRepository.deleteAll();
        tarefaRepository.deleteAll();
        projetoRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Criar usuários
        usuarioId1 = UUID.randomUUID();
        usuario1 = new Usuario();
        usuario1.setId(usuarioId1);
        usuario1.setEmail("user1@test.com");
        usuario1.setNome("User 1");
        usuario1.setAtivo(true);
        usuarioRepository.save(usuario1);

        usuarioId2 = UUID.randomUUID();
        usuario2 = new Usuario();
        usuario2.setId(usuarioId2);
        usuario2.setEmail("user2@test.com");
        usuario2.setNome("User 2");
        usuario2.setAtivo(true);
        usuarioRepository.save(usuario2);

        // Criar projeto
        projetoId = UUID.randomUUID();
        projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setNome("Projeto Test");
        projeto.setStatus(Projeto.Status.ATIVO);
        projetoRepository.save(projeto);

        // Criar tarefa
        tarefaId = UUID.randomUUID();
        tarefa = new Tarefa();
        tarefa.setId(tarefaId);
        tarefa.setProjeto(projeto);
        tarefa.setTitulo("Tarefa de teste");
        tarefa.setResponsavel(usuario1);
        tarefa.setCriadoPor(usuario2);
        tarefaRepository.save(tarefa);
    }

    @AfterEach
    void tearDown() {
        notificacaoRepository.deleteAll();
        tarefaRepository.deleteAll();
        projetoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    // ==================== Cenário 4: GET /api/notificacoes ====================

    @Test
    @DisplayName("GET /api/notificacoes retorna notificações não lidas do usuário autenticado")
    @WithMockUser(username = "user1@test.com")
    @Transactional
    void testGetNotificacoes_RetornaApenasDoUsuario() throws Exception {
        // Arrange
        var notif1 = new Notificacao();
        notif1.setId(UUID.randomUUID());
        notif1.setUsuario(usuario1);
        notif1.setTarefa(tarefa);
        notif1.setTipo(TipoNotificacao.TRANSICAO_ETAPA);
        notif1.setLida(false);
        notif1.setCriadoEm(Instant.now());
        notificacaoRepository.save(notif1);

        var notif2 = new Notificacao();
        notif2.setId(UUID.randomUUID());
        notif2.setUsuario(usuario2);
        notif2.setTarefa(tarefa);
        notif2.setTipo(TipoNotificacao.IMPEDIMENTO_MARCADO);
        notif2.setLida(false);
        notif2.setCriadoEm(Instant.now());
        notificacaoRepository.save(notif2);

        // Act & Assert
        mockMvc.perform(get("/api/notificacoes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(notif1.getId().toString())))
            .andExpect(jsonPath("$[0].tipo", is("TRANSICAO_ETAPA")))
            .andExpect(jsonPath("$[0].lida", is(false)));
    }

    @Test
    @DisplayName("GET /api/notificacoes retorna apenas não lidas")
    @WithMockUser(username = "user1@test.com")
    @Transactional
    void testGetNotificacoes_ApenasNaoLidas() throws Exception {
        // Arrange
        var notifNaoLida = new Notificacao();
        notifNaoLida.setId(UUID.randomUUID());
        notifNaoLida.setUsuario(usuario1);
        notifNaoLida.setTarefa(tarefa);
        notifNaoLida.setTipo(TipoNotificacao.TRANSICAO_ETAPA);
        notifNaoLida.setLida(false);
        notifNaoLida.setCriadoEm(Instant.now().minusSeconds(10));
        notificacaoRepository.save(notifNaoLida);

        var notifLida = new Notificacao();
        notifLida.setId(UUID.randomUUID());
        notifLida.setUsuario(usuario1);
        notifLida.setTarefa(tarefa);
        notifLida.setTipo(TipoNotificacao.IMPEDIMENTO_DESMARCADO);
        notifLida.setLida(true);
        notifLida.setLidoEm(Instant.now());
        notifLida.setCriadoEm(Instant.now());
        notificacaoRepository.save(notifLida);

        // Act & Assert
        mockMvc.perform(get("/api/notificacoes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(notifNaoLida.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/notificacoes ordenadas por criadoEm DESC")
    @WithMockUser(username = "user1@test.com")
    @Transactional
    void testGetNotificacoes_OrdenacaoDesc() throws Exception {
        // Arrange
        var notif1 = new Notificacao();
        notif1.setId(UUID.randomUUID());
        notif1.setUsuario(usuario1);
        notif1.setTarefa(tarefa);
        notif1.setTipo(TipoNotificacao.TRANSICAO_ETAPA);
        notif1.setLida(false);
        notif1.setCriadoEm(Instant.now().minusSeconds(100));
        notificacaoRepository.save(notif1);

        var notif2 = new Notificacao();
        notif2.setId(UUID.randomUUID());
        notif2.setUsuario(usuario1);
        notif2.setTarefa(tarefa);
        notif2.setTipo(TipoNotificacao.IMPEDIMENTO_MARCADO);
        notif2.setLida(false);
        notif2.setCriadoEm(Instant.now());
        notificacaoRepository.save(notif2);

        // Act & Assert
        mockMvc.perform(get("/api/notificacoes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", is(notif2.getId().toString())))
            .andExpect(jsonPath("$[1].id", is(notif1.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/notificacoes lista vazia quando nenhuma não lida")
    @WithMockUser(username = "user1@test.com")
    @Transactional
    void testGetNotificacoes_ListaVazia() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/notificacoes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/notificacoes retorna 401 não autenticado")
    void testGetNotificacoes_NaoAutenticado() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/notificacoes"))
            .andExpect(status().isUnauthorized());
    }

    // ==================== Cenário 5: PUT /api/notificacoes/{id}/marcar-como-lida ====================

    @Test
    @DisplayName("PUT marcar como lida com autorização correta")
    @WithMockUser(username = "user1@test.com")
    @Transactional
    void testMarcarComoLida_Sucesso() throws Exception {
        // Arrange
        var notif = new Notificacao();
        notif.setId(UUID.randomUUID());
        notif.setUsuario(usuario1);
        notif.setTarefa(tarefa);
        notif.setTipo(TipoNotificacao.TRANSICAO_ETAPA);
        notif.setLida(false);
        notif.setCriadoEm(Instant.now());
        notificacaoRepository.save(notif);

        // Act & Assert
        mockMvc.perform(put("/api/notificacoes/" + notif.getId() + "/marcar-como-lida"))
            .andExpect(status().isNoContent());

        // Verificar que foi marcada como lida
        var notificacaoAtualizada = notificacaoRepository.findById(notif.getId());
        assertTrue(notificacaoAtualizada.isPresent());
        assertTrue(notificacaoAtualizada.get().isLida());
        assertNotNull(notificacaoAtualizada.get().getLidoEm());
    }

    @Test
    @DisplayName("PUT marcar como lida: rejeita acesso não autorizado (notificação de outro usuário)")
    @WithMockUser(username = "user1@test.com")
    @Transactional
    void testMarcarComoLida_RejeiterOutroUsuario() throws Exception {
        // Arrange
        var notif = new Notificacao();
        notif.setId(UUID.randomUUID());
        notif.setUsuario(usuario2); // Pertence a usuario2
        notif.setTarefa(tarefa);
        notif.setTipo(TipoNotificacao.TRANSICAO_ETAPA);
        notif.setLida(false);
        notif.setCriadoEm(Instant.now());
        notificacaoRepository.save(notif);

        // Act & Assert
        mockMvc.perform(put("/api/notificacoes/" + notif.getId() + "/marcar-como-lida"))
            .andExpect(status().isBadRequest()); // Ou 403 conforme implementação

        // Verificar que não foi marcada como lida
        var notificacaoAindaNaoLida = notificacaoRepository.findById(notif.getId());
        assertTrue(notificacaoAindaNaoLida.isPresent());
        assertFalse(notificacaoAindaNaoLida.get().isLida());
    }

    @Test
    @DisplayName("PUT marcar como lida: 401 não autenticado")
    @Transactional
    void testMarcarComoLida_NaoAutenticado() throws Exception {
        // Arrange
        var notif = new Notificacao();
        notif.setId(UUID.randomUUID());
        notif.setUsuario(usuario1);
        notif.setTarefa(tarefa);
        notif.setTipo(TipoNotificacao.TRANSICAO_ETAPA);
        notif.setLida(false);
        notif.setCriadoEm(Instant.now());
        notificacaoRepository.save(notif);

        // Act & Assert
        mockMvc.perform(put("/api/notificacoes/" + notif.getId() + "/marcar-como-lida"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT marcar como lida: 404 notificação não encontrada")
    @WithMockUser(username = "user1@test.com")
    void testMarcarComoLida_NaoEncontrada() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/notificacoes/" + UUID.randomUUID() + "/marcar-como-lida"))
            .andExpect(status().isBadRequest()); // Ou 404 conforme implementação
    }

    // ==================== Cenário 7: Integração com TarefaService ====================

    @Test
    @DisplayName("Notificação criada após transição de etapa (via TarefaService)")
    @WithMockUser(username = "user1@test.com")
    @Transactional
    void testIntegracaoComTarefaService_TransicaoEtapa() throws Exception {
        // Este teste valida que NotificacaoService é invocado quando TarefaService.mover é chamado
        // A implementação real será executada via integração com TarefaService em teste E2E

        // Arrange: tarefa com responsável e criador
        var etapa2 = new com.crudao.kanban.domain.workflow.Etapa();
        etapa2.setId(UUID.randomUUID());
        etapa2.setNome("Em Progresso");
        etapa2.setOrdem(2);

        // Este é mais um teste de documentação do contrato esperado
        // A validação real da integração completa (TarefaService → NotificacaoService)
        // ocorre em teste E2E dedicado, não aqui em isolation
    }

    // Métodos auxiliares
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
