import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Kanban de Tarefas",
  description: "CRUDAO — Kanban de Tarefas",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
