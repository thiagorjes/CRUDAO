package com.crudao.kanban.tarefa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import com.crudao.kanban.tarefa.dto.CriarTarefaResponse;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private EtapaRepository etapaRepository;

    @Mock
    private RaiaRepository raiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PermissaoGuard permissaoGuard;

    @InjectMocks
    private TarefaService tarefaService;

    private UUID projetoId;
    private Projeto projeto;
    private Usuario usuarioLogado;
    private Workflow workflow;
    private Etapa etapa1;
    private Etapa etapa2;
    private Raia raiaGlobal;

    @BeforeEach
    void setUp() {
        projetoId = UUID.randomUUID();
        projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setNome("Projeto Tarefas");
        projeto.setStatus(Projeto.Status.ATIVO);

        usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        usuarioLogado.setEmail("dev@test.com");
        usuarioLogado.setAtivo(true);
        UsuarioAutenticadoHolder.set(usuarioLogado);

        workflow = new Workflow(UUID.randomUUID(), projeto, "Workflow Standard");
        etapa1 = new Etapa(UUID.randomUUID(), workflow, "Backlog", 1, false);
        etapa2 = new Etapa(UUID.randomUUID(), workflow, "Done", 2, true);
        raiaGlobal = new Raia(UUID.randomUUID(), null, "Padrão", 1);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    @DisplayName("test_criarTarefa_when_semResponsavelERaiaInformados_should_usarDefaultsEGravarHistorico")
    void test_criarTarefa_when_semResponsavelERaiaInformados_should_usarDefaultsEGravarHistorico() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId())).thenReturn(List.of(etapa1, etapa2));
        when(raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId)).thenReturn(Collections.emptyList());
        when(raiaRepository.findByProjetoIdIsNullOrderByOrdemAsc()).thenReturn(List.of(raiaGlobal));

        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocation -> {
            Tarefa t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CriarTarefaRequest request = new CriarTarefaRequest("Card 1", "Descrição do card 1", null, null);
        CriarTarefaResponse response = tarefaService.criarTarefa(projetoId, request);

        assertNotNull(response.getId());
        assertEquals("Card 1", response.getTitulo());
        assertEquals(etapa1.getId(), response.getEtapaAtualId());
        assertEquals(raiaGlobal.getId(), response.getRaiaId());
        assertNull(response.getResponsavelId());

        verify(permissaoGuard).exigirProjetoAtivo(projetoId);
        verify(permissaoGuard).exigir(projetoId, "tarefa:gerenciar");
        verify(tarefaEtapaHistoricoRepository).save(any(TarefaEtapaHistorico.class));
    }

    @Test
    @DisplayName("test_criarTarefa_when_comResponsavelERaiaCustomizada_should_vincularCorretamente")
    void test_criarTarefa_when_comResponsavelERaiaCustomizada_should_vincularCorretamente() {
        UUID responsavelId = UUID.randomUUID();
        Usuario responsavel = new Usuario();
        responsavel.setId(responsavelId);

        UUID raiaCustomId = UUID.randomUUID();
        Raia raiaCustom = new Raia(raiaCustomId, projeto, "Raia Dev", 1);

        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId())).thenReturn(List.of(etapa1, etapa2));
        when(raiaRepository.findById(raiaCustomId)).thenReturn(Optional.of(raiaCustom));
        when(usuarioRepository.findById(responsavelId)).thenReturn(Optional.of(responsavel));

        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(invocation -> {
            Tarefa t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CriarTarefaRequest request = new CriarTarefaRequest("Card Dev", "Desc", responsavelId, raiaCustomId);
        CriarTarefaResponse response = tarefaService.criarTarefa(projetoId, request);

        assertNotNull(response.getId());
        assertEquals(responsavelId, response.getResponsavelId());
        assertEquals(raiaCustomId, response.getRaiaId());
    }

    @Test
    @DisplayName("test_criarTarefa_when_usuarioNaoAutenticado_should_retornarErro401")
    void test_criarTarefa_when_usuarioNaoAutenticado_should_retornarErro401() {
        UsuarioAutenticadoHolder.clear();
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId())).thenReturn(List.of(etapa1));
        when(raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId)).thenReturn(List.of(raiaGlobal));

        CriarTarefaRequest request = new CriarTarefaRequest("Card 1", null, null, null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.criarTarefa(projetoId, request)
        );

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("test_criarTarefa_when_raiaDeOutroProjeto_should_retornarErro422")
    void test_criarTarefa_when_raiaDeOutroProjeto_should_retornarErro422() {
        Projeto outroProjeto = new Projeto();
        outroProjeto.setId(UUID.randomUUID());

        UUID raiaOutroProjetoId = UUID.randomUUID();
        Raia raiaOutroProjeto = new Raia(raiaOutroProjetoId, outroProjeto, "Raia Outro", 1);

        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId())).thenReturn(List.of(etapa1));
        when(raiaRepository.findById(raiaOutroProjetoId)).thenReturn(Optional.of(raiaOutroProjeto));

        CriarTarefaRequest request = new CriarTarefaRequest("Card Outra Raia", null, null, raiaOutroProjetoId);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.criarTarefa(projetoId, request)
        );

        assertEquals(422, ex.getStatusCode().value());
    }
}

