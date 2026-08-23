'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Tarefa, Usuario } from '@/lib/api/types';
import { AcaoTransicao } from '@/lib/board/transicoes';
import styles from './CardTarefa.module.css';

const ROTULO_ACAO: Record<AcaoTransicao['rotulo'], string> = {
  avancar: '→ ',
  retroceder: '← ',
  reabertura: '↺ Reabrir em: ',
};

export function CardTarefa({
  tarefa,
  responsavel,
  acoes,
  onExecutarAcao,
  onArrastarInicio,
}: {
  tarefa: Tarefa;
  responsavel: Usuario | undefined;
  acoes: AcaoTransicao[];
  onExecutarAcao: (acao: AcaoTransicao) => void;
  onArrastarInicio: (tarefaId: string) => void;
}) {
  const [menuAberto, setMenuAberto] = useState(false);
  const router = useRouter();

  const inicial = responsavel?.nome?.trim().charAt(0).toUpperCase() ?? '?';

  return (
    <div
      className={styles.card}
      draggable
      onDragStart={(e) => {
        e.dataTransfer.setData('text/plain', tarefa.id);
        onArrastarInicio(tarefa.id);
      }}
      onClick={() => router.push(`/tarefas/${tarefa.id}`)}
      role="button"
      tabIndex={0}
    >
      <div className={styles.linha}>
        <span className={styles.tipo}>{tarefa.tipo}</span>
        {tarefa.impedida && <span className={styles.impedida} title="Impedida" />}
        <span style={{ flex: 1 }} />
        <button
          className={styles.menuBotao}
          aria-label="Menu de ações da tarefa"
          onClick={(e) => {
            e.stopPropagation();
            setMenuAberto((aberto) => !aberto);
          }}
        >
          ⋮
        </button>
      </div>

      <div className={styles.titulo}>{tarefa.titulo}</div>

      <div className={styles.rodape}>
        <div className={styles.responsavel}>
          <span className={styles.avatar}>{inicial}</span>
          {responsavel?.nome ?? 'Sem responsável'}
        </div>
      </div>

      {menuAberto && (
        <div className={styles.menu} onClick={(e) => e.stopPropagation()}>
          {acoes.length === 0 && (
            <span className={styles.menuItem} style={{ color: 'var(--color-text-secondary)' }}>
              Nenhuma transição disponível
            </span>
          )}
          {acoes.map((acao) => (
            <button
              key={acao.transicaoId}
              className={`${styles.menuItem} ${acao.rotulo === 'reabertura' ? styles.menuItemReabertura : ''}`}
              onClick={() => {
                setMenuAberto(false);
                onExecutarAcao(acao);
              }}
            >
              {ROTULO_ACAO[acao.rotulo]}
              {acao.etapaDestinoNome}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
