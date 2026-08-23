package com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.domain.rbac.Papel;
import com.crudao.kanban.domain.rbac.PapelRepository;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.domain.rbac.UsuarioRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UsuarioContextoTest {

  @Mock private UsuarioRepository usuarioRepository;
  @Mock private PapelRepository papelRepository;

  private UsuarioContexto usuarioContexto;

  @BeforeEach
  void setUp() {
    usuarioContexto = new UsuarioContexto(usuarioRepository, papelRepository);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void deveFalharQuandoNaoHouverAutenticacao() {
    assertThatThrownBy(() -> usuarioContexto.usuarioAtual())
        .isInstanceOf(AcessoNegadoException.class);
  }

  @Test
  void deveResolverUsuarioJaProvisionadoPeloSubDoJwt() {
    Jwt jwt = jwtComRoles("110c0a15-3ac6-466e-96bb-09adb4155d6c", "admin");
    autenticarComo(jwt);
    Usuario usuarioExistente = new Usuario();
    when(usuarioRepository.findByKeycloakSub(jwt.getSubject()))
        .thenReturn(Optional.of(usuarioExistente));

    Usuario resultado = usuarioContexto.usuarioAtual();

    assertThat(resultado).isSameAs(usuarioExistente);
  }

  @Test
  void deveAutoProvisionarUsuarioNoPrimeiroAcessoMapeandoPapelPelaRoleDoAccessToken() {
    Jwt jwt = jwtComRoles("sub-novo", "admin");
    autenticarComo(jwt);
    Papel papelAdmin = new Papel();
    papelAdmin.setNome("admin");
    when(usuarioRepository.findByKeycloakSub("sub-novo")).thenReturn(Optional.empty());
    when(papelRepository.findByNomeIgnoreCase("admin")).thenReturn(Optional.of(papelAdmin));
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

    Usuario resultado = usuarioContexto.usuarioAtual();

    assertThat(resultado.getKeycloakSub()).isEqualTo("sub-novo");
    assertThat(resultado.getPapel()).isEqualTo(papelAdmin);
  }

  @Test
  void deveUsarPapelPadraoUserQuandoNenhumaRoleDoTokenCorresponderAUmPapelConfigurado() {
    Jwt jwt = jwtComRoles("sub-desconhecido", "role-nao-mapeada");
    autenticarComo(jwt);
    Papel papelUser = new Papel();
    papelUser.setNome("user");
    when(usuarioRepository.findByKeycloakSub("sub-desconhecido")).thenReturn(Optional.empty());
    when(papelRepository.findByNomeIgnoreCase("role-nao-mapeada")).thenReturn(Optional.empty());
    when(papelRepository.findByNomeIgnoreCase("user")).thenReturn(Optional.of(papelUser));
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

    Usuario resultado = usuarioContexto.usuarioAtual();

    assertThat(resultado.getPapel()).isEqualTo(papelUser);
  }

  private void autenticarComo(Jwt jwt) {
    var autenticacao = new TestingAuthenticationToken(jwt, null);
    autenticacao.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(autenticacao);
  }

  private Jwt jwtComRoles(String sub, String... roles) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(sub)
        .claim("email", sub + "@crudao.local")
        .claim("name", "Usuário Teste")
        .claim("realm_access", Map.of("roles", java.util.List.of(roles)))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .build();
  }
}
