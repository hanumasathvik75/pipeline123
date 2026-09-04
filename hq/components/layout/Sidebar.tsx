'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import { getInitials, avatarColor } from '@/lib/utils';
import {
  Zap, LayoutDashboard, FolderKanban, CheckSquare, Users,
  CalendarDays, Building2, FlaskConical, TrendingUp, BookOpen,
  Target, DollarSign, ChevronLeft, ChevronRight,
  LogOut, Settings,
} from 'lucide-react';

const NAV_SECTIONS = [
  {
    label: 'Core',
    items: [
      { href: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
      { href: '/projects', icon: FolderKanban, label: 'Projects' },
      { href: '/tasks', icon: CheckSquare, label: 'Tasks' },
    ],
  },
  {
    label: 'Business',
    items: [
      { href: '/crm', icon: Building2, label: 'CRM' },
      { href: '/fundraising', icon: TrendingUp, label: 'Fundraising' },
      { href: '/meetings', icon: CalendarDays, label: 'Meetings' },
    ],
  },
  {
    label: 'R&D',
    items: [
      { href: '/rd', icon: FlaskConical, label: 'R&D' },
    ],
  },
  {
    label: 'People',
    items: [
      { href: '/team', icon: Users, label: 'Team' },
    ],
  },
  {
    label: 'Strategy',
    items: [
      { href: '/goals', icon: Target, label: 'Goals & OKRs' },
      { href: '/wiki', icon: BookOpen, label: 'Knowledge Base' },
      { href: '/finance', icon: DollarSign, label: 'Finance' },
    ],
  },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { sidebarCollapsed, toggleSidebar, currentUser, logout } = useHQStore();
  const collapsed = sidebarCollapsed;

  return (
    <aside className="sidebar" style={{ width: collapsed ? 'var(--sidebar-collapsed)' : 'var(--sidebar-width)' }}>
      {/* Logo */}
      <div style={{
        padding: collapsed ? '0 0.5rem' : '0 1.25rem',
        display: 'flex',
        alignItems: 'center',
        borderBottom: '1px solid var(--border)',
        height: 'var(--topbar-height)',
        flexShrink: 0,
        justifyContent: collapsed ? 'center' : 'flex-start',
      }}>
        <Link
          href="/dashboard"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            height: '100%',
            width: '100%',
          }}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/logo.png"
            alt="IgnitionAI"
            style={{
              height: collapsed ? '30px' : '46px',
              width: 'auto',
              objectFit: 'contain',
              display: 'block',
            }}
          />
        </Link>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, overflow: 'hidden auto', padding: collapsed ? '0.75rem 0.5rem' : '0.75rem 0.625rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
        {NAV_SECTIONS.map(section => (
          <div key={section.label} style={{ marginBottom: '0.5rem' }}>
            {!collapsed && (
              <div style={{
                fontSize: '0.65rem', fontWeight: 700, color: 'var(--text-muted)',
                textTransform: 'uppercase', letterSpacing: '0.1em',
                padding: '0.25rem 0.75rem',
                marginBottom: '0.125rem',
              }}>
                {section.label}
              </div>
            )}
            {section.items.map(item => {
              const Icon = item.icon;
              const active = pathname === item.href || pathname?.startsWith(item.href + '/');
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`nav-item${active ? ' active' : ''}`}
                  title={collapsed ? item.label : undefined}
                  style={{ justifyContent: collapsed ? 'center' : 'flex-start' }}
                >
                  <Icon size={16} className="nav-icon" style={{ flexShrink: 0 }} />
                  {!collapsed && <span>{item.label}</span>}
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      {/* User + collapse */}
      <div style={{ borderTop: '1px solid var(--border)', padding: collapsed ? '0.75rem 0.5rem' : '0.75rem 0.625rem', display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
        {currentUser && (
          <div
            className="nav-item"
            style={{ justifyContent: collapsed ? 'center' : 'flex-start', cursor: 'default' }}
            title={collapsed ? currentUser.name : undefined}
          >
            <div
              className="avatar"
              style={{ background: 'var(--text-primary)', color: 'var(--bg-surface)', flexShrink: 0, width: 24, height: 24, fontSize: '0.65rem' }}
            >
              {getInitials(currentUser.name)}
            </div>
            {!collapsed && (
              <div style={{ overflow: 'hidden' }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {currentUser.name}
                </div>
                <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {currentUser.department}
                </div>
              </div>
            )}
          </div>
        )}

        <button
          onClick={logout}
          className="nav-item btn-ghost"
          style={{ justifyContent: collapsed ? 'center' : 'flex-start', border: 'none', width: '100%' }}
          title={collapsed ? 'Sign Out' : undefined}
        >
          <LogOut size={14} style={{ flexShrink: 0, color: 'var(--text-muted)' }} />
          {!collapsed && <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Sign Out</span>}
        </button>

        <button
          onClick={toggleSidebar}
          className="nav-item btn-ghost"
          style={{ justifyContent: collapsed ? 'center' : 'flex-start', border: 'none', width: '100%' }}
          title={collapsed ? 'Expand' : 'Collapse'}
        >
          {collapsed
            ? <ChevronRight size={14} style={{ color: 'var(--text-muted)' }} />
            : <><ChevronLeft size={14} style={{ color: 'var(--text-muted)', flexShrink: 0 }} /><span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Collapse</span></>
          }
        </button>
      </div>
    </aside>
  );
}
