package com.crudao.kanban.domain.rbac;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de Papel/Permissão (RF-013). Papel {@code admin} é protegido — RN-006. */
@Service
@RequiredArgsConstructor
public class PapelService {

  private final PapelRepository papelRepository;
  private final PermissaoRepository permissaoRepository;
  private final PapelMapper papelMapper;

  @Transactional(readOnly = true)
  public List<PapelDTO> listar() {
    return papelRepository.findAll().stream().map(papelMapper::paraDTO).toList();
  }

  @Transactional
  public PapelDTO criar(PapelRequest request) {
    Papel papel = new Papel();
    papel.setNome(request.nome());
    papel.setProtegido(false);
    papel.setPermissoes(resolverPermissoes(request.permissoes()));
    return papelMapper.paraDTO(papelRepository.save(papel));
  }

  @Transactional
  public PapelDTO editar(UUID id, PapelRequest request) {
    Papel papel = buscarEntidade(id);
    exigirNaoProtegido(papel, "editado");
    papel.setNome(request.nome());
    papel.setPermissoes(resolverPermissoes(request.permissoes()));
    return papelMapper.paraDTO(papelRepository.save(papel));
  }

  @Transactional
  public void excluir(UUID id) {
    Papel papel = buscarEntidade(id);
    exigirNaoProtegido(papel, "excluído");
    papelRepository.delete(papel);
  }

  /** RN-006: o papel `admin` não pode ser editado nem excluído por nenhum papel delegado. */
  private void exigirNaoProtegido(Papel papel, String acao) {
    if (papel.isProtegido()) {
      throw new RegraDeNegocioException(
          "O papel '%s' é protegido e não pode ser %s.".formatted(papel.getNome(), acao));
    }
  }

  private Set<Permissao> resolverPermissoes(Set<String> chaves) {
    return chaves.stream()
        .map(
            chave ->
                permissaoRepository
                    .findByChave(chave)
                    .orElseThrow(
                        () ->
                            new RecursoNaoEncontradoException(
                                "Permissão não encontrada: " + chave)))
        .collect(Collectors.toSet());
  }

  private Papel buscarEntidade(UUID id) {
    return papelRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Papel não encontrado: " + id));
  }
}
