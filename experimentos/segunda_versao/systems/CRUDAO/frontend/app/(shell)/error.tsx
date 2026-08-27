"use client";

/** Error boundary do shell autenticado — evita a página de erro genérica do Next.js em falhas de API. */
export default function ShellError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <div className="empty-state" role="alert">
      <p>Não foi possível carregar esta página.</p>
      <button className="btn btn-primary" type="button" onClick={reset}>
        Tentar novamente
      </button>
    </div>
  );
}
