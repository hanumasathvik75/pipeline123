'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import Sidebar from '@/components/layout/Sidebar';
import TopBar from '@/components/layout/TopBar';

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isAuthenticated, sidebarCollapsed, syncWithServer } = useHQStore();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (mounted && !isAuthenticated) {
      router.replace('/login');
      return;
    }

    // Initial sync from central server
    if (mounted && isAuthenticated) {
      syncWithServer();
    }
  }, [mounted, isAuthenticated, router, syncWithServer]);

  if (!mounted || !isAuthenticated) return null;

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
