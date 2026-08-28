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
    void migrationsApplyAndCreateFoundationTables() {
        assertThat(usuarioRepository.count()).isZero();
        assertThat(papelRepository.count()).isEqualTo(1);
    }

    @Test
    void permissionCatalogIsComplete() {
        List<String> keys = permissaoRepository.findAll().stream().map(Permissao::getChave).toList();

        assertThat(keys)
                .containsExactlyInAnyOrder(
                        "tarefa:gerenciar",
                        "tarefa:finalizar",
                        "tarefa:impedimento",
                        "tarefa:excluir",
                        "projeto:administrar",
                        "workflow:administrar",
                        "papel:administrar",
                        "usuario:associar");
    }

    @Test
    void adminRoleIsGlobalProtectedAndFullyEnabled() {
        Optional<Papel> admin = papelRepository.findByChaveAndProjetoIsNull("admin");

        assertThat(admin).isPresent();
        assertThat(admin.get().isProtegido()).isTrue();
        assertThat(admin.get().getProjeto()).isNull();

        List<PapelPermissao> toggles = papelPermissaoRepository.findByPapelId(admin.get().getId());
        assertThat(toggles).hasSize(8);
        assertThat(toggles).allMatch(PapelPermissao::isHabilitada);
    }
}
