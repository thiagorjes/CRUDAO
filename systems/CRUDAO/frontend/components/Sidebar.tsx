/**
 * Navegação global do shell (RF-014). Só reflete visibilidade de UX — a validação real de
 * permissão permanece sempre no backend (RNF-003).
 */
export function Sidebar({ mostrarAdmin }: { mostrarAdmin: boolean }) {
  return (
    <aside className="sidebar" aria-label="Navegação principal">
      <div className="sidebar__brand">Kanban</div>
      <nav aria-label="Menu">
        <a href="/projetos" aria-current="page">
          Projetos
        </a>
        {mostrarAdmin && <a href="/admin">Admin</a>}
      </nav>
    </aside>
  );
}
