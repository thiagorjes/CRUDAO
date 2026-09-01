import { redirect } from "next/navigation";

/** /projetos/{id}/admin → aba padrão "Projeto". */
export default async function AdminIndexPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  redirect(`/projetos/${id}/admin/projeto`);
}
