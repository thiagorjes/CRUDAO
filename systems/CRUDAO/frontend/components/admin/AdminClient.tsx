"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { EtapaResponse, ProjetoResponse, RaiaResponse, WorkflowResponse } from "@/lib/admin";
import { mensagemErroAdmin, nomesTransicoesSaida, ordenarPorOrdem } from "@/lib/admin-logic";
import { ConfirmModal } from "./ConfirmModal";
import { EtapaModal } from "./EtapaModal";
import { RaiaModal } from "./RaiaModal";
import { TransicoesModal } from "./TransicoesModal";

type Aba = "colunas" | "transicoes" | "raias";

/** TL-08 — Admin de Projeto (RF-008/009/010/011, TASK-07.4). UI só decora — backend revalida tudo. */
export function AdminClient({
  projeto,
  workflow,
  raias,
}: {
  projeto: ProjetoResponse;
  workflow: WorkflowResponse | null;
  raias: RaiaResponse[];
}) {
  const router = useRouter();
  const [aba, setAba] = useState<Aba>("colunas");
  const [nome, setNome] = useState(projeto.nome);
  const [descricao, setDescricao] = useState(projeto.descricao ?? "");
  const [salvandoProjeto, setSalvandoProjeto] = useState(false);
  const [alternandoStatus, setAlternandoStatus] = useState(false);
  const [criandoWorkflow, setCriandoWorkflow] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const [etapaModal, setEtapaModal] = useState<{ etapa: EtapaResponse | null } | null>(null);
  const [transicoesModal, setTransicoesModal] = useState<EtapaResponse | null>(null);
  const [raiaModal, setRaiaModal] = useState<{ raia: RaiaResponse | null } | null>(null);
  const [excluir, setExcluir] = useState<{ tipo: "etapa" | "raia" | "workflow"; id: string; nome: string } | null>(
    null,
  );

  const finalizado = projeto.status === "FINALIZADO";
  const etapasOrdenadas = workflow ? ordenarPorOrdem(workflow.etapas) : [];
  const etapaPorId = new Map(etapasOrdenadas.map((e) => [e.id, e]));
  const raiasOrdenadas = ordenarPorOrdem(raias.filter((r) => !r.global));

  function tratarErro(mensagem: string) {
    setErro(mensagem);
    setSucesso(null);
  }

  function tratarSucesso(mensagem: string) {
    setSucesso(mensagem);
    setErro(null);
    router.refresh();
  }

  async function salvarProjeto(e: FormEvent) {
    e.preventDefault();
    setSalvandoProjeto(true);
    try {
      const res = await fetch(`/api/projetos/${projeto.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome: nome.trim(), descricao: descricao.trim() }),
      });
      if (!res.ok) {
        tratarErro(mensagemErroAdmin(res.status, "Não foi possível salvar o projeto."));
        return;
      }
      tratarSucesso("Projeto atualizado com sucesso.");
    } finally {
      setSalvandoProjeto(false);
    }
  }

  async function alternarStatusProjeto() {
    setAlternandoStatus(true);
    try {
      const acao = finalizado ? "reabrir" : "finalizar";
      const res = await fetch(`/api/projetos/${projeto.id}/${acao}`, { method: "POST" });
      if (!res.ok) {
        tratarErro(mensagemErroAdmin(res.status, `Não foi possível ${acao} o projeto.`));
        return;
      }
      tratarSucesso(finalizado ? "Projeto reaberto." : "Projeto finalizado.");
    } finally {
      setAlternandoStatus(false);
    }
  }

  async function criarWorkflow() {
    setCriandoWorkflow(true);
    try {
      const res = await fetch(`/api/projetos/${projeto.id}/workflows`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome: "Workflow padrão" }),
      });
      if (!res.ok) {
        tratarErro(mensagemErroAdmin(res.status, "Não foi possível criar o workflow."));
        return;
      }
      tratarSucesso("Workflow criado com sucesso.");
    } finally {
      setCriandoWorkflow(false);
    }
  }

  async function confirmarExclusao() {
    if (!excluir) return;
    const url =
      excluir.tipo === "etapa"
        ? `/api/etapas/${excluir.id}`
        : excluir.tipo === "raia"
          ? `/api/raias/${excluir.id}`
          : `/api/workflows/${excluir.id}`;
    const res = await fetch(url, { method: "DELETE" });
    setExcluir(null);
    if (!res.ok) {
      tratarErro(mensagemErroAdmin(res.status, `Não foi possível excluir ${excluir.nome}.`));
      return;
    }
    tratarSucesso(`"${excluir.nome}" excluído com sucesso.`);
  }

  return (
    <>
      <div className="page-header">
        <h1>Admin de Projeto</h1>
        <div style={{ display: "flex", gap: "8px" }}>
          <button
            className={finalizado ? "btn btn-outline" : "btn btn-danger"}
            type="button"
            onClick={alternarStatusProjeto}
            disabled={alternandoStatus}
            aria-busy={alternandoStatus}
          >
            {finalizado ? "Reabrir projeto" : "Finalizar projeto"}
          </button>
        </div>
      </div>

      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "16px" }}>
          <span style={{ flex: 1 }}>{erro}</span>
          <button className="btn btn-text" type="button" onClick={() => setErro(null)} aria-label="Fechar erro">
            ✕
          </button>
        </div>
      )}
      {sucesso && (
        <div className="toast toast-success" role="status" aria-live="polite" style={{ marginBottom: "16px" }}>
          {sucesso}
        </div>
      )}
      {finalizado && (
        <div className="toast toast-error" role="status" style={{ marginBottom: "16px" }}>
          Projeto finalizado — reabra para editar workflow, etapas, transições ou raias (RN-015).
        </div>
      )}

      <div className="card" style={{ marginBottom: "16px" }}>
        <form aria-label="Formulário de dados do projeto" onSubmit={salvarProjeto}>
          <div className="form-field">
            <label htmlFor="projeto-nome">Nome</label>
            <input
              id="projeto-nome"
              type="text"
              required
              disabled={finalizado}
              value={nome}
              onChange={(e) => setNome(e.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="projeto-descricao">Descrição</label>
            <textarea
              id="projeto-descricao"
              rows={2}
              disabled={finalizado}
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
            />
          </div>
          <button
            className="btn btn-primary"
            type="submit"
            disabled={salvandoProjeto || finalizado}
            aria-busy={salvandoProjeto}
          >
            {salvandoProjeto ? "Salvando…" : "Salvar dados do projeto"}
          </button>
        </form>
      </div>

      {!workflow ? (
        <div className="empty-state">
          Este projeto ainda não possui workflow configurado.
          <br />
          <button
            className="btn btn-primary"
            type="button"
            style={{ marginTop: "8px" }}
            onClick={criarWorkflow}
            disabled={criandoWorkflow || finalizado}
          >
            {criandoWorkflow ? "Criando…" : "Criar workflow"}
          </button>
        </div>
      ) : (
        <>
          <div className="tabs" role="tablist" aria-label="Configuração de workflow">
            <button role="tab" aria-selected={aba === "colunas"} type="button" onClick={() => setAba("colunas")}>
              Colunas
            </button>
            <button
              role="tab"
              aria-selected={aba === "transicoes"}
              type="button"
              onClick={() => setAba("transicoes")}
            >
              Transições
            </button>
            <button role="tab" aria-selected={aba === "raias"} type="button" onClick={() => setAba("raias")}>
              Raias
            </button>
          </div>

          {aba === "colunas" && (
            <section aria-label="Colunas do workflow">
              <table>
                <thead>
                  <tr>
                    <th>Ordem</th>
                    <th>Coluna</th>
                    <th>Final?</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {etapasOrdenadas.map((et) => (
                    <tr key={et.id}>
                      <td>{et.ordem}</td>
                      <td>{et.nome}</td>
                      <td>{et.etapaFinal ? "Sim" : "Não"}</td>
                      <td style={{ display: "flex", gap: "8px" }}>
                        <button
                          className="btn btn-text"
                          type="button"
                          disabled={finalizado}
                          onClick={() => setEtapaModal({ etapa: et })}
                        >
                          Editar
                        </button>
                        <button
                          className="btn btn-text"
                          type="button"
                          disabled={finalizado}
                          onClick={() => setExcluir({ tipo: "etapa", id: et.id, nome: et.nome })}
                        >
                          Excluir
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <button
                className="btn btn-outline"
                type="button"
                style={{ marginTop: "var(--space-md)" }}
                disabled={finalizado}
                onClick={() => setEtapaModal({ etapa: null })}
              >
                + Nova coluna
              </button>
            </section>
          )}

          {aba === "transicoes" && (
            <section aria-label="Transições do workflow">
              <table>
                <thead>
                  <tr>
                    <th>Coluna</th>
                    <th>Transições de saída</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {etapasOrdenadas.map((et) => (
                    <tr key={et.id}>
                      <td>{et.nome}</td>
                      <td>{nomesTransicoesSaida(et, etapaPorId)}</td>
                      <td>
                        <button
                          className="btn btn-text"
                          type="button"
                          disabled={finalizado}
                          onClick={() => setTransicoesModal(et)}
                        >
                          Editar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}

          {aba === "raias" && (
            <section aria-label="Raias do projeto">
              <table>
                <thead>
                  <tr>
                    <th>Ordem</th>
                    <th>Raia</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {raiasOrdenadas.length === 0 && (
                    <tr>
                      <td colSpan={3} className="text-secondary">
                        Usando a raia padrão global — nenhuma raia própria criada.
                      </td>
                    </tr>
                  )}
                  {raiasOrdenadas.map((r) => (
                    <tr key={r.id}>
                      <td>{r.ordem}</td>
                      <td>{r.nome}</td>
                      <td style={{ display: "flex", gap: "8px" }}>
                        <button
                          className="btn btn-text"
                          type="button"
                          disabled={finalizado}
                          onClick={() => setRaiaModal({ raia: r })}
                        >
                          Editar
                        </button>
                        <button
                          className="btn btn-text"
                          type="button"
                          disabled={finalizado}
                          onClick={() => setExcluir({ tipo: "raia", id: r.id, nome: r.nome })}
                        >
                          Excluir
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <button
                className="btn btn-outline"
                type="button"
                style={{ marginTop: "var(--space-md)" }}
                disabled={finalizado}
                onClick={() => setRaiaModal({ raia: null })}
              >
                + Nova raia
              </button>
            </section>
          )}
        </>
      )}

      {etapaModal && workflow && (
        <EtapaModal
          workflowId={workflow.id}
          etapa={etapaModal.etapa}
          onFechar={() => setEtapaModal(null)}
          onSalvo={() => {
            setEtapaModal(null);
            tratarSucesso("Coluna salva com sucesso.");
          }}
          onErro={(msg) => {
            setEtapaModal(null);
            tratarErro(msg);
          }}
        />
      )}

      {transicoesModal && (
        <TransicoesModal
          etapa={transicoesModal}
          etapas={etapasOrdenadas}
          onFechar={() => setTransicoesModal(null)}
          onSalvo={() => {
            setTransicoesModal(null);
            tratarSucesso("Transições salvas com sucesso.");
          }}
          onErro={(msg) => {
            setTransicoesModal(null);
            tratarErro(msg);
          }}
        />
      )}

      {raiaModal && (
        <RaiaModal
          projetoId={projeto.id}
          raia={raiaModal.raia}
          onFechar={() => setRaiaModal(null)}
          onSalvo={() => {
            setRaiaModal(null);
            tratarSucesso("Raia salva com sucesso.");
          }}
          onErro={(msg) => {
            setRaiaModal(null);
            tratarErro(msg);
          }}
        />
      )}

      {excluir && (
        <ConfirmModal
          titulo={`Excluir ${excluir.tipo === "etapa" ? "coluna" : excluir.tipo === "raia" ? "raia" : "workflow"}`}
          mensagem={`Tem certeza de que deseja excluir "${excluir.nome}"? Esta ação não pode ser desfeita. Bloqueada se houver tarefas ativas vinculadas.`}
          onCancelar={() => setExcluir(null)}
          onConfirmar={confirmarExclusao}
        />
      )}
    </>
  );
}
