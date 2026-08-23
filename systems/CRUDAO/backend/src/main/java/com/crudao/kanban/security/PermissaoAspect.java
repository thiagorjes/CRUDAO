package com.crudao.kanban.security;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.domain.rbac.Usuario;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Valida em todo endpoint anotado com {@link ExigePermissao} a permissão do usuário autenticado
 * (RNF-003).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissaoAspect {

  private final UsuarioContexto usuarioContexto;

  @Before("@annotation(exigePermissao)")
  public void validarPermissao(ExigePermissao exigePermissao) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    if (!usuario.getPapel().temPermissao(exigePermissao.value())) {
      throw new AcessoNegadoException(
          "Usuário sem a permissão '%s' necessária para esta operação."
              .formatted(exigePermissao.value()));
    }
  }
}
