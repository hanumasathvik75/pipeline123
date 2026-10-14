'use client';

import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import { getInitials } from '@/lib/utils';
import {
  LayoutDashboard,
  FolderKanban,
  CheckSquare,
  Zap,
  Building2,
  TrendingUp,
  CalendarDays,
  ClipboardList,
  FlaskConical,
  Users,
  MessageSquare,
  Target,
  BookOpen,
  DollarSign,
  ChevronLeft,
  ChevronRight,
  LogOut,
  KeyRound,
  X,
} from 'lucide-react';

interface NavItem {
  href: string;
  label: string;
  icon: React.ElementType;
}

interface NavSection {
  label: string;
  items: NavItem[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    label: 'Core',
    items: [
      { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
      { href: '/projects', label: 'Projects', icon: FolderKanban },
      { href: '/tasks', label: 'Tasks', icon: CheckSquare },
      { href: '/progress', label: 'Progress Logger', icon: Zap },
    ],
  },
  {
    label: 'Business',
    items: [
      { href: '/crm', label: 'CRM', icon: Building2 },
      { href: '/fundraising', label: 'Fundraising', icon: TrendingUp },
      { href: '/meetings', label: 'Meetings', icon: CalendarDays },
      { href: '/registrations', label: 'Registrations', icon: ClipboardList },
    ],
  },
  {
    label: 'R&D',
    items: [
      { href: '/rd', label: 'Research & Labs', icon: FlaskConical },
    ],
  },
  {
    label: 'People',
    items: [
      { href: '/team', label: 'Team', icon: Users },
      { href: '/chat', label: 'Chat', icon: MessageSquare },
    ],
  },
  {
    label: 'Strategy',
    items: [
      { href: '/goals', label: 'Goals & OKRs', icon: Target },
      { href: '/wiki', label: 'Knowledge Base', icon: BookOpen },
      { href: '/finance', label: 'Finance', icon: DollarSign },
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
    }),
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
    <aside
      className={`sidebar${collapsed ? ' collapsed' : ''}`}
      style={{
        width: collapsed ? 'var(--sidebar-collapsed)' : 'var(--sidebar-width)',
        transition: 'width 0.2s ease',
      }}
    >
      {/* Brand Header */}
      <div
        style={{
          padding: collapsed ? '0 0.5rem' : '0 1.25rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: collapsed ? 'center' : 'space-between',
          borderBottom: '1px solid var(--border)',
          height: 'var(--topbar-height)',
          flexShrink: 0,
        }}
      >
        <Link
          href="/dashboard"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            height: '100%',
            textDecoration: 'none',
          }}
          title="IgnitionAI Dashboard"
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/logo.png"
            alt="IgnitionAI"
            style={{
              height: collapsed ? '28px' : '36px',
              maxWidth: collapsed ? '36px' : '140px',
              width: 'auto',
              objectFit: 'contain',
              display: 'block',
            }}
          />
        </Link>
        <button
          className="btn btn-ghost btn-icon mobile-only"
          onClick={useHQStore.getState().closeMobileMenu}
          style={{ padding: '0.25rem', color: 'var(--text-secondary)' }}
          aria-label="Close menu"
        >
          <X size={18} />
        </button>
      </div>

      {/* Nav List */}
      <nav
        style={{
          flex: 1,
          overflowY: 'auto',
          overflowX: 'hidden',
          padding: collapsed ? '0.75rem 0.375rem' : '0.75rem 0.625rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.25rem',
        }}
      >
        {filteredNavSections.map((section, idx) => (
          <div key={section.label} style={{ marginBottom: collapsed ? '0.375rem' : '0.625rem' }}>
            {!collapsed ? (
              <div
                style={{
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  color: 'var(--text-muted)',
                  letterSpacing: '0.08em',
                  textTransform: 'uppercase',
                  padding: '0.375rem 0.75rem 0.25rem',
                }}
              >
                {section.label}
              </div>
            ) : idx > 0 ? (
              <div
                style={{
                  height: 1,
                  background: 'var(--border)',
                  margin: '0.375rem 0.5rem',
                  opacity: 0.6,
                }}
              />
            ) : null}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.125rem' }}>
              {section.items.map(item => {
                const Icon = item.icon;
                const active = pathname === item.href || (item.href !== '/dashboard' && pathname?.startsWith(item.href + '/'));
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={`nav-item${active ? ' active' : ''}`}
                    title={item.label}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: collapsed ? 'center' : 'flex-start',
                      gap: '0.75rem',
                      padding: collapsed ? '0.55rem 0' : '0.5rem 0.75rem',
                      width: collapsed ? 44 : '100%',
                      margin: collapsed ? '0 auto' : undefined,
                      borderRadius: 'var(--radius-sm)',
                      textDecoration: 'none',
                    }}
                  >
                    <Icon
                      size={18}
                      className="nav-icon"
                      style={{
                        flexShrink: 0,
                        color: active ? '#ffffff' : 'currentColor',
                      }}
                    />
                    {!collapsed && (
                      <span
                        style={{
                          fontSize: '0.875rem',
                          fontWeight: active ? 600 : 500,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {item.label}
                      </span>
                    )}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* User Info + Footer Actions */}
      <div
        style={{
          borderTop: '1px solid var(--border)',
          padding: collapsed ? '0.75rem 0.375rem' : '0.75rem 0.625rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.25rem',
          background: 'var(--bg-surface)',
        }}
      >
        {currentUser && (
          <div
            className="nav-item user-profile-item"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: collapsed ? 'center' : 'flex-start',
              gap: '0.625rem',
              padding: collapsed ? '0.375rem 0' : '0.5rem 0.625rem',
              cursor: 'default',
              borderRadius: 'var(--radius-sm)',
            }}
            title={collapsed ? `${currentUser.name} (${currentUser.department})` : undefined}
          >
            <div
              className="avatar"
              style={{
                background: 'var(--accent)',
                color: '#ffffff',
                flexShrink: 0,
                width: 28,
                height: 28,
                fontSize: '0.7rem',
                fontWeight: 700,
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {getInitials(currentUser.name)}
            </div>
            {!collapsed && (
              <div style={{ overflow: 'hidden', flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    color: 'var(--text-primary)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {currentUser.name}
                </div>
                <div
                  style={{
                    fontSize: '0.7rem',
                    color: 'var(--text-muted)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                    textTransform: 'capitalize',
                  }}
                >
                  {currentUser.department || currentUser.role}
                </div>
              </div>
            )}
          </div>
        )}

        <button
          onClick={() => setShowPasswordModal(true)}
          className="nav-item btn-ghost"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            gap: '0.75rem',
            padding: collapsed ? '0.55rem 0' : '0.5rem 0.75rem',
            width: collapsed ? 44 : '100%',
            margin: collapsed ? '0 auto' : undefined,
            border: 'none',
            borderRadius: 'var(--radius-sm)',
            cursor: 'pointer',
          }}
          title={collapsed ? 'Change Password' : undefined}
        >
          <KeyRound size={17} style={{ flexShrink: 0, color: 'var(--text-muted)' }} />
          {!collapsed && (
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              Change Password
            </span>
          )}
        </button>

        <button
          onClick={logout}
          className="nav-item btn-ghost"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            gap: '0.75rem',
            padding: collapsed ? '0.55rem 0' : '0.5rem 0.75rem',
            width: collapsed ? 44 : '100%',
            margin: collapsed ? '0 auto' : undefined,
            border: 'none',
            borderRadius: 'var(--radius-sm)',
            cursor: 'pointer',
          }}
          title={collapsed ? 'Sign Out' : undefined}
        >
          <LogOut size={17} style={{ flexShrink: 0, color: 'var(--text-muted)' }} />
          {!collapsed && (
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              Sign Out
            </span>
          )}
        </button>

        <button
          onClick={toggleSidebar}
          className="nav-item btn-ghost"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            gap: '0.75rem',
            padding: collapsed ? '0.55rem 0' : '0.5rem 0.75rem',
            width: collapsed ? 44 : '100%',
            margin: collapsed ? '0 auto' : undefined,
            border: 'none',
            borderRadius: 'var(--radius-sm)',
            cursor: 'pointer',
          }}
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? (
            <ChevronRight size={17} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
          ) : (
            <>
              <ChevronLeft size={17} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                Collapse
              </span>
            </>
          )}
        </button>
      </div>

      {/* Change Password Modal */}
      {mounted && showPasswordModal && createPortal(
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div
            style={{ position: 'absolute', inset: 0, background: 'rgba(15, 23, 42, 0.45)', backdropFilter: 'blur(4px)' }}
            onClick={() => !isSubmitting && setShowPasswordModal(false)}
          />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 400, padding: '2rem', zIndex: 1 }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>Change Password</h2>

            {feedback && (
              <div
                style={{
                  marginBottom: '1.5rem',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.85rem',
                  background: feedback.type === 'error' ? 'var(--danger-bg)' : 'var(--success-bg)',
                  color: feedback.type === 'error' ? 'var(--danger)' : 'var(--success)',
                  border: `1px solid ${feedback.type === 'error' ? 'var(--danger)' : 'var(--success)'}`,
                }}
              >
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
                <button type="button" onClick={() => setShowPasswordModal(false)} className="btn btn-secondary" style={{ flex: 1 }} disabled={isSubmitting}>Cancel</button>
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
