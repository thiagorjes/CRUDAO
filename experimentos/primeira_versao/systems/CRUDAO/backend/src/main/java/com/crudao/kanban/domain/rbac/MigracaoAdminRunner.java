package com.crudao.kanban.domain.rbac;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migração de dados (BDR-001, data-model.md — Q-006): {@code usuario.papel.nome = 'admin'} vira
 * {@code usuario.admin = true}. Demais usuários não recebem nenhuma linha em {@link
 * UsuarioProjetoPapel} — reatribuição manual pós-migração via RF-015 (decisão deliberada: não
 * herdar escopo implícito de um vínculo que hoje era global, ambiente ainda em dev/homolog).
 *
 * <p>Idempotente — roda a cada subida da aplicação, mas só afeta usuários com {@code admin=false} e
 * papel {@code admin}; nunca desfaz {@code admin=true} já setado manualmente. Loga cada usuário
 * migrado.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class MigracaoAdminRunner implements CommandLineRunner {

  private final UsuarioRepository usuarioRepository;

  @Override
  @Transactional
  public void run(String... args) {
    List<Usuario> candidatos =
        usuarioRepository.findAll().stream()
            .filter(u -> !u.isAdmin())
            .filter(u -> u.getPapel() != null && "admin".equalsIgnoreCase(u.getPapel().getNome()))
            .toList();

    candidatos.forEach(
        usuario -> {
          usuario.setAdmin(true);
          usuarioRepository.save(usuario);
          log.info(
              "Migração RBAC (BDR-001): usuario.id={} promovido a admin global (papel legado 'admin').",
              usuario.getId());
        });
  }
}
