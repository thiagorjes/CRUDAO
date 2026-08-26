import { iniciais } from "@/lib/format";

/** Notificações (RF-005) ficam para TASK-07.7 — topbar hoje só identifica o usuário e faz logout. */
export function Topbar({ nome }: { nome: string }) {
  return (
    <header className="topbar">
      <div className="topbar__user">
        <span className="avatar">{iniciais(nome)}</span>
        {nome}
      </div>
      <form action="/api/auth/logout" method="post">
        <button className="topbar__logout" type="submit">
          Sair
        </button>
      </form>
    </header>
  );
}
