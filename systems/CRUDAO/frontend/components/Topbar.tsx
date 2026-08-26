import { iniciais } from "@/lib/format";
import { NotificacoesBell } from "@/components/notificacoes/NotificacoesBell";

export function Topbar({
  nome,
  usuarioId,
  backendPublicUrl,
}: {
  nome: string;
  usuarioId: string;
  backendPublicUrl: string;
}) {
  return (
    <header className="topbar">
      <NotificacoesBell usuarioId={usuarioId} backendPublicUrl={backendPublicUrl} />
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
