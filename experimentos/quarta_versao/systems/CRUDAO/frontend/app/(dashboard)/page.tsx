import { redirect } from "next/navigation";

export default function Home() {
  // Redireciona sempre para /projetos
  redirect("/projetos");
}
