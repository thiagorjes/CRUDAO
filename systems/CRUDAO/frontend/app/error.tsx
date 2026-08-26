"use client";

/**
 * Error boundary raiz — cobre falhas em `app/(shell)/layout.tsx` (ex.: `GET /api/me`
 * indisponível), que `(shell)/error.tsx` sozinho não captura (limitação do App Router: um
 * error.tsx não intercepta erros lançados no layout do próprio segmento, só nos filhos).
 */
export default function RootError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <main className="login-shell">
      <div className="card login-card" role="alert">
        <h1>Algo deu errado</h1>
        <p className="text-secondary">Não foi possível carregar a aplicação.</p>
        <button className="btn btn-primary full" type="button" onClick={reset}>
          Tentar novamente
        </button>
      </div>
    </main>
  );
}
