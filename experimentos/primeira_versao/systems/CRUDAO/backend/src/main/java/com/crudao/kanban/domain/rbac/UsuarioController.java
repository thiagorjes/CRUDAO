package com.crudao.kanban.domain.rbac;

import com.crudao.kanban.security.UsuarioContexto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listagem somente-leitura de usuários — usada pelo board para resolver {@code responsavelId} em
 * nome/inicial (RF-001) e para o seletor de observadores (RF-005). Sem endpoint de escrita: o
 * cadastro de usuário é feito via auto-provisionamento no primeiro login (Keycloak) ou por seed
 * (login local de fallback, ADR-003) — RBAC/edição de papel fica em {@link PapelController};
 * associação por projeto fica em {@link MembroProjetoController} (RF-015).
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  private final UsuarioContexto usuarioContexto;

  @GetMapping
  public List<UsuarioDTO> listar() {
    return usuarioRepository.findAll().stream()
        .map(u -> new UsuarioDTO(u.getId(), u.getNome()))
        .toList();
  }

  /** Perfil do usuário autenticado — papéis/permissões efetivas por projeto (RF-015). */
  @GetMapping("/me")
  public UsuarioMeDTO me() {
    Usuario usuario = usuarioContexto.usuarioAtual();
    List<UsuarioProjetoPapel> vinculos =
        usuarioProjetoPapelRepository.findComPapelEPermissoesByUsuarioId(usuario.getId());

    Map<UUID, List<UsuarioProjetoPapel>> porProjeto =
        vinculos.stream().collect(Collectors.groupingBy(UsuarioProjetoPapel::getProjetoId));

    List<ProjetoPapeisDTO> projetos =
        porProjeto.entrySet().stream()
            .map(
                entrada ->
                    new ProjetoPapeisDTO(
                        entrada.getKey(),
                        entrada.getValue().stream()
                            .map(v -> v.getPapel().getNome())
                            .distinct()
                            .toList(),
                        entrada.getValue().stream()
                            .flatMap(v -> v.getPapel().getPermissoes().stream())
                            .map(Permissao::getChave)
                            .collect(Collectors.toSet())))
            .toList();

    return new UsuarioMeDTO(usuario.getId(), usuario.getNome(), usuario.isAdmin(), projetos);
  }
}
