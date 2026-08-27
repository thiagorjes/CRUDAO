import type { Metadata } from 'next';
import { Roboto } from 'next/font/google';
import { ToastHost } from '@/components/ui/toast';
import './globals.css';

const roboto = Roboto({
  variable: '--font-roboto',
  subsets: ['latin'],
  weight: ['400', '500', '700'],
});

export const metadata: Metadata = {
  title: 'Kanban Configurável — CRUDAO',
  description: 'Board kanban com workflows e etapas configuráveis por projeto.',
};

export default function RootLayout({ children }: LayoutProps<'/'>) {
  return (
    <html lang="pt-BR" className={roboto.variable}>
      <body>
        {children}
        <ToastHost />
      </body>
    </html>
  );
}
