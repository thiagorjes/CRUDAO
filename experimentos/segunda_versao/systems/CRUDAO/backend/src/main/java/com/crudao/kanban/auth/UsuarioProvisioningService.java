package com.crudao.kanban.auth;

import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Provisioning just-in-time de {@link Usuario} a partir do {@code sub} e claims do token OIDC
 * (RF-014) — cria o usuário local no primeiro login e mantém nome/email sincronizados nos
 * seguintes.
 *
 * <p>Bootstrap do admin global (ADR-007): o primeiro login do e-mail configurado em {@code
 * kanban.bootstrap.admin-email} marca {@link Usuario#isAdminGlobal()}. Não é reaplicado em logins
 * seguintes — uma vez criado, o flag só é alterável fora deste fluxo (evita reset acidental caso a
 * property mude).
 */
@Service
public class UsuarioProvisioningService {

    private final UsuarioRepository usuarioRepository;
    private final String bootstrapAdminEmail;

    public UsuarioProvisioningService(
            UsuarioRepository usuarioRepository,
            @Value("${kanban.bootstrap.admin-email:}") String bootstrapAdminEmail) {
        this.usuarioRepository = usuarioRepository;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    public Usuario provisionar(String keycloakSub, String nome, String email) {
        return usuarioRepository
                .findByKeycloakSub(keycloakSub)
                .map(existente -> atualizarSeNecessario(existente, nome, email))
                .orElseGet(() -> criar(keycloakSub, nome, email));
    }

    /**
     * Duas requisições concorrentes no primeiro login do mesmo {@code sub} podem colidir na
     * constraint {@code UNIQUE(keycloak_sub)} — {@code saveAndFlush} força o INSERT a estourar
     * aqui (em vez de só no commit da transação da requisição), e o perdedor da corrida
     * simplesmente reaproveita o registro criado pelo vencedor.
     */
    private Usuario criar(String keycloakSub, String nome, String email) {
        Usuario usuario = new Usuario();
        usuario.setKeycloakSub(keycloakSub);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setAtivo(true);
        usuario.setAdminGlobal(
                !bootstrapAdminEmail.isBlank() && bootstrapAdminEmail.equalsIgnoreCase(email));
        usuario.setCriadoEm(OffsetDateTime.now());
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            return usuarioRepository
                    .findByKeycloakSub(keycloakSub)
                    .orElseThrow(() -> e);
        }
    }

    private Usuario atualizarSeNecessario(Usuario usuario, String nome, String email) {
        boolean mudou =
                !Objects.equals(usuario.getNome(), nome) || !Objects.equals(usuario.getEmail(), email);
        if (!mudou) {
            return usuario;
        }
        usuario.setNome(nome);
        usuario.setEmail(email);
        return usuarioRepository.save(usuario);
    }
}
