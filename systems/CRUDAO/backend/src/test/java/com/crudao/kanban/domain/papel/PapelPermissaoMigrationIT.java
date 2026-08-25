package com.crudao.kanban.domain.papel;

import static org.assertj.core.api.Assertions.assertThat;

import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Valida que as migrations V1/V2 aplicam sem erro (Flyway + Testcontainers) e que o seed de
 * papel/permissão reflete RN-006 (admin global protegido) — TASK-01.2. Catálogo de permissões
 * atualizado para incluir {@code tarefa:auditoria} (migration V10, TASK-04.4).
 */
@Testcontainers
@SpringBootTest
class PapelPermissaoMigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private PapelRepository papelRepository;
    @Autowired private PermissaoRepository permissaoRepository;
    @Autowired private PapelPermissaoRepository papelPermissaoRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    void contextLoadsAndMigrationsApply() {
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void catalogoDePermissoesEstaCompleto() {
        List<String> chaves = permissaoRepository.findAll().stream().map(Permissao::getChave).toList();

        assertThat(chaves)
                .containsExactlyInAnyOrder(
                        "tarefa:gerenciar",
                        "tarefa:finalizar",
                        "tarefa:impedimento",
                        "tarefa:excluir",
                        "tarefa:auditoria",
                        "projeto:administrar",
                        "workflow:administrar",
                        "papel:administrar",
                        "usuario:associar");
    }

    @Test
    void papelAdminEhGlobalEProtegido() {
        Optional<Papel> admin = papelRepository.findByChaveAndProjetoIsNull("admin");

        assertThat(admin).isPresent();
        assertThat(admin.get().isProtegido()).isTrue();
        assertThat(admin.get().getProjeto()).isNull();
    }

    @Test
    void papelAdminTemTodasAsPermissoesHabilitadas() {
        Papel admin = papelRepository.findByChaveAndProjetoIsNull("admin").orElseThrow();

        List<PapelPermissao> permissoesDoAdmin = papelPermissaoRepository.findByPapelId(admin.getId());

        assertThat(permissoesDoAdmin).hasSize(9);
        assertThat(permissoesDoAdmin).allMatch(PapelPermissao::isHabilitada);
    }
}
