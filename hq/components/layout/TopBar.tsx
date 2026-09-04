'use client';

import { useState } from 'react';
import { usePathname } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import { Search, Bell, Plus, ChevronDown } from 'lucide-react';
import { getInitials } from '@/lib/utils';

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'Command Center',
  '/projects': 'Projects',
  '/tasks': 'Tasks',
  '/crm': 'Business CRM',
  '/fundraising': 'Fundraising',
  '/meetings': 'Meetings',
  '/rd': 'R&D Workspace',
  '/team': 'Team',
  '/goals': 'Goals & OKRs',
  '/wiki': 'Knowledge Base',
  '/finance': 'Finance',
};

export default function TopBar() {
  const pathname = usePathname();
  const { currentUser } = useHQStore();
  const [search, setSearch] = useState('');
  const [hasNotif] = useState(true);

  const title = PAGE_TITLES[pathname || ''] ?? 'IgnitionAI HQ';

  return (
    <header className="topbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1 }}>
        <h2 style={{ fontSize: '0.95rem', fontWeight: 700, letterSpacing: '-0.02em', whiteSpace: 'nowrap' }}>
          {title}
        </h2>
        <span style={{ color: 'var(--border-hover)', userSelect: 'none' }}>/</span>
        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>IgnitionAI HQ</span>
      </div>

      {/* Search */}
      <div style={{ position: 'relative', width: 240 }}>
        <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
        <input
          className="input"
          placeholder="Search anything…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }}
        />
        <kbd style={{
          position: 'absolute', right: '0.5rem', top: '50%', transform: 'translateY(-50%)',
          fontSize: '0.6rem', color: 'var(--text-muted)', background: 'var(--bg-hover)',
          border: '1px solid var(--border)', borderRadius: 4, padding: '0 4px', lineHeight: '18px',
        }}>⌘K</kbd>
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <button
          className="btn btn-primary btn-sm"
          style={{ gap: '0.375rem', fontSize: '0.8rem', height: 34, padding: '0 0.875rem' }}
        >
          <Plus size={14} />
          New
        </button>

        <button
          className="btn btn-ghost btn-icon"
          style={{ position: 'relative', height: 34, width: 34 }}
        >
          <Bell size={16} />
          {hasNotif && (
            <span style={{
              position: 'absolute', top: 6, right: 6,
              width: 6, height: 6, borderRadius: '50%',
              background: '#ef4444', border: '1.5px solid var(--bg-surface)',
            }} />
          )}
        </button>

        {currentUser && (
          <button
            className="btn btn-ghost"
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.25rem 0.5rem', height: 34, fontSize: '0.8rem' }}
          >
            <div
              className="avatar"
              style={{ width: 24, height: 24, fontSize: '0.6rem', background: 'linear-gradient(135deg, #6366f1, #818cf8)' }}
            >
              {getInitials(currentUser.name)}
            </div>
            <span style={{ fontWeight: 600 }}>{currentUser.name}</span>
            <ChevronDown size={12} style={{ color: 'var(--text-muted)' }} />
          </button>
        )}
      </div>
    </header>
  );
}
