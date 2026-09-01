"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import type { Etapa, Workflow } from "@/lib/types";

/**
 * TL-08 — aba "Colunas/Transições" de um workflow (docs/design/.../tl-08-admin-projeto.html).
 * Rota que antes 404ava: gerenciamento de etapas (colunas) e suas transições de saída.
 */
export default function WorkflowDetalhePage() {
  const params = useParams();
  const projetoId = params.id as string;
  const workflowId = params.workflowId as string;

  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [etapaEmEdicao, setEtapaEmEdicao] = useState<string | null>(null);
  const [criandoEtapa, setCriandoEtapa] = useState(false);
  const [novoNome, setNovoNome] = useState("");

  const carregar = useCallback(async () => {
    try {
      setLoading(true);
      const res = await fetch(`/api/admin/workflows?projetoId=${projetoId}`);
      if (!res.ok) throw new Error("Erro ao carregar workflow");
      const todos = (await res.json()) as Workflow[];
      const alvo = todos.find((w) => w.id === workflowId);
      if (!alvo) throw new Error("Workflow não encontrado");
      setWorkflow(alvo);
      setErro(null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar workflow");
    } finally {
      setLoading(false);
    }
  }, [projetoId, workflowId]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const handleCriarEtapa = async () => {
    if (!novoNome.trim() || !workflow) return;
    setCriandoEtapa(true);
    setErro(null);
    try {
      const res = await fetch(`/api/admin/workflows/${workflowId}/etapas`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome: novoNome.trim(), ordem: workflow.etapas.length + 1, etapaFinal: false }),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || `Erro ${res.status}`);
      }
      setNovoNome("");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao criar etapa");
    } finally {
      setCriandoEtapa(false);
    }
  };

  const handleExcluirEtapa = async (etapaId: string) => {
    if (!confirm("Excluir esta coluna?")) return;
    setErro(null);
    try {
      const res = await fetch(`/api/admin/etapas/${etapaId}`, { method: "DELETE" });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(
          err.message ||
            "Não é possível excluir: há tarefas ativas vinculadas ou a etapa é destino de transições (RN-005)."
        );
      }
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao excluir etapa");
    }
  };

  if (loading) {
    return <div className="skeleton" style={{ height: 16, width: "80%" }} />;
  }

  if (erro && !workflow) {
    return (
      <div className="toast toast-error" role="alert">
        {erro}
      </div>
    );
  }

  if (!workflow) return null;

  const etapasOrdenadas = [...workflow.etapas].sort((a, b) => a.ordem - b.ordem);

  return (
    <section aria-label="Colunas do workflow">
      <div className="page-header">
        <h2 style={{ fontSize: 16, margin: 0 }}>{workflow.nome}</h2>
      </div>

      {erro && (
        <div className="toast toast-error" role="alert" style={{ marginBottom: "var(--space-md)" }}>
          {erro}
        </div>
      )}

      <table>
        <thead>
          <tr>
            <th>Ordem</th>
            <th>Coluna</th>
            <th>Transições de saída</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {etapasOrdenadas.map((etapa) =>
            etapaEmEdicao === etapa.id ? (
              <EtapaEditRow
                key={etapa.id}
                etapa={etapa}
                todasEtapas={etapasOrdenadas}
                onCancelar={() => setEtapaEmEdicao(null)}
                onSalvo={async () => {
                  setEtapaEmEdicao(null);
                  await carregar();
                }}
              />
            ) : (
              <tr key={etapa.id}>
                <td>{etapa.ordem}</td>
                <td>
                  {etapa.nome}
                  {etapa.etapaFinal ? " (etapa final)" : ""}
                </td>
                <td>
                  {etapa.transicoesSaida.length === 0
                    ? "— (permite desfinalizar)"
                    : etapa.transicoesSaida
                        .map((id) => etapasOrdenadas.find((e) => e.id === id)?.nome ?? id)
                        .join(", ")}
                </td>
                <td className="row">
                  <button type="button" className="btn btn-text" onClick={() => setEtapaEmEdicao(etapa.id)}>
                    Editar
                  </button>
                  <button type="button" className="btn btn-text" onClick={() => handleExcluirEtapa(etapa.id)}>
                    Excluir
                  </button>
                </td>
              </tr>
            )
          )}
        </tbody>
      </table>

      <div className="row" style={{ marginTop: "var(--space-md)" }}>
        <input
          placeholder="Nome da nova coluna"
          value={novoNome}
          onChange={(e) => setNovoNome(e.target.value)}
          disabled={criandoEtapa}
        />
        <button type="button" className="btn btn-outline" onClick={handleCriarEtapa} disabled={criandoEtapa || !novoNome.trim()}>
          {criandoEtapa ? "Criando…" : "+ Nova coluna"}
        </button>
      </div>
    </section>
  );
}

