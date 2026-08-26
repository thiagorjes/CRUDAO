import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { DashboardClient } from "./DashboardClient";

describe("DashboardClient", () => {
  it("exibe estado vazio quando não há etapas", () => {
    render(<DashboardClient dashboard={{ leadTimeMedioPorEtapa: [], totalTarefasConsideradas: 0 }} />);

    expect(screen.getByText(/Nenhum dado de lead-time/)).toBeInTheDocument();
  });

  it("exibe etapas ordenadas por lead-time decrescente com durações formatadas", () => {
    render(
      <DashboardClient
        dashboard={{
          totalTarefasConsideradas: 5,
          leadTimeMedioPorEtapa: [
            { etapaId: "a", etapaNome: "A fazer", leadTimeMedioSegundos: 3600, tempoImpedimentoMedioSegundos: 0 },
            {
              etapaId: "b",
              etapaNome: "Em andamento",
              leadTimeMedioSegundos: 86400 * 2,
              tempoImpedimentoMedioSegundos: 1800,
            },
          ],
        }}
      />,
    );

    expect(screen.getByText("5 tarefa(s) considerada(s) no cálculo.")).toBeInTheDocument();

    const linhas = screen.getAllByRole("row");
    // linha 0 = cabeçalho; linha 1 deve ser "Em andamento" (maior lead-time)
    expect(linhas[1]).toHaveTextContent("Em andamento");
    expect(linhas[1]).toHaveTextContent("2d");
    expect(linhas[2]).toHaveTextContent("A fazer");
  });
});
