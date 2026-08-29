package com.crudao.kanban.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base dos testes de integração (@SpringBootTest) executados contra o stack Docker final
 * (PostgreSQL + Keycloak do docker-compose.yml), não mais via Testcontainers.
 *
 * <p>O contexto sobe com o profile {@code it} ({@code application-it.yml}): datasource e Keycloak
 * apontam para os containers do compose; o schema é validado contra as migrations Flyway reais
 * ({@code ddl-auto=validate}).
 *
 * <p>Execução: {@code systems/CRUDAO/run-integration-tests.ps1} (sobe o compose, cria o banco
 * {@code kanban_it} e roda {@code mvn -P integration-tests test} num container Maven).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class IntegrationTestBase {}
