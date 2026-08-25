package com.crudao.kanban.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Provisioning JIT de {@link Usuario} a partir de claims OIDC (RF-014) — TASK-02.1. */
@ExtendWith(MockitoExtension.class)
class UsuarioProvisioningServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    private UsuarioProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioProvisioningService(usuarioRepository, "admin.teste@crudao.local");
    }

    @Test
    void quandoUsuarioNaoExiste_crioNovoAtivoComDadosDoToken() {
        when(usuarioRepository.findByKeycloakSub("sub-123")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = service.provisionar("sub-123", "Fulano", "fulano@example.com");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());

        assertThat(resultado.getKeycloakSub()).isEqualTo("sub-123");
        assertThat(resultado.getNome()).isEqualTo("Fulano");
        assertThat(resultado.getEmail()).isEqualTo("fulano@example.com");
        assertThat(resultado.isAtivo()).isTrue();
        assertThat(resultado.getCriadoEm()).isNotNull();
        assertThat(captor.getValue()).isSameAs(resultado);
    }

    @Test
    void quandoUsuarioJaExiste_reutilizoSemCriarDuplicado() {
        Usuario existente = new Usuario();
        existente.setKeycloakSub("sub-123");
        existente.setNome("Fulano");
        existente.setEmail("fulano@example.com");
        existente.setAtivo(true);
        when(usuarioRepository.findByKeycloakSub("sub-123")).thenReturn(Optional.of(existente));

        Usuario resultado = service.provisionar("sub-123", "Fulano", "fulano@example.com");

        assertThat(resultado).isSameAs(existente);
        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void quandoDoisPrimeirosLoginsColidemNaConstraintUnica_reaproveitoORegistroDoVencedor() {
        Usuario criadoPeloConcorrente = new Usuario();
        criadoPeloConcorrente.setKeycloakSub("sub-123");
        criadoPeloConcorrente.setNome("Fulano");
        criadoPeloConcorrente.setEmail("fulano@example.com");
        criadoPeloConcorrente.setAtivo(true);

        when(usuarioRepository.findByKeycloakSub("sub-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(criadoPeloConcorrente));
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_usuario_keycloak_sub"));

        Usuario resultado = service.provisionar("sub-123", "Fulano", "fulano@example.com");

        assertThat(resultado).isSameAs(criadoPeloConcorrente);
    }

    @Test
    void quandoNomeOuEmailMudaramNoIdp_atualizoOsDadosLocais() {
        Usuario existente = new Usuario();
        existente.setKeycloakSub("sub-123");
        existente.setNome("Nome Antigo");
        existente.setEmail("antigo@example.com");
        existente.setAtivo(true);
        when(usuarioRepository.findByKeycloakSub("sub-123")).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = service.provisionar("sub-123", "Nome Novo", "novo@example.com");

        assertThat(resultado.getNome()).isEqualTo("Nome Novo");
        assertThat(resultado.getEmail()).isEqualTo("novo@example.com");
        verify(usuarioRepository).save(existente);
    }

    @Test
    void quandoEmailDoPrimeiroLoginBateComOBootstrap_marcoAdminGlobal() {
        when(usuarioRepository.findByKeycloakSub("sub-admin")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = service.provisionar("sub-admin", "Admin", "admin.teste@crudao.local");

        assertThat(resultado.isAdminGlobal()).isTrue();
    }

    @Test
    void quandoBootstrapAdminEmailNaoConfigurado_naoMarcaNinguemComoAdminGlobal() {
        UsuarioProvisioningService semBootstrap = new UsuarioProvisioningService(usuarioRepository, "");
        when(usuarioRepository.findByKeycloakSub("sub-x")).thenReturn(Optional.empty());
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = semBootstrap.provisionar("sub-x", "Qualquer", "qualquer@example.com");

        assertThat(resultado.isAdminGlobal()).isFalse();
    }
}