interface EtapaEditRowProps {
  etapa: Etapa;
  todasEtapas: Etapa[];
  onCancelar: () => void;
  onSalvo: () => Promise<void>;
}

function EtapaEditRow({ etapa, todasEtapas, onCancelar, onSalvo }: EtapaEditRowProps) {
  const [nome, setNome] = useState(etapa.nome);
  const [ordem, setOrdem] = useState(etapa.ordem);
  const [etapaFinal, setEtapaFinal] = useState(etapa.etapaFinal);
  const [transicoes, setTransicoes] = useState<string[]>(etapa.transicoesSaida);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const salvar = async () => {
    setSalvando(true);
    setErro(null);
    try {
      const resEtapa = await fetch(`/api/admin/etapas/${etapa.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nome, ordem, etapaFinal }),
      });
      if (!resEtapa.ok) throw new Error((await resEtapa.json()).message || "Erro ao salvar etapa");

      const resTransicoes = await fetch(`/api/admin/etapas/${etapa.id}/transicoes`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ etapasDestinoIds: transicoes }),
      });
      if (!resTransicoes.ok) {
        throw new Error(
          (await resTransicoes.json()).message ||
            "Etapa não-final precisa de ao menos uma transição de saída (RN-003)."
        );
      }
      await onSalvo();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao salvar");
    } finally {
      setSalvando(false);
    }
  };

  const toggleTransicao = (id: string) =>
    setTransicoes((prev) => (prev.includes(id) ? prev.filter((t) => t !== id) : [...prev, id]));

  return (
    <tr>
      <td>
        <input
          type="number"
          value={ordem}
          onChange={(e) => setOrdem(Number(e.target.value))}
          style={{ width: 60 }}
          disabled={salvando}
        />
      </td>
      <td>
        <div className="stack">
          <input value={nome} onChange={(e) => setNome(e.target.value)} disabled={salvando} />
          <label className="row" style={{ fontWeight: "normal" }}>
            <input type="checkbox" checked={etapaFinal} onChange={(e) => setEtapaFinal(e.target.checked)} disabled={salvando} />
            Etapa final
          </label>
        </div>
      </td>
      <td>
        <div className="checkbox-list">
          {todasEtapas
            .filter((e) => e.id !== etapa.id)
            .map((e) => (
              <label key={e.id}>
                <input
                  type="checkbox"
                  checked={transicoes.includes(e.id)}
                  onChange={() => toggleTransicao(e.id)}
                  disabled={salvando}
                />
                {e.nome}
              </label>
            ))}
        </div>
      </td>
      <td>
        <div className="stack">
          {erro && (
            <span className="form-error" role="alert">
              {erro}
            </span>
          )}
          <div className="row">
            <button type="button" className="btn btn-primary" onClick={salvar} disabled={salvando}>
              {salvando ? "Salvando…" : "Salvar"}
            </button>
            <button type="button" className="btn btn-outline" onClick={onCancelar} disabled={salvando}>
              Cancelar
            </button>
          </div>
        </div>
      </td>
    </tr>
  );
}
