/** TL-01 — Login (docs/design/kanban-tarefas/prototypes/tl-01-login.html). */
export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ erro?: string }>;
}) {
  const { erro } = await searchParams;

  return (
    <main className="login-shell">
      <section className="card login-card" aria-label="Login">
        <h1>Kanban de Tarefas</h1>
        <p className="text-secondary">
          Autentique-se com sua conta corporativa (Keycloak) para continuar.
        </p>

        {erro && (
          <div className="toast toast-error" role="alert" style={{ marginBottom: "16px" }}>
            Não foi possível autenticar. Verifique suas credenciais no Keycloak e tente novamente.
          </div>
        )}

        <a className="btn btn-primary full" href="/api/auth/login">
          Entrar com Keycloak
        </a>
      </section>
    </main>
  );
}
