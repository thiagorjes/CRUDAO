'use client';

/** Modal de confirmação/erro (design brief §5) — feedback que exige atenção do usuário. */
export function ModalErro({ mensagem, onFechar }: { mensagem: string; onFechar: () => void }) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
      onClick={onFechar}
    >
      <div
        style={{
          background: 'var(--color-surface)',
          borderRadius: 8,
          padding: 'var(--spacing-lg)',
          maxWidth: 420,
          boxShadow: '0 8px 24px rgba(0,0,0,0.2)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--spacing-sm)',
            marginBottom: 'var(--spacing-md)',
          }}
        >
          <span
            style={{
              width: 20,
              height: 20,
              borderRadius: '50%',
              background: 'var(--color-error)',
              flexShrink: 0,
            }}
          />
          <strong style={{ font: 'var(--font-heading-2)' }}>Não foi possível concluir</strong>
        </div>
        <p style={{ font: 'var(--font-body)', color: 'var(--color-text-primary)', marginBottom: 'var(--spacing-lg)' }}>
          {mensagem}
        </p>
        <button
          onClick={onFechar}
          style={{
            padding: '8px 16px',
            background: 'var(--color-primary)',
            color: '#fff',
            border: 'none',
            borderRadius: 6,
            font: 'var(--font-body)',
          }}
        >
          Fechar
        </button>
      </div>
    </div>
  );
}
