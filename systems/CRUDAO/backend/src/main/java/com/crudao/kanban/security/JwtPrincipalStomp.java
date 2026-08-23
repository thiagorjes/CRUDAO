package com.crudao.kanban.security;

import java.security.Principal;

/** {@link Principal} mínimo atribuído à sessão STOMP após validar o JWT do CONNECT. */
public record JwtPrincipalStomp(String name) implements Principal {

  @Override
  public String getName() {
    return name;
  }
}
