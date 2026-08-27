package com.crudao.kanban.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint de escrita como exigindo a permissão indicada (RNF-003). Validado por {@link
 * PermissaoAspect} contra o papel do usuário autenticado ({@link UsuarioContexto}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExigePermissao {
  String value();
}
