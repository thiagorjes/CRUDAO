package com.crudao.kanban.projeto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.*;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {
    @Mock ProjetoRepository projetoRepository;
    @Mock PapelRepository papelRepository;
    @Mock PapelPermissaoRepository papelPermissaoRepository;
    @Mock PermissaoRepository permissaoRepository;
    @Mock PermissaoGuard permissaoGuard;
    @Mock UsuarioProjetoPapelRepository vinculoRepository;
    @InjectMocks ProjetoService service;

    @AfterEach
    void clearContext() { UsuarioAutenticadoHolder.clear(); }

    @Test
    void criarInicializaProjetoEQuatroPapeisDefault() {
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setAtivo(true);
        admin.setAdminGlobal(true);
        UsuarioAutenticadoHolder.set(admin);
        Projeto projeto = new Projeto();
        projeto.setId(UUID.randomUUID());
        when(projetoRepository.save(any())).thenReturn(projeto);
        when(papelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Permissao permissao = new Permissao();
        permissao.setChave("tarefa:gerenciar");
        when(permissaoRepository.findAll()).thenReturn(java.util.List.of(permissao));

        Projeto resultado = service.criar("  Projeto  ", null);

        assertEquals(projeto, resultado);
        assertEquals("Projeto", projeto.getNome());
        verify(papelRepository, times(4)).save(any());
        verify(papelPermissaoRepository, atLeast(1)).save(any());
    }

    @Test
    void atualizarProjetoFinalizadoPermaneceBloqueado() {
        UUID id = UUID.randomUUID();
        doThrow(new org.springframework.security.access.AccessDeniedException("Acesso negado"))
                .when(permissaoGuard).exigirProjetoAtivo(id);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.atualizar(id, "Novo", null));
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void nomeVazioRetorna422() {
        Usuario admin = new Usuario();
        admin.setAtivo(true);
        admin.setAdminGlobal(true);
        UsuarioAutenticadoHolder.set(admin);
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.criar(" ", null));
        assertEquals(422, erro.getStatusCode().value());
    }
}
