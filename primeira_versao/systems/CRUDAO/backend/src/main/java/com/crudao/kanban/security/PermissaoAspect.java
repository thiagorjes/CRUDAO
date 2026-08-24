package com.crudao.kanban.security;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.domain.rbac.Usuario;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Valida em todo endpoint anotado com {@link ExigePermissao} a permissão do usuário autenticado
 * (RNF-003). Restrito, a partir do BDR-001/ADR-006, a ações globais — CRUD escopado a projeto
 * passou a usar {@code AutorizacaoProjetoService} explicitamente nos Services de domínio.
 *
 * <p>{@code papel:gerenciar} é caso especial (G-RBAC-07): checado exclusivamente contra {@link
 * Usuario#isAdmin()}, nunca contra papel de projeto — fecha o vetor de escalação em que um {@code
 * project_admin} manipularia permissões de um papel existente.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissaoAspect {

  private static final String PERMISSAO_PAPEL_GERENCIAR = "papel:gerenciar";

  private final UsuarioContexto usuarioContexto;

  @Before("@annotation(exigePermissao)")
  public void validarPermissao(ExigePermissao exigePermissao) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    String permissao = exigePermissao.value();

    boolean autorizado =
        PERMISSAO_PAPEL_GERENCIAR.equals(permissao)
            ? usuario.isAdmin()
            : usuario.getPapel().temPermissao(permissao);

    if (!autorizado) {
      throw new AcessoNegadoException(
          "Usuário sem a permissão '%s' necessária para esta operação.".formatted(permissao));
    }
  }
}
