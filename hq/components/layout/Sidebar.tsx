'use client';

import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import { getInitials, avatarColor } from '@/lib/utils';
import {
  Zap, LayoutDashboard, FolderKanban, CheckSquare, Users,
  CalendarDays, Building2, FlaskConical, TrendingUp, BookOpen,
  Target, DollarSign, ChevronLeft, ChevronRight,
  LogOut, Settings, X, MessageSquare,
} from 'lucide-react';

const NAV_SECTIONS = [
  {
    label: 'Core',
    items: [
      { href: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
      { href: '/projects', icon: FolderKanban, label: 'Projects' },
      { href: '/tasks', icon: CheckSquare, label: 'Tasks' },
      { href: '/progress', icon: Zap, label: 'Progress Logger' },
    ],
  },
  {
    label: 'Business',
    items: [
      { href: '/crm', icon: Building2, label: 'CRM' },
      { href: '/fundraising', icon: TrendingUp, label: 'Fundraising' },
      { href: '/meetings', icon: CalendarDays, label: 'Meetings' },
      { href: '/registrations', icon: Target, label: 'Registrations' },
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
      { href: '/chat', icon: MessageSquare, label: 'Chat' },
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
  const { sidebarCollapsed, toggleSidebar, currentUser, logout, changePassword } = useHQStore();
  const collapsed = sidebarCollapsed;

  const filteredNavSections = NAV_SECTIONS.map(section => ({
    ...section,
    items: section.items.filter(item => {
      if (currentUser?.role === 'intern') {
        const restricted = ['/finance', '/fundraising'];
        if (restricted.includes(item.href)) return false;
      }
      return true;
    })
  })).filter(section => section.items.length > 0);

  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword) {
      setFeedback({ message: 'Both fields are required', type: 'error' });
      return;
    }
    
    setIsSubmitting(true);
    setFeedback(null);
    const res = await changePassword(currentPassword, newPassword);
    setIsSubmitting(false);

    if (res.success) {
      setFeedback({ message: 'Password updated successfully!', type: 'success' });
      setTimeout(() => {
        setShowPasswordModal(false);
        setCurrentPassword('');
        setNewPassword('');
        setFeedback(null);
      }, 2000);
    } else {
      setFeedback({ message: res.error || 'Failed to update password', type: 'error' });
    }
  };

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
        <button 
          className="btn btn-ghost btn-icon mobile-only" 
          onClick={useHQStore.getState().closeMobileMenu}
          style={{ padding: '0.25rem' }}
        >
          <X size={20} />
        </button>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, overflow: 'hidden auto', padding: collapsed ? '0.75rem 0.5rem' : '0.75rem 0.625rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
        {filteredNavSections.map((section, idx) => (
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
          onClick={() => setShowPasswordModal(true)}
          className="nav-item btn-ghost"
          style={{ justifyContent: collapsed ? 'center' : 'flex-start', border: 'none', width: '100%' }}
          title={collapsed ? 'Settings' : undefined}
        >
          <Settings size={14} style={{ flexShrink: 0, color: 'var(--text-muted)' }} />
          {!collapsed && <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Change Password</span>}
        </button>

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

      {/* Change Password Modal */}
      {mounted && showPasswordModal && createPortal(
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)', backdropFilter: 'blur(4px)' }} onClick={() => !isSubmitting && setShowPasswordModal(false)} />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 400, padding: '2rem', zIndex: 1 }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1.5rem' }}>Change Password</h2>
            
            {feedback && (
              <div style={{ marginBottom: '1.5rem', padding: '0.75rem', borderRadius: 8, fontSize: '0.8rem', background: feedback.type === 'error' ? 'rgba(239,68,68,0.1)' : 'rgba(16,185,129,0.1)', color: feedback.type === 'error' ? '#ef4444' : '#10b981' }}>
                {feedback.message}
              </div>
            )}

            <form onSubmit={handlePasswordChange} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Current Password</label>
                <input
                  className="input"
                  type="password"
                  value={currentPassword}
                  onChange={e => setCurrentPassword(e.target.value)}
                  placeholder="Enter current password"
                  required
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>New Password</label>
                <input
                  className="input"
                  type="password"
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  placeholder="Enter new password"
                  required
                />
              </div>
              <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem' }}>
                <button type="button" onClick={() => setShowPasswordModal(false)} className="btn btn-ghost" style={{ flex: 1 }} disabled={isSubmitting}>Cancel</button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={isSubmitting}>
                  {isSubmitting ? 'Updating...' : 'Update Password'}
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}
    </aside>
  );
}
