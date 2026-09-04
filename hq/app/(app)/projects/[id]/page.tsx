'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import { priorityColor, statusColor, statusLabel, formatDate, daysUntil, getInitials } from '@/lib/utils';
import type { Project, Task, Priority, ProjectStatus } from '@/lib/types';
import {
  ArrowLeft, Users, Calendar, Target, AlertTriangle, CheckSquare,
  ChevronRight, Clock, Tag, Flag, Check, Edit2, Trash2, X, Plus
} from 'lucide-react';

export default function ProjectDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { projects, tasks, team, updateProject, deleteProject } = useHQStore();
  
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const project = projects.find(p => p.id === params.id);
  const [projectForm, setProjectForm] = useState({
    name: project?.name || '',
    description: project?.description || '',
    category: project?.category || 'product',
    priority: project?.priority || 'high',
    status: project?.status || 'in_progress',
    targetDate: project?.targetDate || '',
    ownerId: project?.ownerId || '',
    objectives: project?.objectives?.join('\n') || '',
  });

  if (!project) return (
    <div className="page"><p style={{ color: 'var(--text-muted)' }}>Project not found.</p></div>
  );

  const owner = team.find(m => m.id === project.ownerId);
  const members = project.memberIds.map(id => team.find(m => m.id === id)).filter(Boolean);
  const projectTasks = tasks.filter(t => t.projectId === project.id);
  const tasksByStatus = {
    in_progress: projectTasks.filter(t => t.status === 'in_progress'),
    blocked: projectTasks.filter(t => t.status === 'blocked'),
    planned: projectTasks.filter(t => t.status === 'planned'),
    done: projectTasks.filter(t => t.status === 'done'),
  };

  const CATEGORY_COLORS: Record<string, string> = {
    product: '#6366f1', rd: '#a78bfa', bizdev: '#10b981',
    operations: '#f59e0b', fundraising: '#f97316', internal: '#94a3b8',
  };

  const handleDelete = () => {
    if (window.confirm('Are you sure you want to delete this project?')) {
      deleteProject(project.id);
      router.push('/projects');
    }
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectForm.name.trim()) return;

    const objList = projectForm.objectives
      ? projectForm.objectives.split('\n').map(o => o.trim()).filter(Boolean)
      : ['Deliver baseline implementation'];

    const newOwnerId = projectForm.ownerId || (team[0]?.id ?? 'user-1');

    updateProject(project.id, {
      name: projectForm.name.trim(),
      description: projectForm.description.trim(),
      category: projectForm.category,
      priority: projectForm.priority,
      status: projectForm.status,
      ownerId: newOwnerId,
      targetDate: projectForm.targetDate,
      objectives: objList,
    });
    
    setIsEditModalOpen(false);
  };

  return (
    <div className="page animate-fade-in">
      {/* Back */}
      <button onClick={() => router.back()} className="btn btn-ghost btn-sm" style={{ width: 'fit-content', paddingLeft: 0 }}>
        <ArrowLeft size={14} />
        Projects
      </button>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '1rem' }}>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <span className={`badge ${priorityColor(project.priority)}`}>
              <Flag size={10} />
              {project.priority}
            </span>
            <span className={`badge ${statusColor(project.status)}`}>{statusLabel(project.status)}</span>
            <span style={{
              fontSize: '0.75rem', color: CATEGORY_COLORS[project.category] || '#818cf8',
              background: `${CATEGORY_COLORS[project.category] || '#6366f1'}15`,
              padding: '0.125rem 0.5rem', borderRadius: 99, fontWeight: 600,
            }}>
              {project.category}
            </span>
            
            <div style={{ marginLeft: 'auto', display: 'flex', gap: '0.5rem' }}>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsEditModalOpen(true)}>
                <Edit2 size={16} />
              </button>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={handleDelete} style={{ color: '#ef4444' }}>
                <Trash2 size={16} />
              </button>
            </div>
          </div>
          
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.03em' }}>{project.name}</h1>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.375rem', fontSize: '0.875rem', maxWidth: 600 }}>{project.description}</p>
        </div>
        <div className="card" style={{ padding: '1rem 1.25rem', flexShrink: 0, minWidth: 200 }}>
          <div style={{ textAlign: 'center', marginBottom: '0.75rem' }}>
            <div style={{ fontSize: '2.5rem', fontWeight: 800, color: project.progress >= 70 ? '#10b981' : '#818cf8', letterSpacing: '-0.04em' }}>
              {project.progress}%
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 500 }}>Complete</div>
          </div>
          <div className="progress-bar" style={{ height: 8 }}>
            <div className={`progress-fill${project.progress >= 70 ? ' success' : ''}`} style={{ width: `${project.progress}%` }} />
          </div>
          <div style={{ marginTop: '0.75rem', fontSize: '0.75rem', color: daysUntil(project.targetDate) <= 14 ? '#ef4444' : 'var(--text-muted)', textAlign: 'center' }}>
            {daysUntil(project.targetDate) > 0
              ? `${daysUntil(project.targetDate)} days remaining`
              : `${Math.abs(daysUntil(project.targetDate))} days overdue`}
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: '1rem' }}>
        {/* Main content */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

          {/* Tasks Kanban */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
              <CheckSquare size={16} color="#3b82f6" />
              <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Tasks ({projectTasks.length})</span>
            </div>
            <div style={{ padding: '1rem', display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.75rem' }}>
              {Object.entries(tasksByStatus).map(([status, statusTasks]) => (
                <div key={status}>
                  <div style={{ fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--text-muted)', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    {statusLabel(status as Task['status'])}
                    <span style={{ color: 'var(--text-secondary)' }}>{statusTasks.length}</span>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
                    {statusTasks.map(t => {
                      const assignee = team.find(m => m.id === t.assigneeId);
                      return (
                        <div key={t.id} style={{
                          background: 'var(--bg-elevated)', border: '1px solid var(--border)',
                          borderRadius: 8, padding: '0.625rem',
                        }}>
                          <div style={{ fontSize: '0.78rem', fontWeight: 500, lineHeight: 1.3, marginBottom: '0.375rem' }}>
                            {t.title}
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                            <span className={`badge ${priorityColor(t.priority)}`} style={{ fontSize: '0.6rem' }}>{t.priority}</span>
                            {assignee && (
                              <div className="avatar" style={{ width: 18, height: 18, fontSize: '0.45rem', background: 'linear-gradient(135deg, #6366f1, #818cf8)' }}>
                                {getInitials(assignee.name)}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Milestones */}
          <div className="card" style={{ overflow: 'hidden' }}>
            <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
              <Target size={16} color="#10b981" />
              <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Milestones</span>
            </div>
            <div style={{ padding: '0.75rem 1.25rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {project.milestones.map((m, i) => (
                <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div style={{
                    width: 20, height: 20, borderRadius: '50%', flexShrink: 0,
                    border: `2px solid ${m.completed ? '#10b981' : 'var(--border)'}`,
                    background: m.completed ? '#10b981' : 'transparent',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    {m.completed && <Check size={12} color="white" />}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 600, fontSize: '0.875rem', color: m.completed ? 'var(--text-secondary)' : 'var(--text-primary)', textDecoration: m.completed ? 'line-through' : 'none' }}>
                      {m.title}
                    </div>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: m.completed ? '#10b981' : daysUntil(m.dueDate) <= 7 ? '#ef4444' : 'var(--text-muted)', flexShrink: 0 }}>
                    {formatDate(m.dueDate)}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Risks */}
          {project.risks.length > 0 && (
            <div className="card" style={{ overflow: 'hidden' }}>
              <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                <AlertTriangle size={16} color="#f59e0b" />
                <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>Risks</span>
              </div>
              <div style={{ padding: '1rem 1.25rem', display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
                {project.risks.map(r => (
                  <div key={r.id} style={{ padding: '0.875rem', background: r.severity === 'high' ? 'rgba(239,68,68,0.05)' : 'var(--bg-elevated)', borderRadius: 8, border: `1px solid ${r.severity === 'high' ? 'rgba(239,68,68,0.2)' : 'var(--border)'}` }}>
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
                      <AlertTriangle size={14} color={r.severity === 'high' ? '#ef4444' : r.severity === 'medium' ? '#f59e0b' : '#94a3b8'} style={{ flexShrink: 0, marginTop: 1 }} />
                      <div>
                        <div style={{ fontWeight: 600, fontSize: '0.85rem', marginBottom: '0.25rem' }}>{r.description}</div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Mitigation: {r.mitigation}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Right sidebar */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {/* Details */}
          <div className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <h3 style={{ fontWeight: 700, fontSize: '0.875rem' }}>Project Details</h3>
            {[
              { label: 'Start Date', value: formatDate(project.startDate) },
              { label: 'Target Date', value: formatDate(project.targetDate) },
            ].map(d => (
              <div key={d.label}>
                <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.25rem' }}>{d.label}</div>
                <div style={{ fontSize: '0.875rem', fontWeight: 500 }}>{d.value}</div>
              </div>
            ))}

            <div>
              <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.5rem' }}>Team</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {members.map(m => m && (
                  <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                    <div className="avatar" style={{ width: 26, height: 26, fontSize: '0.6rem', background: 'linear-gradient(135deg, #6366f1, #818cf8)' }}>
                      {getInitials(m.name)}
                    </div>
                    <div>
                      <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>{m.name}</div>
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                        {m.id === project.ownerId ? 'Owner' : 'Member'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '0.5rem' }}>Tags</div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.375rem' }}>
                {project.tags.map(tag => <span key={tag} className="tag">{tag}</span>)}
              </div>
            </div>
          </div>

          {/* Objectives */}
          <div className="card" style={{ padding: '1.25rem' }}>
            <h3 style={{ fontWeight: 700, fontSize: '0.875rem', marginBottom: '0.875rem' }}>Objectives</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
              {project.objectives.map((obj, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.625rem' }}>
                  <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#6366f1', flexShrink: 0, marginTop: 7 }} />
                  <span style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>{obj}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

       {/* Edit Project Modal */}
       {isEditModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 520, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>Edit Project</h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Update project details and settings.</p>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsEditModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleEditSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Project Name *
                </label>
                <input
                  className="input"
                  value={projectForm.name}
                  onChange={e => setProjectForm({ ...projectForm, name: e.target.value })}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Description
                </label>
                <textarea
                  className="input"
                  rows={2}
                  value={projectForm.description}
                  onChange={e => setProjectForm({ ...projectForm, description: e.target.value })}
                  style={{ resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Category Pillar
                  </label>
                  <select
                    className="input"
                    value={projectForm.category}
                    onChange={e => setProjectForm({ ...projectForm, category: e.target.value as Project['category'] })}
                  >
                    <option value="product">Product & Engineering</option>
                    <option value="rd">R&D / Physical-AI</option>
                    <option value="bizdev">Business Development / OEM</option>
                    <option value="operations">Operations & Testing</option>
                    <option value="fundraising">Fundraising & Cap Table</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Lead Owner
                  </label>
                  <select
                    className="input"
                    value={projectForm.ownerId}
                    onChange={e => setProjectForm({ ...projectForm, ownerId: e.target.value })}
                  >
                    {team.map(m => (
                      <option key={m.id} value={m.id}>{m.name} ({m.role})</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Priority
                  </label>
                  <select
                    className="input"
                    value={projectForm.priority}
                    onChange={e => setProjectForm({ ...projectForm, priority: e.target.value as Priority })}
                  >
                    <option value="critical">Critical</option>
                    <option value="high">High</option>
                    <option value="medium">Medium</option>
                    <option value="low">Low</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Status
                  </label>
                  <select
                    className="input"
                    value={projectForm.status}
                    onChange={e => setProjectForm({ ...projectForm, status: e.target.value as ProjectStatus })}
                  >
                    <option value="in_progress">In Progress</option>
                    <option value="planned">Planned</option>
                    <option value="review">Review</option>
                    <option value="testing">Testing</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Target Completion
                  </label>
                  <input
                    type="date"
                    className="input"
                    value={projectForm.targetDate}
                    onChange={e => setProjectForm({ ...projectForm, targetDate: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Objectives (one per line)
                </label>
                <textarea
                  className="input"
                  rows={2}
                  value={projectForm.objectives}
                  onChange={e => setProjectForm({ ...projectForm, objectives: e.target.value })}
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
