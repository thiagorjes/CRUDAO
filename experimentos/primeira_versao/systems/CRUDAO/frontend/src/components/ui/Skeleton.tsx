/** Skeleton screen (design brief §5) — loading de operações assíncronas mais longas. */
export function Skeleton({ altura = 140 }: { altura?: number }) {
  return (
    <div
      style={{
        height: altura,
        borderRadius: 6,
        background:
          'linear-gradient(90deg, var(--color-border) 25%, #eceef0 37%, var(--color-border) 63%)',
        backgroundSize: '400% 100%',
        animation: 'crudao-skeleton 1.4s ease infinite',
      }}
    >
      <style>{`
        @keyframes crudao-skeleton {
          0% { background-position: 100% 50%; }
          100% { background-position: 0 50%; }
        }
      `}</style>
    </div>
  );
}
