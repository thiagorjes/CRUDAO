package com.crudao.kanban.raia;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.raia.dto.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.crudao.kanban.domain.tarefa.TarefaRepository;

@ExtendWith(MockitoExtension.class)
class RaiaServiceTest {

    @Mock
    private RaiaRepository raiaRepository;

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private PermissaoGuard permissaoGuard;

    @InjectMocks
    private RaiaService raiaService;

    private UUID projetoId;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        projetoId = UUID.randomUUID();
        projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setNome("Projeto Teste");
        projeto.setStatus(Projeto.Status.ATIVO);
    }

    @Test
    @DisplayName("test_listarRaias_when_projetoSemRaiasCustomizadas_should_retornarRaiaGlobal")
    void test_listarRaias_when_projetoSemRaiasCustomizadas_should_retornarRaiaGlobal() {
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId)).thenReturn(Collections.emptyList());

        Raia raiaGlobal = new Raia(UUID.randomUUID(), null, "Padrão", 1);
        when(raiaRepository.findByProjetoIdIsNullOrderByOrdemAsc()).thenReturn(List.of(raiaGlobal));

        List<RaiaResponse> resps = raiaService.listarRaias(projetoId);

        assertEquals(1, resps.size());
        assertTrue(resps.get(0).getGlobal());
        assertEquals("Padrão", resps.get(0).getNome());
    }

    @Test
    @DisplayName("test_listarRaias_when_projetoComRaiasCustomizadas_should_retornarRaiasCustomizadas")
    void test_listarRaias_when_projetoComRaiasCustomizadas_should_retornarRaiasCustomizadas() {
        when(permissaoGuard.membro(projetoId)).thenReturn(true);

        Raia raiaCustom = new Raia(UUID.randomUUID(), projeto, "Frontend", 1);
        when(raiaRepository.findByProjetoIdOrderByOrdemAsc(projetoId)).thenReturn(List.of(raiaCustom));

        List<RaiaResponse> resps = raiaService.listarRaias(projetoId);

        assertEquals(1, resps.size());
        assertFalse(resps.get(0).getGlobal());
        assertEquals("Frontend", resps.get(0).getNome());
    }

    @Test
    @DisplayName("test_criarRaia_when_dadosValidos_should_salvarERetornar")
    void test_criarRaia_when_dadosValidos_should_salvarERetornar() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        Raia savedRaia = new Raia(UUID.randomUUID(), projeto, "Backend", 1);
        when(raiaRepository.save(any(Raia.class))).thenReturn(savedRaia);

        CriarRaiaRequest req = new CriarRaiaRequest("Backend", 1);
        RaiaResponse resp = raiaService.criarRaia(projetoId, req);

        assertNotNull(resp.getId());
        assertEquals("Backend", resp.getNome());
        assertFalse(resp.getGlobal());
        verify(permissaoGuard).exigirProjetoAtivo(projetoId);
        verify(permissaoGuard).exigir(projetoId, "workflow:administrar");
    }

    @Test
    @DisplayName("test_atualizarRaia_when_raiaGlobal_should_retornarErro403")
    void test_atualizarRaia_when_raiaGlobal_should_retornarErro403() {
        UUID raiaId = UUID.randomUUID();
        Raia raiaGlobal = new Raia(raiaId, null, "Padrão", 1);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaGlobal));

        AtualizarRaiaRequest req = new AtualizarRaiaRequest("Novo Nome", 2);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            raiaService.atualizarRaia(raiaId, req)
        );

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("test_excluirRaia_when_raiaCustomizadaSemTarefasAtivas_should_excluir")
    void test_excluirRaia_when_raiaCustomizadaSemTarefasAtivas_should_excluir() {
        UUID raiaId = UUID.randomUUID();
        Raia raiaCustom = new Raia(raiaId, projeto, "Raia 1", 1);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaCustom));
        when(tarefaRepository.existsByRaiaId(raiaId)).thenReturn(false);

        assertDoesNotThrow(() -> raiaService.excluirRaia(raiaId));

        verify(raiaRepository).delete(raiaCustom);
    }

    @Test
    @DisplayName("test_excluirRaia_when_raiaCustomizadaComTarefasAtivas_should_retornarErro409")
    void test_excluirRaia_when_raiaCustomizadaComTarefasAtivas_should_retornarErro409() {
        UUID raiaId = UUID.randomUUID();
        Raia raiaCustom = new Raia(raiaId, projeto, "Raia 1", 1);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaCustom));
        when(tarefaRepository.existsByRaiaId(raiaId)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            raiaService.excluirRaia(raiaId)
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(raiaRepository, never()).delete(any());
    }
}

