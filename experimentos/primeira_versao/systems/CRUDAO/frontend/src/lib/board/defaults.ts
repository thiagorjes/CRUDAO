import { Etapa, Raia } from '@/lib/api/types';

/** Defaults de criação de card (RF-001, TechSpec D-04) — resolvidos no frontend a partir do
 * estado já carregado pelo BoardApp, sem endpoint novo. */
export type DefaultsNovoCard = {
  etapaInicialId: string | null;
  raiaId: string | null;
};

/**
 * Etapa padrão = etapa de menor `ordem` do workflow ativo ("coluna 0"); `null` se o workflow não
 * tiver etapas (não há "coluna 0" possível — botão "Novo card" fica desabilitado nesse estado).
 * Raia padrão = raia de menor `ordem` do projeto; `null` se o projeto não tiver raia própria.
 */
export function resolverDefaults(etapas: Etapa[], raias: Raia[]): DefaultsNovoCard {
  const etapaPadrao = etapas.reduce<Etapa | null>(
    (menor, etapa) => (!menor || etapa.ordem < menor.ordem ? etapa : menor),
    null,
  );
  const raiaPadrao = raias.reduce<Raia | null>(
    (menor, raia) => (!menor || raia.ordem < menor.ordem ? raia : menor),
    null,
  );
  return {
    etapaInicialId: etapaPadrao?.id ?? null,
    raiaId: raiaPadrao?.id ?? null,
  };
}
