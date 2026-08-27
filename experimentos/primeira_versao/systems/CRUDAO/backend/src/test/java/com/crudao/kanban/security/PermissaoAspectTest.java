package com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.domain.rbac.Papel;
import com.crudao.kanban.domain.rbac.Permissao;
import com.crudao.kanban.domain.rbac.Usuario;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissaoAspectTest {

  @Mock private UsuarioContexto usuarioContexto;

  private PermissaoAspect permissaoAspect;

  @BeforeEach
  void setUp() {
    permissaoAspect = new PermissaoAspect(usuarioContexto);
  }

  @Test
  void devePermitirQuandoPapelDoUsuarioTemAPermissaoExigida() {
    when(usuarioContexto.usuarioAtual()).thenReturn(usuarioComPermissoes("tarefa:gerenciar"));

    assertThatCode(() -> permissaoAspect.validarPermissao(exigePermissao("tarefa:gerenciar")))
        .doesNotThrowAnyException();
  }

  @Test
  void deveNegarQuandoPapelDoUsuarioNaoTemAPermissaoExigida() {
    when(usuarioContexto.usuarioAtual()).thenReturn(usuarioComPermissoes("dashboard:visualizar"));

    assertThatThrownBy(() -> permissaoAspect.validarPermissao(exigePermissao("papel:gerenciar")))
        .isInstanceOf(AcessoNegadoException.class);
  }

  private Usuario usuarioComPermissoes(String... chaves) {
    Papel papel = new Papel();
    papel.setNome("Gestor");
    Set<Permissao> permissoes =
        Set.of(
            java.util.Arrays.stream(chaves)
                .map(c -> new Permissao(null, c))
                .toArray(Permissao[]::new));
    papel.setPermissoes(permissoes);
    Usuario usuario = new Usuario();
    usuario.setPapel(papel);
    return usuario;
  }

  private ExigePermissao exigePermissao(String chave) {
    return new ExigePermissao() {
      @Override
      public String value() {
        return chave;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return ExigePermissao.class;
      }
    };
  }
}
