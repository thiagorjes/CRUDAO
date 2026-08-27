package com.crudao.kanban.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.rbac.Papel;
import com.crudao.kanban.domain.rbac.Permissao;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.domain.rbac.UsuarioProjetoPapel;
import com.crudao.kanban.domain.rbac.UsuarioProjetoPapelRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutorizacaoProjetoServiceTest {

  @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  @Mock private ProjetoRepository projetoRepository;

  private AutorizacaoProjetoService autorizacaoProjetoService;
  private Projeto projeto;
  private Usuario usuarioComum;

  @BeforeEach
  void setUp() {
    autorizacaoProjetoService =
        new AutorizacaoProjetoService(usuarioProjetoPapelRepository, projetoRepository);

    projeto = new Projeto();
    projeto.setId(UUID.randomUUID());
    projeto.setNome("Projeto A");
    when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

    usuarioComum = new Usuario();
    usuarioComum.setId(UUID.randomUUID());
  }

  @Test
  void deveAutorizarAdminGlobalSemConsultarUsuarioProjetoPapel() {
    usuarioComum.setAdmin(true);

    assertThatCode(
            () ->
                autorizacaoProjetoService.exigirPermissao(
                    usuarioComum, projeto.getId(), "tarefa:gerenciar"))
        .doesNotThrowAnyException();

    org.mockito.Mockito.verifyNoInteractions(usuarioProjetoPapelRepository);
  }

  @Test
  void deveAutorizarUsuarioComPapelNoProjetoQueTemAPermissao() {
    when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(
            usuarioComum.getId(), projeto.getId()))
        .thenReturn(List.of(vinculoComPermissao("tarefa:gerenciar")));

    assertThatCode(
            () ->
                autorizacaoProjetoService.exigirPermissao(
                    usuarioComum, projeto.getId(), "tarefa:gerenciar"))
        .doesNotThrowAnyException();
  }

  @Test
  void deveAcumularPermissoesDeDoisPapeisNoMesmoProjeto() {
    when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(
            usuarioComum.getId(), projeto.getId()))
        .thenReturn(
            List.of(vinculoComPermissao("dashboard:visualizar"), vinculoComPermissao("dev-only")));

    assertThatCode(
            () ->
                autorizacaoProjetoService.exigirPermissao(
                    usuarioComum, projeto.getId(), "dev-only"))
        .doesNotThrowAnyException();
  }

  @Test
  void deveNegarUsuarioComPapelSoEmOutroProjeto() {
    when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(
            usuarioComum.getId(), projeto.getId()))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                autorizacaoProjetoService.exigirPermissao(
                    usuarioComum, projeto.getId(), "tarefa:gerenciar"))
        .isInstanceOf(AcessoNegadoException.class);
  }

  @Test
  void deveBloquearEscritaEmProjetoFinalizadoMesmoParaAdmin_RN015() {
    projeto.setDataFinalizacao(Instant.now());
    usuarioComum.setAdmin(true);

    assertThatThrownBy(
            () ->
                autorizacaoProjetoService.exigirPermissao(
                    usuarioComum, projeto.getId(), "tarefa:gerenciar"))
        .isInstanceOf(RegraDeNegocioException.class);
  }

  @Test
  void deveBloquearMesmoComPermissaoProjetoGerenciar_ProjetoGerenciarNaoEhBypassGeral_TASK_01_3() {
    // Achado do code review da TASK-01.3: exigirPermissao NUNCA deve liberar projeto finalizado,
    // nem para "projeto:gerenciar" — editar/excluir/atualizarConfiguracao do próprio ProjetoService
    // usam essa mesma chave e não podem reabrir por acidente. Só exigirPermissaoParaReabertura
    // pode.
    projeto.setDataFinalizacao(Instant.now());
    usuarioComum.setAdmin(true);

    assertThatThrownBy(
            () ->
                autorizacaoProjetoService.exigirPermissao(
                    usuarioComum, projeto.getId(), "projeto:gerenciar"))
        .isInstanceOf(RegraDeNegocioException.class);
  }

  @Test
  void exigirPermissaoParaReaberturaDevePermitirMesmoComProjetoFinalizado() {
    projeto.setDataFinalizacao(Instant.now());
    usuarioComum.setAdmin(true);

    assertThatCode(
            () ->
                autorizacaoProjetoService.exigirPermissaoParaReabertura(
                    usuarioComum, projeto.getId()))
        .doesNotThrowAnyException();
  }

  @Test
  void exigirPermissaoParaReaberturaDeveNegarUsuarioSemPermissaoProjetoGerenciar() {
    projeto.setDataFinalizacao(Instant.now());
    when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(
            usuarioComum.getId(), projeto.getId()))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                autorizacaoProjetoService.exigirPermissaoParaReabertura(
                    usuarioComum, projeto.getId()))
        .isInstanceOf(AcessoNegadoException.class);
  }

  private UsuarioProjetoPapel vinculoComPermissao(String chave) {
    Papel papel = new Papel();
    papel.setNome("papel-teste");
    papel.setPermissoes(Set.of(new Permissao(UUID.randomUUID(), chave)));
    UsuarioProjetoPapel vinculo = new UsuarioProjetoPapel();
    vinculo.setPapel(papel);
    return vinculo;
  }
}
