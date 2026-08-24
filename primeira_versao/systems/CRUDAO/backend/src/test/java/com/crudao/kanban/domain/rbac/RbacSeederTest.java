package com.crudao.kanban.domain.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacSeederTest {

  @Mock private PapelRepository papelRepository;
  @Mock private PermissaoRepository permissaoRepository;

  private RbacSeeder rbacSeeder;
  private final Map<String, Papel> papeisSalvos = new HashMap<>();
  private final Map<String, Permissao> permissoesSalvas = new HashMap<>();

  @BeforeEach
  void setUp() {
    rbacSeeder = new RbacSeeder(papelRepository, permissaoRepository);

    when(papelRepository.findByNomeIgnoreCase(anyString()))
        .thenAnswer(inv -> Optional.ofNullable(papeisSalvos.get(inv.getArgument(0))));
    when(papelRepository.save(any(Papel.class)))
        .thenAnswer(
            inv -> {
              Papel papel = inv.getArgument(0);
              papel.setId(UUID.randomUUID());
              papeisSalvos.put(papel.getNome(), papel);
              return papel;
            });
    when(permissaoRepository.findByChave(anyString()))
        .thenAnswer(inv -> Optional.ofNullable(permissoesSalvas.get(inv.getArgument(0))));
    when(permissaoRepository.save(any(Permissao.class)))
        .thenAnswer(
            inv -> {
              Permissao permissao = inv.getArgument(0);
              // id único é necessário: Permissao usa equals/hashCode só pelo id
              // (onlyExplicitlyIncluded)
              // — sem isso, todo Permissao com id nulo colide no Set<Permissao> do papel.
              permissao.setId(UUID.randomUUID());
              permissoesSalvas.put(permissao.getChave(), permissao);
              return permissao;
            });

    rbacSeeder.run();
  }

  @Test
  void gestorNaoDeveReceberImpedimentoMarcarPorPadrao_RN013() {
    assertThat(papeisSalvos.get("gestor").temPermissao("impedimento:marcar")).isFalse();
    assertThat(papeisSalvos.get("gestor").temPermissao("dashboard:visualizar")).isTrue();
  }

  @Test
  void projectAdminNaoDeveReceberPapelGerenciar_G_RBAC_07() {
    assertThat(papeisSalvos.get("project_admin").temPermissao("papel:gerenciar")).isFalse();
    assertThat(papeisSalvos.get("project_admin").temPermissao("tarefa:gerenciar")).isTrue();
  }

  @Test
  void adminDeveReceberTodasAsPermissoesInclusivePapelGerenciar() {
    assertThat(papeisSalvos.get("admin").isProtegido()).isTrue();
    assertThat(papeisSalvos.get("admin").temPermissao("papel:gerenciar")).isTrue();
  }

  @Test
  void userLegadoNaoDeveReceberNenhumaPermissao() {
    assertThat(papeisSalvos.get("user").getPermissoes()).isEmpty();
  }
}
