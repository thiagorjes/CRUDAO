package com.crudao.kanban.raia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/** CRUD de Raia — TASK-03.3 (RF-011, RN-CB-005, RN-005). */
@ExtendWith(MockitoExtension.class)
class RaiaServiceTest {

    @Mock private RaiaRepository raiaRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private PermissaoGuard permissaoGuard;

    private RaiaService service;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID raiaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RaiaService(raiaRepository, projetoRepository, permissaoGuard);
    }

    @Test
    void listar_semVinculo_lanca403() {
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        assertThatThrownBy(() -> service.listar(projetoId)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listar_projetoComRaiasProprias_retornaApenasAsDoProjeto() {
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(raiaRepository.findByProjetoId(projetoId)).thenReturn(List.of(raia(projeto(), "Backend", 0)));

        List<RaiaResponse> resultado = service.listar(projetoId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).global()).isFalse();
        verify(raiaRepository, never()).findByProjetoIdIsNull();
    }

    @Test
    void listar_projetoSemRaiaPropria_retornaDefaultGlobal() {
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(raiaRepository.findByProjetoId(projetoId)).thenReturn(List.of());
        when(raiaRepository.findByProjetoIdIsNull()).thenReturn(List.of(raia(null, "Padrão", 0)));

        List<RaiaResponse> resultado = service.listar(projetoId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).global()).isTrue();
    }

    @Test
    void criar_semPermissao_lanca403SemSalvar() {
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "workflow:administrar");

        assertThatThrownBy(() -> service.criar(projetoId, new CriarRaiaRequest("Frontend", 0)))
                .isInstanceOf(AccessDeniedException.class);
        verify(raiaRepository, never()).save(any());
    }

    @Test
    void criar_ordemNegativa_lanca422() {
        assertThatThrownBy(() -> service.criar(projetoId, new CriarRaiaRequest("Frontend", -1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(raiaRepository, never()).save(any());
    }

    @Test
    void criar_nomeVazio_lanca422() {
        assertThatThrownBy(() -> service.criar(projetoId, new CriarRaiaRequest(" ", 0)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(raiaRepository, never()).save(any());
    }

    @Test
    void criar_autorizado_persisteVinculadoAoProjeto() {
        when(projetoRepository.getReferenceById(projetoId)).thenReturn(projeto());
        when(raiaRepository.save(any(Raia.class))).thenAnswer(inv -> inv.getArgument(0));

        RaiaResponse resposta = service.criar(projetoId, new CriarRaiaRequest("Frontend", 0));

        assertThat(resposta.nome()).isEqualTo("Frontend");
        assertThat(resposta.global()).isFalse();
    }

    @Test
    void editar_raiaDefaultGlobal_lanca422() {
        Raia raiaGlobal = raia(null, "Padrão", 0);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaGlobal));

        assertThatThrownBy(() -> service.editar(raiaId, new EditarRaiaRequest("Outro", 1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(raiaRepository, never()).save(any());
    }

    @Test
    void editar_semPermissao_lanca403SemSalvar() {
        Raia raiaDoProjeto = raia(projeto(), "Backend", 0);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaDoProjeto));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "workflow:administrar");

        assertThatThrownBy(() -> service.editar(raiaId, new EditarRaiaRequest("Outro", 1)))
                .isInstanceOf(AccessDeniedException.class);
        verify(raiaRepository, never()).save(any());
    }

    @Test
    void editar_projetoFinalizado_lanca403() {
        Raia raiaDoProjeto = raia(projeto(), "Backend", 0);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaDoProjeto));
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.editar(raiaId, new EditarRaiaRequest("Outro", 1)))
                .isInstanceOf(AccessDeniedException.class);
        verify(raiaRepository, never()).save(any());
    }

    @Test
    void excluir_raiaDefaultGlobal_lanca422() {
        Raia raiaGlobal = raia(null, "Padrão", 0);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaGlobal));

        assertThatThrownBy(() -> service.excluir(raiaId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(raiaRepository, never()).delete(any());
    }

    @Test
    void excluir_semPermissao_lanca403SemDeletar() {
        Raia raiaDoProjeto = raia(projeto(), "Backend", 0);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaDoProjeto));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "workflow:administrar");

        assertThatThrownBy(() -> service.excluir(raiaId)).isInstanceOf(AccessDeniedException.class);
        verify(raiaRepository, never()).delete(any());
    }

    @Test
    void excluir_autorizado_removeRaiaDoProjeto() {
        // Stub RN-005 sempre retorna "sem tarefas ativas" até TASK-04.1 — apenas garante que a raia
        // do projeto é excluída com sucesso quando autorizada. Checagem real de 409 fica para TASK-04.1.
        Raia raiaDoProjeto = raia(projeto(), "Backend", 0);
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.of(raiaDoProjeto));

        service.excluir(raiaId);

        verify(raiaRepository).delete(raiaDoProjeto);
    }

    @Test
    void excluir_raiaInexistente_lanca404() {
        when(raiaRepository.findById(raiaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(raiaId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    private Projeto projeto() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        return projeto;
    }

    private Raia raia(Projeto projeto, String nome, int ordem) {
        Raia raia = new Raia();
        raia.setId(raiaId);
        raia.setProjeto(projeto);
        raia.setNome(nome);
        raia.setOrdem(ordem);
        return raia;
    }
}
