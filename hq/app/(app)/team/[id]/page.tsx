'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { getInitials, priorityColor, statusColor, statusLabel, daysUntil } from '@/lib/utils';
import Link from 'next/link';
import { notFound, useParams, useRouter } from 'next/navigation';
import {
  ArrowLeft, Mail, Calendar, Briefcase, CheckCircle2, AlertCircle,
  Award, Shield, FileText, Clock, Edit2, Trash2, X
} from 'lucide-react';
import type { TeamMember } from '@/lib/types';

const ROLE_LABELS: Record<string, { label: string; color: string; bg: string }> = {
  c_level: { label: 'Admin (C-Level)', color: '#818cf8', bg: 'rgba(99,102,241,0.12)' },
  executive: { label: 'Executive', color: '#34d399', bg: 'rgba(52,211,153,0.12)' },
  intern: { label: 'Intern', color: '#f472b6', bg: 'rgba(244,114,182,0.12)' },
};

export default function MemberProfilePage() {
  const params = useParams();
  const router = useRouter();
  const id = Array.isArray(params.id) ? params.id[0] : params.id;
  const { team, tasks, projects, currentUser, updateMember, deleteMember } = useHQStore();

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  
  const member = team.find(m => m.id === id);
  const [formData, setFormData] = useState<Partial<TeamMember>>({
     name: member?.name || '',
     designation: member?.designation || '',
     department: member?.department || '',
     role: member?.role || 'intern',
     skills: member?.skills || [],
     performanceNotes: member?.performanceNotes || ''
  });

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

  const isEditable = currentUser?.role === 'c_level';

  const handleDelete = async () => {
      if(window.confirm('Are you sure you want to permanently delete this user?')) {
          const res = await deleteMember(member.id);
          if (res.success) {
              router.push('/team');
          } else {
              alert('Failed to delete member: ' + res.error);
          }
      }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      const res = await updateMember(member.id, formData);
      if (res.success) {
          setIsEditModalOpen(false);
      } else {
          alert('Failed to update member: ' + res.error);
      }
  };

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
      <div className="card" style={{ padding: '1.75rem', position: 'relative' }}>
         {isEditable && (
             <div style={{ position: 'absolute', top: '1.25rem', right: '1.25rem', display: 'flex', gap: '0.5rem' }}>
                 <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsEditModalOpen(true)}>
                     <Edit2 size={16} />
                 </button>
                 <button className="btn btn-icon btn-ghost btn-sm" onClick={handleDelete} style={{ color: '#ef4444' }}>
                     <Trash2 size={16} />
                 </button>
             </div>
         )}
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
                {member.email && (
                    <>
                        <span>·</span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                        <Mail size={13} />
                        {member.email}
                        </span>
                    </>
                )}
                <span>·</span>
                <span>Joined {member.joiningDate}</span>
                <span>·</span>
                <span>@{member.username}</span>
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
              <div style={{ fontSize: '1.2rem', fontWeight: 700, color: member.overdueTasks > 0 ? '#ef4444' : '#10b981' }}>{member.overdueTasks || 0}</div>
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
            {member.skills.length === 0 && <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>No skills listed</span>}
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

       {/* Edit Member Modal */}
       {isEditModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 520, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>Edit Member Profile</h3>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsEditModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleEditSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Name
                </label>
                <input
                  className="input"
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Designation
                  </label>
                  <input
                    className="input"
                    value={formData.designation}
                    onChange={e => setFormData({ ...formData, designation: e.target.value })}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Department
                  </label>
                   <input
                    className="input"
                    value={formData.department}
                    onChange={e => setFormData({ ...formData, department: e.target.value })}
                  />
                </div>
              </div>
              
              <div>
                 <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Role (Access Level)
                  </label>
                  <select
                    className="input"
                    value={formData.role}
                    onChange={e => setFormData({ ...formData, role: e.target.value as TeamMember['role'] })}
                  >
                    <option value="c_level">C-Level (Admin)</option>
                    <option value="executive">Executive</option>
                    <option value="intern">Intern</option>
                  </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Skills (comma separated)
                </label>
                 <input
                  className="input"
                  value={formData.skills?.join(', ')}
                  onChange={e => setFormData({ ...formData, skills: e.target.value.split(',').map(s => s.trim()).filter(Boolean) })}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Performance Notes
                </label>
                <textarea
                  className="input"
                  rows={3}
                  value={formData.performanceNotes}
                  onChange={e => setFormData({ ...formData, performanceNotes: e.target.value })}
                  style={{ resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsEditModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
