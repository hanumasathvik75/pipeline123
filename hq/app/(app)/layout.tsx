'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import Sidebar from '@/components/layout/Sidebar';
import TopBar from '@/components/layout/TopBar';

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isAuthenticated, sidebarCollapsed, syncWithServer } = useHQStore();

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace('/login');
      return;
    }

    // Initial sync from central server
    syncWithServer();

    // Polling interval for live multi-user shared updates
    const interval = setInterval(() => {
      syncWithServer();
    }, 4000);

    return () => clearInterval(interval);
  }, [isAuthenticated, router, syncWithServer]);

  if (!isAuthenticated) return null;

  return (
    <div className={`app-shell${sidebarCollapsed ? ' collapsed' : ''}`}>
      <Sidebar />
      <TopBar />
      <main className="main-content">
        {children}
      </main>
    </div>
  );
}
