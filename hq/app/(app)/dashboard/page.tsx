'use client';

import { useHQStore } from '@/lib/store';
import { priorityColor, statusColor, statusLabel, formatDate, daysUntil, getInitials, formatCurrency } from '@/lib/utils';
import { AreaChart, Area, ResponsiveContainer, Tooltip } from 'recharts';
import Link from 'next/link';
import {
  FolderKanban,
  CheckSquare,
  Building2,
  TrendingUp,
  FlaskConical,
  CalendarDays,
  Users,
  Clock,
  ArrowRight,
} from 'lucide-react';

function StatWidget({ icon: Icon, label, value, sub, accent, link }: {
  icon?: React.ElementType; label: string; value: string | number;
  sub?: string; accent?: string; link?: string;
}) {
  const content = (
    <div className="card stat-card" style={{ gap: '0.875rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div className="stat-label">{label}</div>
          <div className="stat-value" style={{ color: accent || 'var(--text-primary)' }}>{value}</div>
          {sub && <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 4 }}>{sub}</div>}
        </div>
        {Icon && (
          <div
            style={{
              padding: '0.5rem',
              borderRadius: 'var(--radius-sm)',
              background: 'var(--accent-muted)',
              color: 'var(--accent)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Icon size={18} />
          </div>
        )}
      </div>
    </div>
  );
  return link ? <Link href={link} style={{ textDecoration: 'none' }}>{content}</Link> : content;
}

export default function DashboardPage() {
  const { projects, tasks, team, meetings, leads, investors, currentUser } = useHQStore();

  const now = new Date();
  const today = now.toISOString().split('T')[0];
  const weekEnd = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

  const activeProjects = projects.filter(p => p.status === 'in_progress');
  const atRiskProjects = projects.filter(p => {
    const days = daysUntil(p.targetDate);
    return p.status === 'in_progress' && (days <= 14 && p.progress < 70);
  });
  const inProgressTasks = tasks.filter(t => t.status === 'in_progress');
  const blockedTasks = tasks.filter(t => t.status === 'blocked');
  const dueTasks = tasks.filter(t => t.dueDate <= weekEnd && t.status !== 'done');

  // Generate task velocity data (last 8 weeks) based on actual completed tasks
  const progressData = Array.from({ length: 8 }).map((_, i) => {
    const weekIndex = 7 - i;
    const startOffset = new Date(now.getTime() - (weekIndex * 7 + 7) * 24 * 60 * 60 * 1000);
    const endOffset = new Date(now.getTime() - (weekIndex * 7) * 24 * 60 * 60 * 1000);

    const count = tasks.filter(t =>
      t.status === 'done' &&
      new Date(t.updatedAt) >= startOffset &&
      new Date(t.updatedAt) < endOffset
    ).length;

    return { w: `W${i + 1}`, v: count };
  });

  // Generate R&D Progress from actual R&D projects
  const rdProgressData = projects
    .filter(p => p.category === 'rd')
    .slice(0, 3)
    .map(p => ({
      label: p.name,
      progress: p.progress,
      stage: p.status === 'done' ? 'Completed' : (p.status === 'in_progress' ? 'In Progress' : 'Planned'),
      color: p.progress >= 75 ? '#10b981' : (p.progress >= 40 ? '#073988' : '#f59e0b')
    }));
  const upcomingMeetings = meetings.filter(m => m.date >= today).sort((a, b) => a.date.localeCompare(b.date)).slice(0, 3);
  const totalLeads = leads.length;
  const pilotLeads = leads.filter(l => l.stage === 'pilot' || l.stage === 'meeting').length;
  const activeInvestors = investors.filter(i => !['pass', 'closed'].includes(i.stage)).length;

  const hour = now.getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

  return (
    <div className="page animate-fade-in">
      {/* Welcome */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.25rem', letterSpacing: '0.02em', fontWeight: 500 }}>
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, letterSpacing: '-0.04em', color: 'var(--text-primary)' }}>
            {greeting}, {currentUser?.name?.split(' ')[0]}
          </h1>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.25rem', fontSize: '0.875rem' }}>
            Here&apos;s everything happening at IgnitionAI today.
          </p>
        </div>
      </div>

      {/* Top stats */}
      <div className="grid-4">
        <StatWidget icon={FolderKanban} label="Active Projects" value={activeProjects.length} sub={`${atRiskProjects.length} at risk`} accent="var(--text-primary)" link="/projects" />
        <StatWidget icon={CheckSquare} label="Tasks In Progress" value={inProgressTasks.length} sub={`${blockedTasks.length} blocked`} accent="var(--text-primary)" link="/tasks" />
        <StatWidget icon={Building2} label="CRM Leads" value={totalLeads} sub={`${pilotLeads} in pilot/meetings`} accent="var(--text-primary)" link="/crm" />
        <StatWidget icon={TrendingUp} label="Active Investors" value={activeInvestors} sub="Pre-seed round" accent="var(--text-primary)" link="/fundraising" />
      </div>

      {/* Main grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '1rem' }}>

        {/* Left column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

          {/* Projects */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <FolderKanban size={16} style={{ color: 'var(--accent)' }} />
                <span style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>Active Projects</span>
              </div>
              <Link href="/projects" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: 'var(--accent)', textDecoration: 'none', fontWeight: 600 }}>
                View all <ArrowRight size={13} />
              </Link>
            </div>
            <div style={{ padding: '0.5rem' }}>
              {activeProjects.length === 0 ? (
                <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                  No active projects currently in progress.
                </div>
              ) : (
                activeProjects.map(p => (
                  <Link key={p.id} href={`/projects/${p.id}`} style={{ textDecoration: 'none' }}>
                    <div
                      style={{
                        padding: '0.75rem 0.875rem',
                        borderRadius: 8,
                        display: 'flex',
                        alignItems: 'center',
                        gap: '1rem',
                        transition: 'background 0.15s ease',
                        cursor: 'pointer',
                      }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <div style={{
                        width: 8, height: 8, borderRadius: '50%', flexShrink: 0,
                        background: p.priority === 'critical' ? 'var(--danger)' : p.priority === 'high' ? 'var(--warning)' : 'var(--success)',
                      }} />
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: 600, fontSize: '0.875rem', marginBottom: '0.375rem', color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {p.name}
                        </div>
                        <div className="progress-bar" style={{ height: 4 }}>
                          <div
                            className={`progress-fill${p.progress >= 70 ? ' success' : p.progress >= 40 ? '' : ' warning'}`}
                            style={{ width: `${p.progress}%` }}
                          />
                        </div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexShrink: 0 }}>
                        <span style={{ fontSize: '0.8rem', fontWeight: 700, color: p.progress >= 70 ? 'var(--success)' : 'var(--text-primary)' }}>
                          {p.progress}%
                        </span>
                        <span style={{ fontSize: '0.7rem', color: daysUntil(p.targetDate) <= 0 ? 'var(--danger)' : 'var(--text-muted)', fontWeight: daysUntil(p.targetDate) <= 0 ? 600 : 400 }}>
                          {daysUntil(p.targetDate) > 0 ? `${daysUntil(p.targetDate)}d left` : 'Overdue'}
                        </span>
                      </div>
                    </div>
                  </Link>
                ))
              )}
            </div>
          </div>

          {/* Tasks */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <CheckSquare size={16} style={{ color: 'var(--accent)' }} />
                <span style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>Priority Tasks</span>
              </div>
              <Link href="/tasks" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: 'var(--accent)', textDecoration: 'none', fontWeight: 600 }}>
                View all <ArrowRight size={13} />
              </Link>
            </div>
            <div style={{ padding: '0.5rem' }}>
              {tasks.filter(t => t.status !== 'done' && (t.status === 'blocked' || t.priority === 'critical' || t.priority === 'high')).slice(0, 5).map(t => {
                const member = team.find(m => m.id === t.assigneeId);
                const isOverdue = daysUntil(t.dueDate) <= 0;
                return (
                  <div
                    key={t.id}
                    style={{
                      padding: '0.75rem 0.875rem',
                      borderRadius: 8,
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.875rem',
                      transition: 'background 0.15s ease',
                    }}
                    onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                    onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                  >
                    <div style={{
                      width: 7, height: 7, borderRadius: '50%', flexShrink: 0,
                      background: t.status === 'blocked' ? 'var(--danger)' : t.priority === 'critical' ? 'var(--danger)' : 'var(--warning)',
                    }} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 500, fontSize: '0.85rem', color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {t.title}
                      </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexShrink: 0 }}>
                      {t.status === 'blocked' && (
                        <span className="badge" style={{ background: 'var(--danger-bg)', color: 'var(--danger)', border: '1px solid rgba(239, 68, 68, 0.25)' }}>
                          Blocked
                        </span>
                      )}
                      <span style={{ fontSize: '0.72rem', color: isOverdue ? 'var(--danger)' : 'var(--text-muted)', fontWeight: isOverdue ? 600 : 400 }}>
                        {isOverdue ? 'Overdue' : `${daysUntil(t.dueDate)}d`}
                      </span>
                      {member && (
                        <div
                          className="avatar"
                          style={{
                            width: 22, height: 22, fontSize: '0.55rem',
                            background: 'var(--accent)', color: '#ffffff',
                            fontWeight: 700, borderRadius: '50%',
                          }}
                          title={member.name}
                        >
                          {getInitials(member.name)}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* R&D Progress */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <FlaskConical size={16} style={{ color: 'var(--accent)' }} />
                <span style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-primary)' }}>R&D Progress</span>
              </div>
              <Link href="/rd" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: 'var(--accent)', textDecoration: 'none', fontWeight: 600 }}>
                View lab <ArrowRight size={13} />
              </Link>
            </div>
            <div style={{ padding: '1rem 1.25rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {rdProgressData.length > 0 ? (
                rdProgressData.map(item => (
                  <div key={item.label}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)' }}>{item.label}</span>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{item.stage}</span>
                        <span style={{ fontSize: '0.8rem', fontWeight: 700, color: item.color }}>{item.progress}%</span>
                      </div>
                    </div>
                    <div className="progress-bar">
                      <div className="progress-fill" style={{ width: `${item.progress}%`, background: item.color }} />
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                  No active R&D projects found.
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

          {/* Company Pulse Chart */}
          <div className="card" style={{ padding: '1.25rem' }}>
            <div style={{ marginBottom: '0.875rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-primary)' }}>Task Velocity</span>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Last 8 weeks</span>
            </div>
            <ResponsiveContainer width="100%" height={80}>
              <AreaChart data={progressData}>
                <defs>
                  <linearGradient id="vg" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#073988" stopOpacity={0.25} />
                    <stop offset="95%" stopColor="#073988" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <Tooltip
                  contentStyle={{ background: 'var(--bg-surface)', border: '1px solid var(--border)', borderRadius: 8, fontSize: '0.75rem', color: 'var(--text-primary)' }}
                  cursor={{ stroke: 'var(--border)' }}
                />
                <Area type="monotone" dataKey="v" stroke="#073988" strokeWidth={2} fill="url(#vg)" dot={false} />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Meetings */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <CalendarDays size={16} style={{ color: 'var(--accent)' }} />
                <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-primary)' }}>Upcoming</span>
              </div>
              <Link href="/meetings" style={{ fontSize: '0.75rem', color: 'var(--accent)', textDecoration: 'none', fontWeight: 600 }}>All</Link>
            </div>
            <div style={{ padding: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
              {upcomingMeetings.length === 0 && (
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', padding: '0.5rem', textAlign: 'center' }}>No upcoming meetings</p>
              )}
              {upcomingMeetings.map(m => (
                <div key={m.id} style={{
                  padding: '0.75rem',
                  borderRadius: 8,
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                }}>
                  <div style={{ fontWeight: 600, fontSize: '0.8rem', marginBottom: '0.25rem', color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {m.title}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Clock size={12} style={{ color: 'var(--text-muted)' }} />
                    <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                      {formatDate(m.date)} · {m.time}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Investors */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <TrendingUp size={16} style={{ color: 'var(--accent)' }} />
                <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-primary)' }}>Fundraising</span>
              </div>
              <Link href="/fundraising" style={{ fontSize: '0.75rem', color: 'var(--accent)', textDecoration: 'none', fontWeight: 600 }}>All</Link>
            </div>
            <div style={{ padding: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
              {investors.slice(0, 4).map(inv => {
                const stageColors: Record<string, { bg: string; color: string; border: string }> = {
                  first_meeting: { bg: 'rgba(7, 57, 136, 0.08)', color: '#073988', border: 'rgba(7, 57, 136, 0.2)' },
                  application: { bg: 'rgba(245, 158, 11, 0.1)', color: '#d97706', border: 'rgba(245, 158, 11, 0.25)' },
                  intro: { bg: 'rgba(2, 132, 199, 0.1)', color: '#0284c7', border: 'rgba(2, 132, 199, 0.25)' },
                  partner_meeting: { bg: 'rgba(16, 185, 129, 0.1)', color: '#059669', border: 'rgba(16, 185, 129, 0.25)' },
                };
                const stageLabels: Record<string, string> = {
                  first_meeting: 'Meeting', application: 'Application', intro: 'Intro', partner_meeting: 'Partner Mtg',
                };
                const styling = stageColors[inv.stage] || { bg: 'var(--bg-elevated)', color: 'var(--text-secondary)', border: 'var(--border)' };
                return (
                  <div key={inv.id} style={{
                    padding: '0.625rem 0.75rem',
                    borderRadius: 8,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '0.5rem',
                  }}>
                    <span style={{ fontWeight: 600, fontSize: '0.8rem', color: 'var(--text-primary)', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {inv.name}
                    </span>
                    <span className="badge" style={{
                      background: styling.bg,
                      color: styling.color,
                      border: `1px solid ${styling.border}`,
                    }}>
                      {stageLabels[inv.stage] || inv.stage}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Team */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <Users size={16} style={{ color: 'var(--accent)' }} />
                <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-primary)' }}>Team ({team.length})</span>
              </div>
            </div>
            <div style={{ padding: '0.875rem 1.25rem', display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
              {team.slice(0, 4).map(m => (
                <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div className="avatar" style={{ background: 'var(--accent)', color: '#ffffff', width: 28, height: 28, fontSize: '0.65rem', fontWeight: 700, borderRadius: '50%' }}>
                    {getInitials(m.name)}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 600, fontSize: '0.8rem', color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {m.name}
                    </div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{m.activeTasks} active tasks</div>
                  </div>
                  {m.overdueTasks > 0 && (
                    <span style={{ fontSize: '0.7rem', color: 'var(--danger)', fontWeight: 600 }}>
                      {m.overdueTasks} overdue
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
