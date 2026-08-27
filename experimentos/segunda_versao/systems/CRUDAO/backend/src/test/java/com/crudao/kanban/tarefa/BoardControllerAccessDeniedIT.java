package com.crudao.kanban.tarefa;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crudao.kanban.auth.UsuarioProvisioningService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Confirma, no nível de endpoint (não só de {@code BoardService}), que {@link
 * AccessDeniedException} lançada por {@code board(projetoId)} vira {@code 403} — mesma checagem
 * de nível de endpoint já feita para o guard em {@code PermissaoGuardEndpointIT} (achado de code
 * review, agent QA, TASK-04.5).
 */
@WebMvcTest(controllers = BoardController.class)
@Import(BoardControllerAccessDeniedIT.SegurancaDeTeste.class)
class BoardControllerAccessDeniedIT {

    @Autowired private MockMvc mockMvc;
    @MockBean private BoardService boardService;

    // AtivoUsuarioFilter (Filter @Component incluído automaticamente na fatia do @WebMvcTest)
    // depende deste bean — sem o @MockBean o contexto falha ao subir (mesmo achado documentado em
    // PermissaoGuardEndpointIT, TASK-02.3).
    @MockBean private UsuarioProvisioningService usuarioProvisioningService;

    @Test
    void naoMembroDoProjeto_endpointRetorna403() throws Exception {
        UUID projetoId = UUID.randomUUID();
        when(boardService.board(projetoId)).thenThrow(new AccessDeniedException("Acesso negado"));

        mockMvc.perform(get("/api/projetos/{projetoId}/board", projetoId)).andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class SegurancaDeTeste {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            http.exceptionHandling(
                    handling ->
                            handling.accessDeniedHandler(
                                    (request, response, e) -> response.setStatus(403)));
            return http.build();
        }
    }
}
