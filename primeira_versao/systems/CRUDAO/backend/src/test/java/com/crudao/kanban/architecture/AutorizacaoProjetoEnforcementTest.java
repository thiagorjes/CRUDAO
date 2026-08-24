package com.crudao.kanban.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Teste estrutural de CI (G-RBAC-06, ADR-006): todo método público de {@code @Service} de domínio
 * que grava entidade com {@code projetoId} deve conter, no corpo do método, uma chamada a {@code
 * AutorizacaoProjetoService.exigirPermissao} — direta ou via helper privado do próprio arquivo.
 * Falha propositalmente se a chamada for removida de um método listado abaixo (validação do próprio
 * mecanismo, cobrindo inclusive {@code TarefaService} — finding G1 do /analyze).
 *
 * <p>Verificação por leitura do código-fonte (não bytecode) — suficiente para pegar o esquecimento
 * silencioso que o ADR-006 identifica como risco ao trocar o AOP genérico por chamada explícita.
 */
class AutorizacaoProjetoEnforcementTest {

  private static final String RAIZ = "src/main/java/com/crudao/kanban/";

  private static final Map<String, List<String>> METODOS_ESCRITA_ESCOPADOS_A_PROJETO =
      Map.of(
          "domain/projeto/ProjetoService.java",
              List.of(
                  "editar",
                  "excluir",
                  "definirWorkflowAtivo",
                  "atualizarConfiguracao",
                  "finalizar"),
          "domain/workflow/WorkflowService.java", List.of("criar", "editar", "excluir"),
          "domain/workflow/EtapaService.java", List.of("criar", "editar", "excluir"),
          "domain/workflow/TransicaoService.java", List.of("criar", "excluir"),
          "domain/raia/RaiaService.java", List.of("criar", "editar", "excluir"),
          "domain/tarefa/TarefaService.java",
              List.of(
                  "criar",
                  "editar",
                  "excluir",
                  "mover",
                  "moverParaProjeto",
                  "marcarImpedimento",
                  "desmarcarImpedimento"));

  @Test
  void todoMetodoDeEscritaEscopadoAProjetoDeveChamarAutorizacaoProjetoService() throws IOException {
    for (var entrada : METODOS_ESCRITA_ESCOPADOS_A_PROJETO.entrySet()) {
      String conteudo = Files.readString(Path.of(RAIZ + entrada.getKey()));
      for (String metodo : entrada.getValue()) {
        String corpo = extrairCorpoDoMetodo(conteudo, metodo);
        assertThat(corpo)
            .as(
                "%s#%s deve chamar exigirPermissao/AutorizacaoProjetoService (G-RBAC-06)",
                entrada.getKey(), metodo)
            .isNotNull()
            .contains("exigirPermissao(");
      }
    }
  }

  /**
   * Localiza a assinatura {@code nomeMetodo(} e extrai o corpo delimitado pelas chaves
   * correspondentes (contagem de profundidade). Assume formatação padrão do projeto (Spotless).
   */
  private String extrairCorpoDoMetodo(String conteudo, String nomeMetodo) {
    String marcador = " " + nomeMetodo + "(";
    int inicioAssinatura = conteudo.indexOf(marcador);
    if (inicioAssinatura < 0) {
      return null;
    }
    int inicioCorpo = conteudo.indexOf('{', inicioAssinatura);
    if (inicioCorpo < 0) {
      return null;
    }
    int profundidade = 0;
    for (int i = inicioCorpo; i < conteudo.length(); i++) {
      char c = conteudo.charAt(i);
      if (c == '{') {
        profundidade++;
      } else if (c == '}') {
        profundidade--;
        if (profundidade == 0) {
          return conteudo.substring(inicioCorpo, i + 1);
        }
      }
    }
    return null;
  }
}
