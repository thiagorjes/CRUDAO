package com.crudao.kanban.domain.papel;

import static org.assertj.core.api.Assertions.assertThat;

import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Valida que a migration V8 aplica sem erro (Flyway + Testcontainers) e que {@link
 * PapelPermissaoAuditoria} persiste corretamente (RF-016, RN-017) — TASK-02.3.
 */
@Testcontainers
@SpringBootTest
class PapelPermissaoAuditoriaMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private PapelPermissaoAuditoriaRepository auditoriaRepository;
    @Autowired private PapelRepository papelRepository;
    @Autowired private PermissaoRepository permissaoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    void registraEPersisteAlteracaoDeToggle() {
        Usuario autor = new Usuario();
        autor.setKeycloakSub(UUID.randomUUID().toString());
        autor.setNome("Autor");
        autor.setEmail(UUID.randomUUID() + "@teste.com");
        autor.setCriadoEm(OffsetDateTime.now());
        autor = usuarioRepository.save(autor);

        Papel admin = papelRepository.findByChaveAndProjetoIsNull("admin").orElseThrow();
        Permissao permissao = permissaoRepository.findByChave("papel:administrar").orElseThrow();

        PapelPermissaoAuditoria auditoria = new PapelPermissaoAuditoria();
        auditoria.setPapel(admin);
        auditoria.setPermissao(permissao);
        auditoria.setAutor(autor);
        auditoria.setValorAnterior(false);
        auditoria.setValorNovo(true);
        auditoria.setDataHora(OffsetDateTime.now());

        PapelPermissaoAuditoria salva = auditoriaRepository.save(auditoria);

        assertThat(auditoriaRepository.findById(salva.getId())).isPresent();
    }
}
