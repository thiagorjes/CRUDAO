import AdminLayoutClient from "@/components/admin/AdminLayoutClient";

interface AdminLayoutProps {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}

export default async function AdminLayout({ children, params }: AdminLayoutProps) {
  const { id } = await params;

  return <AdminLayoutClient projetoId={id}>{children}</AdminLayoutClient>;
}
