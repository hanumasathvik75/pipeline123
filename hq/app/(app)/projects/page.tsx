'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { priorityColor, statusColor, statusLabel, daysUntil, getInitials } from '@/lib/utils';
import { Plus, LayoutGrid, Table, List, Search, Filter, ArrowUpRight, X } from 'lucide-react';
import Link from 'next/link';
import type { Project, Priority, ProjectStatus } from '@/lib/types';

const STATUS_ORDER = ['in_progress', 'review', 'testing', 'planned', 'backlog', 'done', 'on_hold'];

const CATEGORY_LABELS: Record<string, string> = {
  product: 'Product', rd: 'R&D', bizdev: 'Business Dev',
  operations: 'Operations', fundraising: 'Fundraising', internal: 'Internal',
};

const CATEGORY_COLORS: Record<string, string> = {
  product: '#6366f1', rd: '#a78bfa', bizdev: '#10b981',
  operations: '#f59e0b', fundraising: '#f97316', internal: '#94a3b8',
};

export default function ProjectsPage() {
  const { projects, team, addProject } = useHQStore();
  const [view, setView] = useState<'grid' | 'table'>('grid');
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<string>('all');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newProject, setNewProject] = useState({
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

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Projects</h1>
          <p className="page-subtitle">{projects.length} projects · {projects.filter(p => p.status === 'in_progress').length} active</p>
        </div>
        <button className="btn btn-primary" onClick={() => setIsAddModalOpen(true)}>
          <Plus size={15} />
          New Project
        </button>
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
              color: filter === f ? '#818cf8' : 'var(--text-secondary)',
              border: `1px solid ${filter === f ? 'rgba(99,102,241,0.3)' : 'var(--border)'}`,
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
                <div className="card" style={{ padding: '1.25rem', cursor: 'pointer', height: '100%', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  {/* Header */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '0.5rem' }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.375rem' }}>
                        <span style={{
                          width: 8, height: 8, borderRadius: '50%', flexShrink: 0,
                          background: CATEGORY_COLORS[p.category] || '#6366f1',
                        }} />
                        <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 500 }}>
                          {CATEGORY_LABELS[p.category]}
                        </span>
                      </div>
                      <h3 style={{ fontWeight: 700, fontSize: '0.9rem', lineHeight: 1.3, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                        {p.name}
                      </h3>
                    </div>
                    <ArrowUpRight size={14} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
                  </div>

                  {/* Progress */}
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.375rem' }}>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Progress</span>
                      <span style={{ fontSize: '0.8rem', fontWeight: 700, color: p.progress >= 70 ? '#10b981' : p.progress >= 40 ? '#818cf8' : '#f59e0b' }}>
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
                          background: 'linear-gradient(135deg, #6366f1, #818cf8)',
                          marginLeft: i > 0 ? -6 : 0, border: '2px solid var(--bg-surface)',
                        }}>
                          {getInitials(m.name)}
                        </div>
                      ))}
                    </div>
                  </div>

                  <div style={{ fontSize: '0.7rem', color: daysUntil(p.targetDate) <= 14 ? '#ef4444' : 'var(--text-muted)' }}>
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
              </tr>
            </thead>
            <tbody>
              {sorted.map(p => {
                const owner = team.find(m => m.id === p.ownerId);
                return (
                  <tr key={p.id} style={{ cursor: 'pointer' }}>
                    <td>
                      <Link href={`/projects/${p.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                        <span style={{ fontWeight: 600 }}>{p.name}</span>
                      </Link>
                    </td>
                    <td><span style={{ fontSize: '0.8rem', color: CATEGORY_COLORS[p.category] }}>{CATEGORY_LABELS[p.category]}</span></td>
                    <td>
                      {owner && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <div className="avatar" style={{ width: 22, height: 22, fontSize: '0.55rem', background: 'linear-gradient(135deg, #6366f1, #818cf8)' }}>
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
                    <td style={{ fontSize: '0.8rem', color: daysUntil(p.targetDate) <= 14 ? '#ef4444' : 'var(--text-secondary)' }}>
                      {new Date(p.targetDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* New Project Modal */}
      {isAddModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 520, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>Create New Initiative</h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Define a new engineering, R&D, product, or business milestone.</p>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsAddModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={(e) => {
              e.preventDefault();
              if (!newProject.name.trim()) return;

              const objList = newProject.objectives
                ? newProject.objectives.split('\n').map(o => o.trim()).filter(Boolean)
                : ['Deliver baseline implementation'];

              const owner = newProject.ownerId || (team[0]?.id ?? 'user-1');

              addProject({
                name: newProject.name.trim(),
                description: newProject.description.trim() || 'Core initiative for IgnitionAI platform',
                category: newProject.category,
                priority: newProject.priority,
                status: newProject.status,
                ownerId: owner,
                memberIds: [owner],
                startDate: new Date().toISOString().split('T')[0],
                targetDate: newProject.targetDate || new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
                progress: newProject.status === 'in_progress' ? 10 : 0,
                objectives: objList,
                milestones: [],
                risks: [],
                tags: [newProject.category, 'active'],
              });

              setIsAddModalOpen(false);
              setNewProject({
                name: '',
                description: '',
                category: 'product',
                priority: 'high',
                status: 'in_progress',
                targetDate: '',
                ownerId: '',
                objectives: '',
              });
            }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Project Name *
                </label>
                <input
                  className="input"
                  placeholder="e.g. Next-Gen CAN Telematics Edge Hub"
                  value={newProject.name}
                  onChange={e => setNewProject({ ...newProject, name: e.target.value })}
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
                  value={newProject.description}
                  onChange={e => setNewProject({ ...newProject, description: e.target.value })}
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
                    value={newProject.category}
                    onChange={e => setNewProject({ ...newProject, category: e.target.value as Project['category'] })}
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
                    value={newProject.ownerId}
                    onChange={e => setNewProject({ ...newProject, ownerId: e.target.value })}
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
                    value={newProject.priority}
                    onChange={e => setNewProject({ ...newProject, priority: e.target.value as Priority })}
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
                    value={newProject.status}
                    onChange={e => setNewProject({ ...newProject, status: e.target.value as ProjectStatus })}
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
                    value={newProject.targetDate}
                    onChange={e => setNewProject({ ...newProject, targetDate: e.target.value })}
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
                  value={newProject.objectives}
                  onChange={e => setNewProject({ ...newProject, objectives: e.target.value })}
                  style={{ resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsAddModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  <Plus size={14} />
                  Launch Project
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
