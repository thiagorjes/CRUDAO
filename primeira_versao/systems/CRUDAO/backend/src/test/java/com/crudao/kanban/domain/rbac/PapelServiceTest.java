package com.crudao.kanban.domain.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PapelServiceTest {

  @Mock private PapelRepository papelRepository;
  @Mock private PermissaoRepository permissaoRepository;

  private PapelService papelService;
  private Papel papelAdmin;
  private Papel papelDelegado;
  private Permissao permissaoTarefa;

  @BeforeEach
  void setUp() {
    papelService = new PapelService(papelRepository, permissaoRepository, new PapelMapper());

    papelAdmin = new Papel();
    papelAdmin.setId(UUID.randomUUID());
    papelAdmin.setNome("admin");
    papelAdmin.setProtegido(true);

    papelDelegado = new Papel();
    papelDelegado.setId(UUID.randomUUID());
    papelDelegado.setNome("Gestor de Projeto");
    papelDelegado.setProtegido(false);

    permissaoTarefa = new Permissao(UUID.randomUUID(), "tarefa:gerenciar");
  }

  @Test
  void deveCriarPapelResolvendoPermissoesPelasChaves() {
    when(permissaoRepository.findByChave("tarefa:gerenciar"))
        .thenReturn(Optional.of(permissaoTarefa));
    when(papelRepository.save(any(Papel.class))).thenAnswer(inv -> inv.getArgument(0));

    PapelDTO dto = papelService.criar(new PapelRequest("Gestor", Set.of("tarefa:gerenciar")));

    assertThat(dto.nome()).isEqualTo("Gestor");
    assertThat(dto.protegido()).isFalse();
    assertThat(dto.permissoes()).containsExactly("tarefa:gerenciar");
  }

  @Test
  void deveFalharAoCriarPapelComChaveDePermissaoInexistente() {
    when(permissaoRepository.findByChave("inexistente")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> papelService.criar(new PapelRequest("Gestor", Set.of("inexistente"))))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void naoDevePermitirEditarPapelAdminProtegido_RN006() {
    when(papelRepository.findById(papelAdmin.getId())).thenReturn(Optional.of(papelAdmin));

    assertThatThrownBy(
            () ->
                papelService.editar(
                    papelAdmin.getId(), new PapelRequest("admin-renomeado", Set.of())))
        .isInstanceOf(RegraDeNegocioException.class);
  }

  @Test
  void naoDevePermitirExcluirPapelAdminProtegido_RN006() {
    when(papelRepository.findById(papelAdmin.getId())).thenReturn(Optional.of(papelAdmin));

    assertThatThrownBy(() -> papelService.excluir(papelAdmin.getId()))
        .isInstanceOf(RegraDeNegocioException.class);

    verify(papelRepository, org.mockito.Mockito.never()).delete(any());
  }

  @Test
  void devePermitirEditarPapelDelegadoNaoProtegido() {
    when(papelRepository.findById(papelDelegado.getId())).thenReturn(Optional.of(papelDelegado));
    when(papelRepository.save(any(Papel.class))).thenAnswer(inv -> inv.getArgument(0));

    PapelDTO dto =
        papelService.editar(papelDelegado.getId(), new PapelRequest("Novo Nome", Set.of()));

    assertThat(dto.nome()).isEqualTo("Novo Nome");
  }

  @Test
  void devePermitirExcluirPapelDelegadoNaoProtegido() {
    when(papelRepository.findById(papelDelegado.getId())).thenReturn(Optional.of(papelDelegado));

    papelService.excluir(papelDelegado.getId());

    verify(papelRepository).delete(papelDelegado);
  }
}
