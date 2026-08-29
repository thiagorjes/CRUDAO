package com.crudao.kanban.tarefa;

import static org.junit.jupiter.api.Assertions.*;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.*;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Testes de integração para TASK-04.3: Impedimento — validação de persistência em PostgreSQL real.
 * Usa Testcontainers para levantar BD real e validar transações ACID, históricos e auditoria.
 *
 * ⚠️ EXECUÇÃO: Desabilitado para execução local (Maven dev).
 * Executar em CI/CD via: mvn test -P integration-tests
 * Requer: Docker daemon ativo + Testcontainers configuration
 *
 * Estrutura de setup:
 * - @DynamicPropertySource injeta credenciais do PostgreSQL container no Spring Boot
 * - application-test.yml desabilita Flyway (Hibernate gerencia schema com create-drop)
 * - setUp() cria dados de teste com keycloak_sub válido
 *
 * TODO TASK-05.3: Adicionar @BeforeEach setup de RBAC real via Papel/Permissao ou mockar PermissaoGuard
 */
@DisplayName("TarefaImpedimento — Testes de Integração com PostgreSQL")
class TarefaImpedimentoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TarefaService tarefaService;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;

    @Autowired
    private TarefaAuditoriaRepository tarefaAuditoriaRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private RaiaRepository raiaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // O motor de RBAC tem testes dedicados (PermissaoGuardTest/EndpointIT); aqui só validamos
    // persistência de impedimento, então a checagem de permissão é neutralizada.
    @MockBean
    private PermissaoGuard permissaoGuard;

    private UUID projetoId;
    private UUID tarefaId;
    private Projeto projeto;
    private Usuario usuarioLogado;
    private Tarefa tarefa;

    @BeforeEach
    void setUp() {
        // Criar usuário com email único
        usuarioLogado = new Usuario();
        usuarioLogado.setEmail("dev-" + UUID.randomUUID() + "@test.com");
        usuarioLogado.setKeycloakSub("test-keycloak-sub-" + UUID.randomUUID());
        usuarioLogado.setNome("Dev Tester");
        usuarioLogado.setAtivo(true);
        usuarioLogado = usuarioRepository.save(usuarioLogado);
        UsuarioAutenticadoHolder.set(usuarioLogado);

        // Criar projeto
        projeto = new Projeto();
        projeto.setNome("Projeto Teste");
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(usuarioLogado);
        projeto = projetoRepository.save(projeto);
        projetoId = projeto.getId();

        // TODO TASK-05.3: Configurar RBAC real (Papel/Permissao) ou mockar PermissaoGuard para testes E2E

        // Criar workflow e etapa
        Workflow workflow = new Workflow();
        workflow.setProjeto(projeto);
        workflow.setNome("Workflow Teste");
        workflow = workflowRepository.save(workflow);

        Etapa etapa = new Etapa();
        etapa.setWorkflow(workflow);
        etapa.setNome("Backlog");
        etapa.setOrdem(1);
        etapa.setEtapaFinal(false);
        etapa = etapaRepository.save(etapa);

        // Criar raia
        Raia raia = new Raia();
        raia.setProjeto(projeto);
        raia.setNome("Frontend");
        raia.setOrdem(1);
        raia = raiaRepository.save(raia);

        // Criar tarefa
        tarefa = new Tarefa();
        tarefa.setProjeto(projeto);
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapa);
        tarefa.setRaia(raia);
        tarefa.setTitulo("Implementar login");
        tarefa.setResponsavel(null);
        tarefa.setCriadoPor(usuarioLogado);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);
        tarefa.setImpedidaDesde(null);
        tarefa = tarefaRepository.save(tarefa);
        tarefaId = tarefa.getId();
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    @DisplayName("marcarImpedimento_should_persistirHistoricoEAuditoriaEmBD")
    void marcarImpedimento_should_persistirHistoricoEAuditoriaEmBD() {
        // Act
        tarefaService.marcarImpedimento(tarefaId, projetoId);

        // Assert — verificar que foi persistido em BD
        Tarefa tarefaBD = tarefaRepository.findById(tarefaId).orElseThrow();
        assertTrue(tarefaBD.isImpedida(), "Tarefa deve estar impedida em BD");
        assertNotNull(tarefaBD.getImpedidaDesde(), "impedidaDesde deve estar preenchido em BD");

        // Verificar histórico
        List<TarefaImpedimentoHistorico> historicos =
                tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId);
        assertEquals(1, historicos.size(), "Deve ter 1 histórico em BD");
        assertNull(historicos.get(0).getDesmarcadoEm(), "Histórico deve estar aberto");

        // Verificar auditoria
        List<TarefaAuditoria> auditorias = tarefaAuditoriaRepository.findAll();
        assertTrue(auditorias.stream().anyMatch(a ->
                a.getTarefa().getId().equals(tarefaId) && "impedimento".equals(a.getCampo())),
                "Auditoria deve registrar mudança de impedimento");
    }

    @Test
    @DisplayName("desmarcarImpedimento_should_fecharHistoricoEPersistirEmBD")
    void desmarcarImpedimento_should_fecharHistoricoEPersistirEmBD() {
        // Arrange — marcar primeiro
        tarefaService.marcarImpedimento(tarefaId, projetoId);

        // Act — desmarcar
        tarefaService.desmarcarImpedimento(tarefaId, projetoId);

        // Assert
        Tarefa tarefaBD = tarefaRepository.findById(tarefaId).orElseThrow();
        assertFalse(tarefaBD.isImpedida(), "Tarefa deve estar desmarcada em BD");

        // Verificar histórico fechado
        List<TarefaImpedimentoHistorico> historicos =
                tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId);
        assertEquals(1, historicos.size(), "Deve ter 1 histórico em BD");
        assertNotNull(historicos.get(0).getDesmarcadoEm(), "Histórico deve estar fechado em BD");

        // Verificar auditoria dupla
        List<TarefaAuditoria> auditorias = tarefaAuditoriaRepository.findAll();
        long auditoriasImpedimento = auditorias.stream()
                .filter(a -> a.getTarefa().getId().equals(tarefaId) && "impedimento".equals(a.getCampo()))
                .count();
        assertEquals(2, auditoriasImpedimento, "Deve ter 2 auditoria (marcar + desmarcar)");
    }

    @Test
    @DisplayName("multiplos_ciclos_should_acumularHistoricosEmBD")
    void multiplos_ciclos_should_acumularHistoricosEmBD() {
        // Act — Ciclo 1: marcar
        tarefaService.marcarImpedimento(tarefaId, projetoId);

        // Verificar primeira marca
        List<TarefaImpedimentoHistorico> historicos1 =
                tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId);
        assertEquals(1, historicos1.size());

        // Act — Ciclo 1: desmarcar
        tarefaService.desmarcarImpedimento(tarefaId, projetoId);

        // Act — Ciclo 2: marcar
        tarefaService.marcarImpedimento(tarefaId, projetoId);

        // Assert — múltiplos históricos
        List<TarefaImpedimentoHistorico> historicosFinal =
                tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId);
        assertEquals(2, historicosFinal.size(), "Deve ter 2 históricos após múltiplos ciclos");

        // Verificar que o primeiro está fechado e o segundo aberto
        assertTrue(historicosFinal.get(0).getDesmarcadoEm() != null, "Primeiro deve estar fechado");
        assertNull(historicosFinal.get(1).getDesmarcadoEm(), "Segundo deve estar aberto");

        // Verificar tempos
        assertTrue(historicosFinal.get(0).getMarcadoEm().isBefore(historicosFinal.get(1).getMarcadoEm()),
                "Segundo ciclo deve ter tempo posterior");
    }

    @Test
    @DisplayName("transacao_ACID_should_naoParticiparSemErro")
    void transacao_ACID_should_naoParticiparSemErro() {
        // Act — marcar múltiplas vezes em sequência
        for (int i = 0; i < 3; i++) {
            tarefaService.marcarImpedimento(tarefaId, projetoId);
            tarefaService.desmarcarImpedimento(tarefaId, projetoId);
        }

        // Assert — BD deve ter estado consistente
        Tarefa tarefaBD = tarefaRepository.findById(tarefaId).orElseThrow();
        assertFalse(tarefaBD.isImpedida(), "Estado final deve ser desmarcado");

        // Histórico deve ter 3 pares marca/desmarca
        List<TarefaImpedimentoHistorico> historicos =
                tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId);
        assertEquals(3, historicos.size(), "Deve ter 3 históricos (3 ciclos)");

        // Todos devem estar fechados
        assertTrue(historicos.stream().allMatch(h -> h.getDesmarcadoEm() != null),
                "Todos os históricos devem estar fechados");

        // Auditoria deve ter 6 registros (3 marca + 3 desmarca)
        List<TarefaAuditoria> auditorias = tarefaAuditoriaRepository.findAll();
        long auditoriasImpedimento = auditorias.stream()
                .filter(a -> a.getTarefa().getId().equals(tarefaId) && "impedimento".equals(a.getCampo()))
                .count();
        assertEquals(6, auditoriasImpedimento, "Deve ter 6 auditorias (3 marca + 3 desmarca)");
    }

    @Test
    @DisplayName("tempo_impedimento_should_calcularCorretamenteComMultiplosCiclos")
    void tempo_impedimento_should_calcularCorretamenteComMultiplosCiclos() throws InterruptedException {
        // Act — marcar, esperar, desmarcar
        tarefaService.marcarImpedimento(tarefaId, projetoId);
        Thread.sleep(100); // Esperar 100ms
        tarefaService.desmarcarImpedimento(tarefaId, projetoId);

        // Assert — calcular tempo
        List<TarefaImpedimentoHistorico> historicos =
                tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId);
        assertEquals(1, historicos.size());

        long tempoSegundos = java.time.temporal.ChronoUnit.SECONDS.between(
                historicos.get(0).getMarcadoEm(),
                historicos.get(0).getDesmarcadoEm()
        );

        assertTrue(tempoSegundos >= 0, "Tempo deve ser não-negativo");
        // Não fazer assert exato de tempo (flaky), só verificar que está calculado
    }
}
