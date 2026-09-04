'use client';

import { useHQStore } from '@/lib/store';
import { getInitials, priorityColor, statusColor, statusLabel, daysUntil } from '@/lib/utils';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import {
  ArrowLeft, Mail, Calendar, Briefcase, CheckCircle2, AlertCircle,
  Award, Shield, FileText, Clock
} from 'lucide-react';

const ROLE_LABELS: Record<string, { label: string; color: string; bg: string }> = {
  founder: { label: 'Founder & CEO', color: '#818cf8', bg: 'rgba(99,102,241,0.12)' },
  coo: { label: 'Operations & BD Lead', color: '#34d399', bg: 'rgba(52,211,153,0.12)' },
  engineering: { label: 'Lead Engineer', color: '#38bdf8', bg: 'rgba(56,189,248,0.12)' },
  ml_rd: { label: 'Lead AI Scientist', color: '#c084fc', bg: 'rgba(192,132,252,0.12)' },
  bizdev: { label: 'Business Development', color: '#fbbf24', bg: 'rgba(251,191,36,0.12)' },
  intern: { label: 'ML / Telematics Intern', color: '#f472b6', bg: 'rgba(244,114,182,0.12)' },
};

export default function MemberProfilePage({ params }: { params: { id: string } }) {
  const { team, tasks, projects } = useHQStore();

  const member = team.find(m => m.id === params.id);
  if (!member) {
    return (
      <div className="page animate-fade-in" style={{ textAlign: 'center', padding: '3rem' }}>
        <p>Member not found.</p>
        <Link href="/team" className="btn btn-primary" style={{ marginTop: '1rem' }}>
          Back to Team
        </Link>
      </div>
    );
  }

  const roleMeta = ROLE_LABELS[member.role] || ROLE_LABELS.intern;
  const memberTasks = tasks.filter(t => t.assigneeId === member.id);
  const memberProjects = projects.filter(p => p.ownerId === member.id || p.memberIds.includes(member.id));

  return (
    <div className="page animate-fade-in">
      {/* Back button */}
      <div>
        <Link href="/team" className="btn btn-ghost btn-sm" style={{ paddingLeft: 0, gap: '0.4rem', color: 'var(--text-muted)' }}>
          <ArrowLeft size={14} />
          Back to Team Directory
        </Link>
      </div>

      {/* Member Header Card */}
      <div className="card" style={{ padding: '1.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: '50%',
                background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '1.5rem',
                fontWeight: 700,
                color: '#fff',
                flexShrink: 0,
              }}
            >
              {getInitials(member.name)}
            </div>

            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <h1 style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {member.name}
                </h1>
                <span
                  style={{
                    fontSize: '0.72rem',
                    fontWeight: 600,
                    padding: '0.2rem 0.6rem',
                    borderRadius: 4,
                    color: roleMeta.color,
                    background: roleMeta.bg,
                  }}
                >
                  {roleMeta.label}
                </span>
              </div>

              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: '#818cf8', marginTop: '0.25rem' }}>
                {member.designation || roleMeta.label}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '0.4rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                <span>{member.department}</span>
                <span>·</span>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                  <Mail size={13} />
                  {member.email}
                </span>
                <span>·</span>
                <span>Joined {member.joiningDate}</span>
              </div>
            </div>
          </div>

          {/* Quick task badges */}
          <div style={{ display: 'flex', gap: '1rem' }}>
            <div className="card" style={{ padding: '0.75rem 1.25rem', textAlign: 'center', background: 'var(--bg-elevated)' }}>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>{memberTasks.length}</div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Assigned Tasks</div>
            </div>
            <div className="card" style={{ padding: '0.75rem 1.25rem', textAlign: 'center', background: 'var(--bg-elevated)' }}>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, color: '#38bdf8' }}>{memberProjects.length}</div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Initiatives</div>
            </div>
            <div className="card" style={{ padding: '0.75rem 1.25rem', textAlign: 'center', background: 'var(--bg-elevated)' }}>
              <div style={{ fontSize: '1.2rem', fontWeight: 700, color: member.overdueTasks > 0 ? '#ef4444' : '#10b981' }}>{member.overdueTasks}</div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Overdue</div>
            </div>
          </div>
        </div>

        {/* Skills */}
        <div style={{ marginTop: '1.25rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border)' }}>
          <h4 style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>
            Core Technical Skills & Domains
          </h4>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
            {member.skills.map((skill, i) => (
              <span key={i} className="badge" style={{ padding: '0.25rem 0.6rem', fontSize: '0.72rem' }}>
                {skill}
              </span>
            ))}
          </div>
        </div>

        {/* Performance Notes */}
        {member.performanceNotes && (
          <div style={{ marginTop: '1.25rem', paddingTop: '1.25rem', borderTop: '1px solid var(--border)' }}>
            <h4 style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>
              Execution & Leadership Notes
            </h4>
            <div style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', background: 'var(--bg-elevated)', padding: '0.875rem', borderRadius: 'var(--radius)', lineHeight: 1.5 }}>
              {member.performanceNotes}
            </div>
          </div>
        )}
      </div>

      {/* Two columns: Projects and Tasks */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.3fr', gap: '1.5rem', alignItems: 'start' }}>
        {/* Projects */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '1rem' }}>
            Active Projects ({memberProjects.length})
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {memberProjects.map(proj => (
              <Link key={proj.id} href={`/projects/${proj.id}`} style={{ textDecoration: 'none' }}>
                <div
                  style={{
                    padding: '0.875rem',
                    borderRadius: 'var(--radius)',
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    cursor: 'pointer',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.35rem' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--text-primary)' }}>{proj.name}</div>
                    <span className={`badge ${statusColor(proj.status)}`} style={{ fontSize: '0.65rem' }}>
                      {statusLabel(proj.status)}
                    </span>
                  </div>
                  <p style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginBottom: '0.5rem', lineHeight: 1.3 }}>
                    {proj.description}
                  </p>
                  <div className="progress-bar" style={{ height: 4 }}>
                    <div className="progress-fill" style={{ width: `${proj.progress}%` }} />
                  </div>
                </div>
              </Link>
            ))}
            {memberProjects.length === 0 && (
              <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textAlign: 'center', padding: '1rem' }}>
                No active projects assigned
              </div>
            )}
          </div>
        </div>

        {/* Tasks */}
        <div className="card" style={{ padding: '1.25rem' }}>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '1rem' }}>
            Assigned Tasks ({memberTasks.length})
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
            {memberTasks.map(task => {
              const due = daysUntil(task.dueDate);

              return (
                <div
                  key={task.id}
                  style={{
                    padding: '0.875rem',
                    borderRadius: 'var(--radius)',
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.35rem',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span className={`badge ${priorityColor(task.priority)}`} style={{ fontSize: '0.65rem' }}>
                      {task.priority}
                    </span>
                    <span className={`badge ${statusColor(task.status)}`} style={{ fontSize: '0.65rem' }}>
                      {statusLabel(task.status)}
                    </span>
                  </div>

                  <div style={{ fontWeight: 600, fontSize: '0.85rem', color: 'var(--text-primary)' }}>
                    {task.title}
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                    <span>Hours: {task.actualHours} / {task.estimatedHours}h</span>
                    <span style={{ color: due < 0 ? '#ef4444' : due <= 2 ? '#f59e0b' : 'var(--text-muted)' }}>
                      Due: {task.dueDate} {due < 0 ? '(Overdue)' : ''}
                    </span>
                  </div>
                </div>
              );
            })}

            {memberTasks.length === 0 && (
              <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textAlign: 'center', padding: '1rem' }}>
                No tasks assigned
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
