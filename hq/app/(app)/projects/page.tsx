'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { priorityColor, statusColor, statusLabel, daysUntil, getInitials } from '@/lib/utils';
import { Plus, LayoutGrid, Table, Search, ArrowUpRight, X, Edit2, Trash2 } from 'lucide-react';
import Link from 'next/link';
import type { Project, Priority, ProjectStatus } from '@/lib/types';

const CATEGORY_LABELS: Record<string, string> = {
  product: 'Product', rd: 'R&D', bizdev: 'Business Dev',
  operations: 'Operations', fundraising: 'Fundraising', internal: 'Internal',
};

const CATEGORY_COLORS: Record<string, string> = {
  product: '#000000', rd: '#000000', bizdev: '#000000',
  operations: '#000000', fundraising: '#000000', internal: '#000000',
};

export default function ProjectsPage() {
  const { projects, team, addProject, updateProject, deleteProject, currentUser } = useHQStore();
  const [view, setView] = useState<'grid' | 'table'>('grid');
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<string>('all');
  
  // Modal states
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [projectForm, setProjectForm] = useState({
    name: '',
    description: '',
    category: 'product' as Project['category'],
    priority: 'high' as Priority,
    status: 'in_progress' as ProjectStatus,
    targetDate: '',
    ownerId: '',
    objectives: '',
  });

  const filtered = projects.filter(p => {
    const matchSearch = p.name.toLowerCase().includes(search.toLowerCase());
    const matchFilter = filter === 'all' || p.status === filter || p.category === filter;
    return matchSearch && matchFilter;
  });

  const sorted = [...filtered].sort((a, b) => {
    const pa = ['critical', 'high', 'medium', 'low'].indexOf(a.priority);
    const pb = ['critical', 'high', 'medium', 'low'].indexOf(b.priority);
    return pa - pb;
  });

  const openAddModal = () => {
    setProjectForm({
      name: '',
      description: '',
      category: 'product',
      priority: 'high',
      status: 'in_progress',
      targetDate: '',
      ownerId: currentUser?.id || '',
      objectives: '',
    });
    setIsAddModalOpen(true);
  };

  const openEditModal = (p: Project, e: React.MouseEvent) => {
    e.preventDefault(); // Prevent Link navigation
    e.stopPropagation();
    setEditingProject(p);
    setProjectForm({
      name: p.name,
      description: p.description,
      category: p.category,
      priority: p.priority,
      status: p.status,
      targetDate: p.targetDate,
      ownerId: p.ownerId,
      objectives: p.objectives.join('\n'),
    });
  };

  const handleDelete = (id: string, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (window.confirm('Are you sure you want to delete this project?')) {
      deleteProject(id);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectForm.name.trim()) return;

    const objList = projectForm.objectives
      ? projectForm.objectives.split('\n').map(o => o.trim()).filter(Boolean)
      : ['Deliver baseline implementation'];

    const owner = projectForm.ownerId || (team[0]?.id ?? 'user-1');

    if (editingProject) {
      updateProject(editingProject.id, {
        name: projectForm.name.trim(),
        description: projectForm.description.trim(),
        category: projectForm.category,
        priority: projectForm.priority,
        status: projectForm.status,
        ownerId: owner,
        targetDate: projectForm.targetDate,
        objectives: objList,
      });
      setEditingProject(null);
    } else {
      addProject({
        name: projectForm.name.trim(),
        description: projectForm.description.trim() || 'Core initiative for IgnitionAI platform',
        category: projectForm.category,
        priority: projectForm.priority,
        status: projectForm.status,
        ownerId: owner,
        memberIds: [owner],
        startDate: new Date().toISOString().split('T')[0],
        targetDate: projectForm.targetDate || new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
        progress: projectForm.status === 'in_progress' ? 10 : 0,
        objectives: objList,
        milestones: [],
        risks: [],
        tags: [projectForm.category, 'active'],
      });
      setIsAddModalOpen(false);
    }
  };

  const isModalOpen = isAddModalOpen || !!editingProject;

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Projects</h1>
          <p className="page-subtitle">{projects.length} projects · {projects.filter(p => p.status === 'in_progress').length} active</p>
        </div>
        {currentUser?.role !== 'intern' && (
          <button className="btn btn-primary" onClick={openAddModal}>
            <Plus size={15} />
            New Project
          </button>
        )}
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: '0 0 240px' }}>
          <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input className="input" placeholder="Search projects…" value={search} onChange={e => setSearch(e.target.value)} style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }} />
        </div>
        {['all', 'in_progress', 'planned', 'product', 'rd', 'bizdev', 'operations'].map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className="btn btn-sm"
            style={{
              background: filter === f ? 'var(--accent-muted)' : 'var(--bg-elevated)',
              color: filter === f ? '#000000' : 'var(--text-secondary)',
              border: `1px solid ${filter === f ? 'rgba(0, 0, 0, 0.3)' : 'var(--border)'}`,
            }}
          >
            {f === 'all' ? 'All' : f === 'in_progress' ? 'In Progress' : f === 'rd' ? 'R&D' : f === 'bizdev' ? 'Biz Dev' : f.charAt(0).toUpperCase() + f.slice(1)}
          </button>
        ))}
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '0.25rem' }}>
          <button onClick={() => setView('grid')} className={`btn btn-icon btn-sm ${view === 'grid' ? 'btn-secondary' : 'btn-ghost'}`}><LayoutGrid size={14} /></button>
          <button onClick={() => setView('table')} className={`btn btn-icon btn-sm ${view === 'table' ? 'btn-secondary' : 'btn-ghost'}`}><Table size={14} /></button>
        </div>
      </div>

      {/* Grid View */}
      {view === 'grid' && (
        <div className="grid-auto">
          {sorted.map(p => {
            const owner = team.find(m => m.id === p.ownerId);
            const members = p.memberIds.map(id => team.find(m => m.id === id)).filter(Boolean);
            return (
              <Link key={p.id} href={`/projects/${p.id}`} style={{ textDecoration: 'none' }}>
                <div className="card hover-effect" style={{ padding: '1.25rem', cursor: 'pointer', height: '100%', display: 'flex', flexDirection: 'column', gap: '1rem', position: 'relative' }}>
                  
                  {/* Actions overlay */}
                  <div style={{ position: 'absolute', top: '0.75rem', right: '0.75rem', display: 'flex', gap: '0.25rem' }}>
                     <button className="btn btn-icon btn-ghost btn-sm" onClick={(e) => openEditModal(p, e)}>
                        <Edit2 size={14} />
                     </button>
                     <button className="btn btn-icon btn-ghost btn-sm" onClick={(e) => handleDelete(p.id, e)} style={{ color: '#000000' }}>
                        <Trash2 size={14} />
                     </button>
                  </div>

                  {/* Header */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '0.5rem', paddingRight: '3rem' }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.375rem' }}>
                        <span style={{
                          width: 8, height: 8, borderRadius: '50%', flexShrink: 0,
                          background: CATEGORY_COLORS[p.category] || '#000000',
                        }} />
                        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 500 }}>
                          {CATEGORY_LABELS[p.category]}
                        </span>
                      </div>
                      <h3 style={{ fontWeight: 700, fontSize: '0.9rem', lineHeight: 1.3, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                        {p.name}
                      </h3>
                    </div>
                  </div>

                  {/* Progress */}
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Progress</span>
                      <span style={{ fontSize: '0.8rem', fontWeight: 700, color: p.progress >= 70 ? '#000000' : p.progress >= 40 ? '#000000' : '#000000' }}>
                        {p.progress}%
                      </span>
                    </div>
                    <div className="progress-bar">
                      <div className={`progress-fill${p.progress >= 70 ? ' success' : ''}`} style={{ width: `${p.progress}%` }} />
                    </div>
                  </div>

                  {/* Meta */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 'auto' }}>
                    <div style={{ display: 'flex', gap: '0.375rem' }}>
                      <span className={`badge ${statusColor(p.status)}`}>{statusLabel(p.status)}</span>
                      <span className={`badge ${priorityColor(p.priority)}`}>{p.priority}</span>
                    </div>
                    <div style={{ display: 'flex' }}>
                      {members.slice(0, 3).map((m, i) => m && (
                        <div key={m.id} className="avatar" style={{
                          width: 24, height: 24, fontSize: '0.55rem',
                          background: 'linear-gradient(135deg, #000000, #000000)',
                          marginLeft: i > 0 ? -6 : 0, border: '2px solid var(--bg-surface)',
                        }}>
                          {getInitials(m.name)}
                        </div>
                      ))}
                    </div>
                  </div>

                  <div style={{ fontSize: '0.7rem', color: daysUntil(p.targetDate) <= 14 ? '#000000' : 'var(--text-muted)' }}>
                    {daysUntil(p.targetDate) > 0 ? `Due in ${daysUntil(p.targetDate)} days` : `${Math.abs(daysUntil(p.targetDate))} days overdue`}
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}

      {/* Table View */}
      {view === 'table' && (
        <div className="card" style={{ overflow: 'hidden' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>Category</th>
                <th>Owner</th>
                <th>Progress</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Due Date</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {sorted.map(p => {
                const owner = team.find(m => m.id === p.ownerId);
                return (
                  <tr key={p.id}>
                    <td>
                      <Link href={`/projects/${p.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                        <span style={{ fontWeight: 600 }}>{p.name}</span>
                      </Link>
                    </td>
                    <td><span style={{ fontSize: '0.8rem', color: CATEGORY_COLORS[p.category] }}>{CATEGORY_LABELS[p.category]}</span></td>
                    <td>
                      {owner && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <div className="avatar" style={{ width: 22, height: 22, fontSize: '0.55rem', background: 'linear-gradient(135deg, #000000, #000000)' }}>
                            {getInitials(owner.name)}
                          </div>
                          <span style={{ fontSize: '0.8rem' }}>{owner.name}</span>
                        </div>
                      )}
                    </td>
                    <td style={{ width: 120 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <div className="progress-bar" style={{ flex: 1 }}>
                          <div className="progress-fill" style={{ width: `${p.progress}%` }} />
                        </div>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600, width: 32 }}>{p.progress}%</span>
                      </div>
                    </td>
                    <td><span className={`badge ${priorityColor(p.priority)}`}>{p.priority}</span></td>
                    <td><span className={`badge ${statusColor(p.status)}`}>{statusLabel(p.status)}</span></td>
                    <td style={{ fontSize: '0.8rem', color: daysUntil(p.targetDate) <= 14 ? '#000000' : 'var(--text-secondary)' }}>
                      {new Date(p.targetDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                       <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.25rem' }}>
                          <button className="btn btn-icon btn-ghost btn-sm" onClick={(e) => openEditModal(p, e)}>
                            <Edit2 size={14} />
                          </button>
                          <button className="btn btn-icon btn-ghost btn-sm" onClick={(e) => handleDelete(p.id, e)} style={{ color: '#000000' }}>
                            <Trash2 size={14} />
                          </button>
                        </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Project Modal */}
      {isModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 520, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                  {editingProject ? 'Edit Project' : 'Create New Initiative'}
                </h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {editingProject ? 'Update project details and settings.' : 'Define a new engineering, R&D, product, or business milestone.'}
                </p>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => { setIsAddModalOpen(false); setEditingProject(null); }}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Project Name *
                </label>
                <input
                  className="input"
                  placeholder="e.g. Next-Gen CAN Telematics Edge Hub"
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
                  placeholder="Scope, technical specs, and deliverables..."
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
                  placeholder="Key measurable outcomes..."
                  value={projectForm.objectives}
                  onChange={e => setProjectForm({ ...projectForm, objectives: e.target.value })}
                  style={{ resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => { setIsAddModalOpen(false); setEditingProject(null); }}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  {editingProject ? 'Save Changes' : <><Plus size={14} /> Launch Project</>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
