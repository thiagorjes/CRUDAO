package com.crudao.kanban.domain.rbac;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.EntradaInvalidaException;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.security.UsuarioContexto;
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
class MembroProjetoServiceTest {

  @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private PapelRepository papelRepository;
  @Mock private ProjetoRepository projetoRepository;
  @Mock private UsuarioContexto usuarioContexto;

  private MembroProjetoService membroProjetoService;
  private Projeto projeto;
  private Usuario usuarioAlvo;

  @BeforeEach
  void setUp() {
    membroProjetoService =
        new MembroProjetoService(
            usuarioProjetoPapelRepository,
            usuarioRepository,
            papelRepository,
            projetoRepository,
            usuarioContexto);

    projeto = new Projeto();
    projeto.setId(UUID.randomUUID());
    lenient().when(projetoRepository.findById(projeto.getId())).thenReturn(Optional.of(projeto));

    usuarioAlvo = new Usuario();
    usuarioAlvo.setId(UUID.randomUUID());
    lenient()
        .when(usuarioRepository.findById(usuarioAlvo.getId()))
        .thenReturn(Optional.of(usuarioAlvo));
  }

  @Test
  void deveRejeitarAtribuicaoDePapelProtegido_422() {
    Usuario admin = new Usuario();
    admin.setAdmin(true);
    when(usuarioContexto.usuarioAtual()).thenReturn(admin);

    Papel papelAdmin = new Papel();
    papelAdmin.setId(UUID.randomUUID());
    papelAdmin.setProtegido(true);
    when(papelRepository.findById(papelAdmin.getId())).thenReturn(Optional.of(papelAdmin));

    assertThatThrownBy(
            () ->
                membroProjetoService.atribuir(
                    projeto.getId(),
                    usuarioAlvo.getId(),
                    new AtribuirPapeisRequest(List.of(papelAdmin.getId()))))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  @Test
  void deveRejeitarAtribuicaoDePapelComPapelGerenciar_422_G_RBAC_07() {
    Usuario admin = new Usuario();
    admin.setAdmin(true);
    when(usuarioContexto.usuarioAtual()).thenReturn(admin);

    Papel papelComPapelGerenciar = new Papel();
    papelComPapelGerenciar.setId(UUID.randomUUID());
    papelComPapelGerenciar.setPermissoes(
        Set.of(new Permissao(UUID.randomUUID(), "papel:gerenciar")));
    when(papelRepository.findById(papelComPapelGerenciar.getId()))
        .thenReturn(Optional.of(papelComPapelGerenciar));

    assertThatThrownBy(
            () ->
                membroProjetoService.atribuir(
                    projeto.getId(),
                    usuarioAlvo.getId(),
                    new AtribuirPapeisRequest(List.of(papelComPapelGerenciar.getId()))))
        .isInstanceOf(EntradaInvalidaException.class);
  }

  @Test
  void deveNegarUsuarioSemVinculoAoListarMembros() {
    Usuario semVinculo = new Usuario();
    semVinculo.setId(UUID.randomUUID());
    when(usuarioContexto.usuarioAtual()).thenReturn(semVinculo);
    when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(
            semVinculo.getId(), projeto.getId()))
        .thenReturn(List.of());

    assertThatThrownBy(() -> membroProjetoService.listar(projeto.getId()))
        .isInstanceOf(AcessoNegadoException.class);
  }

  @Test
  void deveNegarUsuarioComumAoAtribuirPapeis() {
    Usuario comum = new Usuario();
    comum.setId(UUID.randomUUID());
    when(usuarioContexto.usuarioAtual()).thenReturn(comum);
    when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(comum.getId(), projeto.getId()))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                membroProjetoService.atribuir(
                    projeto.getId(), usuarioAlvo.getId(), new AtribuirPapeisRequest(List.of())))
        .isInstanceOf(AcessoNegadoException.class);
  }

  @Test
  void devePermitirAdminGlobalAtribuirPapelValido() {
    Usuario admin = new Usuario();
    admin.setAdmin(true);
    when(usuarioContexto.usuarioAtual()).thenReturn(admin);

    Papel papelDev = new Papel();
    papelDev.setId(UUID.randomUUID());
    papelDev.setNome("dev");
    when(papelRepository.findById(papelDev.getId())).thenReturn(Optional.of(papelDev));

    assertThatCode(
            () ->
                membroProjetoService.atribuir(
                    projeto.getId(),
                    usuarioAlvo.getId(),
                    new AtribuirPapeisRequest(List.of(papelDev.getId()))))
        .doesNotThrowAnyException();
  }
}
