'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { api, ApiError } from '@/lib/api/client';
import { Projeto, UsuarioMe } from '@/lib/api/types';
import { ModalErro } from '@/components/ui/ModalErro';
import { Skeleton } from '@/components/ui/Skeleton';
import { ProjetosAba } from './abas/ProjetosAba';
import { WorkflowsAba } from './abas/WorkflowsAba';
import { RaiasAba } from './abas/RaiasAba';
import { MembrosAba } from './abas/MembrosAba';
import { TogglesAba } from './abas/TogglesAba';
import { PapeisAba } from './abas/PapeisAba';
import styles from './AdminApp.module.css';

type Aba = 'projeto' | 'workflows' | 'raias' | 'membros' | 'toggles' | 'papeis';

/**
 * Painel de Administração (RF-008 a RF-011, RF-013, RF-015, RF-016) — separado do board, com
 * seletor de projeto próprio. `GET /api/usuarios/me` é usado só para gating de UI (esconder/desabilitar);
 * o backend revalida toda ação (RNF-003, ADR-006) — ver TASK-05.3.
 */
export function AdminApp() {
  const [usuarioMe, setUsuarioMe] = useState<UsuarioMe | null>(null);
  const [projetos, setProjetos] = useState<Projeto[] | null>(null);
  const [projetoId, setProjetoId] = useState<string | null>(null);
  const [projetoAtual, setProjetoAtual] = useState<Projeto | null>(null);
  const [aba, setAba] = useState<Aba>('projeto');
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([api.get<UsuarioMe>('/usuarios/me'), api.get<Projeto[]>('/projetos')])
      .then(([me, todosProjetos]) => {
        setUsuarioMe(me);
        const visiveis = me.admin
          ? todosProjetos
          : todosProjetos.filter((p) => me.projetos.some((mp) => mp.projetoId === p.id));
        setProjetos(visiveis);
        const salvo = typeof window !== 'undefined' ? localStorage.getItem('crudao_projeto_id') : null;
        setProjetoId(visiveis.find((p) => p.id === salvo)?.id ?? visiveis[0]?.id ?? null);
      })
      .catch(() => setErro('Não foi possível carregar o painel de administração.'));
  }, []);

  useEffect(() => {
    (async () => {
      if (!projetoId) {
        setProjetoAtual(null);
        return;
      }
      localStorage.setItem('crudao_projeto_id', projetoId);
      try {
        setProjetoAtual(await api.get<Projeto>(`/projetos/${projetoId}`));
      } catch {
        setErro('Não foi possível carregar o projeto selecionado.');
      }
    })();
  }, [projetoId]);

  const permissoesProjeto = useMemo(() => {
    if (!usuarioMe || !projetoId) return new Set<string>();
    const vinculo = usuarioMe.projetos.find((p) => p.projetoId === projetoId);
    return new Set(vinculo?.permissoes ?? []);
  }, [usuarioMe, projetoId]);

  const admin = usuarioMe?.admin ?? false;
  // `admin` global tem acesso irrestrito por definição — checagem direta por chave em vez de
  // sintetizar um Set fixo, para não esquecer de estender ao introduzir uma nova permissão
  // escopada a projeto numa aba futura (achado de code review TASK-05.3, guardrail G-FE-02).
  const podeGerenciarProjeto = admin || permissoesProjeto.has('projeto:gerenciar');
  const podeGerenciarWorkflow = admin || permissoesProjeto.has('workflow:gerenciar');
  const bloqueado = Boolean(projetoAtual?.dataFinalizacao);

  function tratarErro(e: unknown, padrao: string) {
    setErro(e instanceof ApiError ? e.message : padrao);
  }

  function atualizarProjetoNaLista(atualizado: Projeto) {
    setProjetoAtual(atualizado);
    setProjetos((atual) => atual?.map((p) => (p.id === atualizado.id ? atualizado : p)) ?? atual);
  }

  if (erro && !projetos) {
    return (
      <div className={styles.pagina}>
        <p className={styles.vazio}>{erro}</p>
      </div>
    );
  }

  if (!projetos || !usuarioMe) {
    return (
      <div className={styles.pagina}>
        <Skeleton />
      </div>
    );
  }

  const abas: { id: Aba; label: string; visivel: boolean }[] = [
    { id: 'projeto', label: 'Projeto', visivel: true },
    { id: 'workflows', label: 'Workflows e etapas', visivel: podeGerenciarWorkflow },
    { id: 'raias', label: 'Raias', visivel: podeGerenciarProjeto },
    { id: 'membros', label: 'Membros', visivel: podeGerenciarProjeto },
    { id: 'toggles', label: 'Configurações', visivel: podeGerenciarProjeto },
    { id: 'papeis', label: 'Papéis e permissões', visivel: admin },
  ];
  const abaAtual = abas.find((a) => a.id === aba && a.visivel) ? aba : 'projeto';

  return (
    <div className={styles.pagina}>
      <div className={styles.cabecalho}>
        <h1 className={styles.titulo}>Administração</h1>
        {projetos.length > 0 && (
          <select
            className={styles.seletorProjeto}
            value={projetoId ?? ''}
            onChange={(e) => setProjetoId(e.target.value)}
          >
            {projetos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nome}
              </option>
            ))}
          </select>
        )}
        <Link href="/" style={{ marginLeft: 'auto', font: 'var(--font-body)' }}>
          ← Voltar ao board
        </Link>
      </div>

      {bloqueado && (
        <p className={styles.banner}>
          Este projeto está finalizado — somente leitura. {podeGerenciarProjeto && 'Reabra na aba "Projeto" para editar.'}
        </p>
      )}

      <div className={styles.abas}>
        {abas
          .filter((a) => a.visivel)
          .map((a) => (
            <button
              key={a.id}
              className={`${styles.aba} ${abaAtual === a.id ? styles.abaAtiva : ''}`}
              onClick={() => setAba(a.id)}
            >
              {a.label}
            </button>
          ))}
      </div>

      {projetos.length === 0 && !admin && (
        <p className={styles.vazio}>Você não é membro de nenhum projeto ainda.</p>
      )}

      {/* Sem projeto algum, `projetoAtual` nunca existe — mas o admin precisa desta aba mesmo
          assim para poder criar o primeiro projeto (bootstrap, achado de uso real). */}
      {(projetoAtual || (projetos.length === 0 && admin)) && abaAtual === 'projeto' && (
        <ProjetosAba
          projeto={projetoAtual}
          admin={admin}
          podeGerenciar={podeGerenciarProjeto}
          bloqueado={bloqueado}
          onProjetoAtualizado={atualizarProjetoNaLista}
          onProjetoCriado={(novo) => {
            setProjetos((atual) => [...(atual ?? []), novo]);
            setProjetoId(novo.id);
          }}
          onErro={tratarErro}
        />
      )}
      {projetoAtual && abaAtual === 'workflows' && (
        <WorkflowsAba
          projeto={projetoAtual}
          bloqueado={bloqueado}
          onProjetoAtualizado={atualizarProjetoNaLista}
          onErro={tratarErro}
        />
      )}
      {projetoAtual && abaAtual === 'raias' && (
        <RaiasAba projeto={projetoAtual} admin={admin} bloqueado={bloqueado} onErro={tratarErro} />
      )}
      {projetoAtual && abaAtual === 'membros' && (
        <MembrosAba projeto={projetoAtual} bloqueado={bloqueado} onErro={tratarErro} />
      )}
      {projetoAtual && abaAtual === 'toggles' && (
        <TogglesAba projeto={projetoAtual} bloqueado={bloqueado} onErro={tratarErro} />
      )}
      {abaAtual === 'papeis' && admin && <PapeisAba onErro={tratarErro} />}

      {erro && <ModalErro mensagem={erro} onFechar={() => setErro(null)} />}
    </div>
  );
}
