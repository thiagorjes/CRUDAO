package com.crudao.kanban.rbac;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.auth.UsuarioProvisioningService;
import com.crudao.kanban.domain.usuario.Usuario;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifica, no nível de endpoint (não só de regra), que {@code @PreAuthorize} usando {@link
 * PermissaoGuard} bloqueia com {@code 403} quando o usuário não possui a permissão exigida
 * (RF-013/RNF-003).
 */
// `controllers = EndpointDeTeste.class` sozinho NÃO registra o bean — o component-scan do
// @WebMvcTest não alcança classes aninhadas dentro da própria classe de teste (só filtra o que o
// scan de com.crudao.kanban já encontraria); sem o @Import explícito de EndpointDeTeste aqui, o
// controller nunca vira bean e toda requisição cai no ResourceHttpRequestHandler (404 silencioso,
// achado em investigação pós-TASK-02.3 — os 3 testes deste arquivo nunca haviam rodado de fato).
@WebMvcTest(controllers = PermissaoGuardEndpointIT.EndpointDeTeste.class)
@Import({PermissaoGuardEndpointIT.EndpointDeTeste.class, PermissaoGuardEndpointIT.SegurancaDeTeste.class})
class PermissaoGuardEndpointIT {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID PROJETO_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @MockBean private PermissaoService permissaoService;
    @MockBean private com.crudao.kanban.domain.usuario.ProjetoRepository projetoRepository;

    // Não usado diretamente pelos testes — mas AtivoUsuarioFilter (um Filter @Component, incluído
    // automaticamente na fatia de contexto do @WebMvcTest) depende dele; sem este @MockBean o
    // contexto falha ao subir (NoSuchBeanDefinitionException).
    @MockBean private UsuarioProvisioningService usuarioProvisioningService;

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    void semAPermissaoExigida_endpointRetorna403() throws Exception {
        autenticar();
        when(permissaoService.possui(USUARIO_ID, PROJETO_ID, "papel:administrar")).thenReturn(false);

        mockMvc.perform(
                        get("/test/rbac/{projetoId}", PROJETO_ID)
                                .with(user(USUARIO_ID.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void comAPermissaoExigida_endpointRetorna200() throws Exception {
        autenticar();
        when(permissaoService.possui(USUARIO_ID, PROJETO_ID, "papel:administrar")).thenReturn(true);

        mockMvc.perform(
                        get("/test/rbac/{projetoId}", PROJETO_ID)
                                .with(user(USUARIO_ID.toString())))
                .andExpect(status().isOk());
    }

    /**
     * Em produção o {@code AtivoUsuarioFilter} já bloqueia usuário {@code ativo=false} com {@code
     * 401} antes de popular o {@code UsuarioAutenticadoHolder} (nunca chega ao guard) — aqui
     * simula-se justamente essa ausência de contexto para o endpoint, confirmando que o guard nega
     * por padrão quando não há usuário resolvido, sem depender de o service mentir um "possui".
     */
    @Test
    void semUsuarioAutenticadoNoContexto_endpointRetorna403MesmoQueServicoAutorize() throws Exception {
        UsuarioAutenticadoHolder.clear();
        when(permissaoService.possui(USUARIO_ID, PROJETO_ID, "papel:administrar")).thenReturn(true);

        mockMvc.perform(
                        get("/test/rbac/{projetoId}", PROJETO_ID)
                                .with(user(USUARIO_ID.toString())))
                .andExpect(status().isForbidden());
    }

    private void autenticar() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setAtivo(true);
        UsuarioAutenticadoHolder.set(usuario);
    }

    @RestController
    static class EndpointDeTeste {

        @GetMapping("/test/rbac/{projetoId}")
        @PreAuthorize("@permissaoGuard.permitido(#projetoId, 'papel:administrar')")
        public ResponseEntity<Void> acao(@PathVariable("projetoId") UUID projetoId) {
            return ResponseEntity.ok().build();
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class SegurancaDeTeste {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }

        @Bean("permissaoGuard")
        PermissaoGuard permissaoGuard(
                PermissaoService permissaoService,
                com.crudao.kanban.domain.usuario.ProjetoRepository projetoRepository) {
            return new PermissaoGuard(permissaoService, projetoRepository);
        }
    }
}
