"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { EtapaResponse, MembroProjeto } from "@/lib/board";
import { mensagemErro } from "@/lib/board-logic";
import { TarefaAuditoriaResponse, TarefaDetalheResponse } from "@/lib/tarefa";
import {
  camposEstruturaisBloqueados,
  formatarDuracao,
  nomeEtapa,
  ordenarAuditoriaDesc,
  ordenarHistoricoEtapas,
} from "@/lib/tarefa-logic";
import { iniciais } from "@/lib/format";

/** TL-04 — Detalhe da Tarefa (RF-003, RF-006, RF-017, TASK-07.3). */
export function TarefaDetalheClient({
  projetoId,
  tarefa,
  etapas,
  membros,
  observadoresIniciais,
  auditoria,
}: {
  projetoId: string;
  tarefa: TarefaDetalheResponse;
  etapas: EtapaResponse[];
  membros: MembroProjeto[];
  observadoresIniciais: string[];
  auditoria: TarefaAuditoriaResponse[] | null;
}) {
  const router = useRouter();
  const etapaPorId = new Map(etapas.map((e) => [e.id, e]));
  const membroPorId = new Map(membros.map((m) => [m.usuarioId, m]));
  const bloqueado = camposEstruturaisBloqueados(tarefa.iniciada);

  const [titulo, setTitulo] = useState(tarefa.titulo);
  const [descricaoEscopo, setDescricaoEscopo] = useState(tarefa.descricaoEscopo ?? "");
  const [responsavelId, setResponsavelId] = useState(tarefa.responsavelId ?? "");
  const [observadores, setObservadores] = useState(observadoresIniciais);
  const [novoObservadorId, setNovoObservadorId] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function salvar(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setSucesso(null);
    setSalvando(true);

    try {
      const body: Record<string, unknown> = {
        removerResponsavel: responsavelId === "" && tarefa.responsavelId !== null,
      };
      if (responsavelId) body.responsavelId = responsavelId;
      if (!bloqueado) {
        body.titulo = titulo.trim();
        // Sem fallback para `undefined`: apagar a descrição precisa persistir "", não ser
        // interpretado como "campo não enviado" (achado de code review, agent QA, TASK-07.3).
        body.descricaoEscopo = descricaoEscopo.trim();
      }

      const res = await fetch(`/api/tarefas/${tarefa.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        setErro(mensagemErro(res.status, "Não foi possível salvar as alterações."));
        return;
      }

      setSucesso("Alterações salvas com sucesso.");
      router.refresh();
    } finally {
      setSalvando(false);
    }
  }

  async function adicionarObservador() {
    if (!novoObservadorId) return;
    const res = await fetch(`/api/tarefas/${tarefa.id}/observadores`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ usuarioId: novoObservadorId }),
    });
    if (!res.ok) {
      setErro(mensagemErro(res.status, "Não foi possível adicionar o observador."));
      return;
    }
    setObservadores((atual) => [...atual, novoObservadorId]);
    setNovoObservadorId("");
  }

  async function removerObservador(usuarioId: string) {
    const res = await fetch(`/api/tarefas/${tarefa.id}/observadores/${usuarioId}`, {
      method: "DELETE",
    });
    if (!res.ok) {
      setErro(mensagemErro(res.status, "Não foi possível remover o observador."));
      return;
    }
    setObservadores((atual) => atual.filter((id) => id !== usuarioId));
  }

  const historicoOrdenado = ordenarHistoricoEtapas(tarefa.historicoEtapas);
  const auditoriaOrdenada = auditoria ? ordenarAuditoriaDesc(auditoria) : null;
  const observadoresDisponiveis = membros.filter((m) => !observadores.includes(m.usuarioId));

  return (
    <>
      <div className="page-header">
        <div>
          <a href={`/projetos/${projetoId}/board`} className="btn btn-text" style={{ paddingLeft: 0 }}>
            ← Board
          </a>
          <h1 style={{ fontSize: "20px" }}>{tarefa.titulo}</h1>
        </div>
        {tarefa.impedida && <span className="badge badge-warning">Impedido</span>}
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

      <div className="card" style={{ marginBottom: "16px" }}>
        <form aria-label="Formulário de edição da tarefa" onSubmit={salvar}>
          <div className="form-field">
            <label htmlFor="titulo">Título</label>
            {bloqueado ? (
              <div id="titulo" className="field-locked" aria-readonly="true">
                {tarefa.titulo}
              </div>
            ) : (
              <input
                id="titulo"
                type="text"
                required
                value={titulo}
                onChange={(e) => setTitulo(e.target.value)}
              />
            )}
          </div>

          <div className="form-field">
            <label htmlFor="descricao">Descrição {bloqueado && "(campo travado pós-início)"}</label>
            {bloqueado ? (
              <div id="descricao" className="field-locked" aria-readonly="true">
                {tarefa.descricaoEscopo || "—"}
              </div>
            ) : (
              <textarea
                id="descricao"
                rows={3}
                value={descricaoEscopo}
                onChange={(e) => setDescricaoEscopo(e.target.value)}
              />
            )}
          </div>

          <div className="form-field">
            <label htmlFor="responsavel">Responsável</label>
            <select
              id="responsavel"
              value={responsavelId}
              onChange={(e) => setResponsavelId(e.target.value)}
            >
              <option value="">Sem responsável</option>
              {membros.map((membro) => (
                <option key={membro.usuarioId} value={membro.usuarioId}>
                  {membro.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <span>Etapa atual</span>
            <p className="text-secondary">{nomeEtapa(etapaPorId, tarefa.etapaAtualId)}</p>
          </div>

          <button className="btn btn-primary" type="submit" disabled={salvando} aria-busy={salvando}>
            {salvando ? "Salvando…" : "Salvar"}
          </button>
        </form>
      </div>

      <div className="card" style={{ marginBottom: "16px" }}>
        <h2 style={{ fontSize: "14px" }}>Lead-time por etapa</h2>
        {historicoOrdenado.length === 0 && <p className="text-secondary">Sem histórico de etapas.</p>}
        <p className="text-secondary">
          {historicoOrdenado
            .map(
              (h) =>
                `${nomeEtapa(etapaPorId, h.etapaId)}: ${formatarDuracao(h.leadTimeSegundos)}${
                  h.saidaEm === null ? " (em andamento)" : ""
                }`,
            )
            .join(" · ")}
        </p>
        <p className="text-secondary">
          Impedimento acumulado: {formatarDuracao(tarefa.tempoImpedimentoTotalSegundos)}
        </p>
      </div>

      <div className="card" style={{ marginBottom: "16px" }}>
        <h2 style={{ fontSize: "14px" }}>Observadores</h2>
        {observadores.length === 0 && <p className="text-secondary">Nenhum observador explícito.</p>}
        <ul style={{ listStyle: "none", padding: 0, margin: "0 0 8px" }}>
          {observadores.map((usuarioId) => {
            const membro = membroPorId.get(usuarioId);
            return (
              <li
                key={usuarioId}
                style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}
              >
                <span className="avatar" style={{ width: 20, height: 20, fontSize: 10 }}>
                  {iniciais(membro?.nome ?? "?")}
                </span>
                <span style={{ flex: 1 }}>{membro?.nome ?? usuarioId}</span>
                <button
                  className="btn btn-text"
                  type="button"
                  onClick={() => removerObservador(usuarioId)}
                  aria-label={`Remover observador ${membro?.nome ?? usuarioId}`}
                >
                  Remover
                </button>
              </li>
            );
          })}
        </ul>
        {observadoresDisponiveis.length > 0 && (
          <div style={{ display: "flex", gap: "8px" }}>
            <select
              aria-label="Adicionar observador"
              value={novoObservadorId}
              onChange={(e) => setNovoObservadorId(e.target.value)}
            >
              <option value="">Selecione um membro</option>
              {observadoresDisponiveis.map((membro) => (
                <option key={membro.usuarioId} value={membro.usuarioId}>
                  {membro.nome}
                </option>
              ))}
            </select>
            <button className="btn btn-outline" type="button" onClick={adicionarObservador}>
              Adicionar
            </button>
          </div>
        )}
      </div>

      {auditoriaOrdenada && (
        <section aria-label="Histórico de auditoria" className="card">
          <h2 style={{ fontSize: "14px" }}>Histórico</h2>
          {auditoriaOrdenada.length === 0 && <p className="text-secondary">Sem alterações registradas.</p>}
          {auditoriaOrdenada.map((item, i) => {
            const autor = membroPorId.get(item.autorId);
            return (
              <div className="history-item" key={i}>
                <strong>{autor?.nome ?? item.autorId}</strong> alterou {item.campo}:{" "}
                {item.valorAnterior ?? "—"} → {item.valorNovo ?? "—"}{" "}
                <span className="text-secondary">
                  {new Date(item.dataHora).toLocaleString("pt-BR")}
                </span>
              </div>
            );
          })}
        </section>
      )}
    </>
  );
}
