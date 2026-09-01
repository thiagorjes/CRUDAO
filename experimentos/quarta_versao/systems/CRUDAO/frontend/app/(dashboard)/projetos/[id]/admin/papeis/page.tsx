/**
 * TL-09 — Papéis e Permissões.
 *
 * O backend não tem nenhum endpoint para papéis/permissões do projeto (listar, criar, toggle de
 * permissão por papel) — não é um problema de tela, é RBAC ainda não desenhado/implementado no
 * servidor (quem pode alterar o quê, proteção do papel `admin`, RN-017 de autoproteção). Ver
 * docs/checklists/kanban-tarefas-ui-fidelidade-review.md.
 */
export default function PapeisAdminPage() {
  return (
    <section aria-label="Permissões por papel">
      <div className="empty-state">
        Gestão de papéis e permissões ainda não está disponível — depende de um backend de RBAC
        (listar papéis/permissões do projeto, alternar permissão por papel) que ainda não existe.
      </div>
    </section>
  );
}
