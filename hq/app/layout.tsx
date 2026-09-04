import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'IgnitionAI HQ — Founder Operating System',
  description: 'Internal command center for IgnitionAI — projects, tasks, R&D, CRM, fundraising, and team management.',
  icons: {
    icon: '/logo.png',
    apple: '/logo.png',
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
