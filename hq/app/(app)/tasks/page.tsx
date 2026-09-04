'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { priorityColor, statusColor, statusLabel, daysUntil, getInitials, avatarColor } from '@/lib/utils';
import type { Task, TaskStatus, Priority } from '@/lib/types';
import {
  Plus, Search, Filter, CheckCircle2, Clock, AlertTriangle, ChevronRight,
  User as UserIcon, Calendar, CheckSquare, MoreVertical, LayoutGrid, List,
  ArrowRight
} from 'lucide-react';

const COLUMNS: { status: TaskStatus; label: string; color: string }[] = [
  { status: 'backlog', label: 'Backlog', color: '#64748b' },
  { status: 'planned', label: 'Planned', color: '#38bdf8' },
  { status: 'in_progress', label: 'In Progress', color: '#6366f1' },
  { status: 'review', label: 'Review', color: '#a855f7' },
  { status: 'testing', label: 'Testing', color: '#06b6d4' },
  { status: 'blocked', label: 'Blocked', color: '#ef4444' },
  { status: 'done', label: 'Done', color: '#10b981' },
];

export default function TasksPage() {
  const { tasks, projects, team, updateTaskStatus, toggleSubtask, addTask } = useHQStore();
  const [view, setView] = useState<'kanban' | 'list'>('kanban');
  const [search, setSearch] = useState('');
  const [projectFilter, setProjectFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');
  const [assigneeFilter, setAssigneeFilter] = useState('all');
  const [showNewModal, setShowNewModal] = useState(false);

  // New task form state
  const [newTitle, setNewTitle] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [newProjectId, setNewProjectId] = useState(projects[0]?.id || '');
  const [newAssigneeId, setNewAssigneeId] = useState(team[0]?.id || '');
  const [newPriority, setNewPriority] = useState<Priority>('medium');
  const [newDueDate, setNewDueDate] = useState('');
  const [newEstHours, setNewEstHours] = useState(4);

  const filteredTasks = tasks.filter(task => {
    const matchesSearch = task.title.toLowerCase().includes(search.toLowerCase()) ||
      task.description.toLowerCase().includes(search.toLowerCase());
    const matchesProject = projectFilter === 'all' || task.projectId === projectFilter;
    const matchesPriority = priorityFilter === 'all' || task.priority === priorityFilter;
    const matchesAssignee = assigneeFilter === 'all' || task.assigneeId === assigneeFilter;
    return matchesSearch && matchesProject && matchesPriority && matchesAssignee;
  });

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    const task: Task = {
      id: `task-${Date.now()}`,
      title: newTitle.trim(),
      description: newDesc.trim() || 'No description provided',
      projectId: newProjectId || (projects[0]?.id ?? 'proj-1'),
      assigneeId: newAssigneeId || (team[0]?.id ?? 'user-1'),
      status: 'planned',
      priority: newPriority,
      dueDate: newDueDate || new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
      estimatedHours: Number(newEstHours) || 4,
      actualHours: 0,
      tags: ['task'],
      subtasks: [],
      dependencies: [],
      deliverables: [],
      createdAt: new Date().toISOString().split('T')[0],
      updatedAt: new Date().toISOString().split('T')[0],
    };

    addTask(task);
    setNewTitle('');
    setNewDesc('');
    setShowNewModal(false);
  };

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Tasks</h1>
          <p className="page-subtitle">
            {tasks.length} total tasks · {tasks.filter(t => t.status === 'in_progress').length} in progress · {tasks.filter(t => t.status === 'blocked').length} blocked
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowNewModal(true)}>
          <Plus size={15} />
          New Task
        </button>
      </div>

      {/* Filter bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: '0 0 240px' }}>
          <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="input"
            placeholder="Search tasks…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }}
          />
        </div>

        {/* Project filter */}
        <select
          value={projectFilter}
          onChange={e => setProjectFilter(e.target.value)}
          className="input"
          style={{ width: 'auto', height: 34, fontSize: '0.8rem', padding: '0 0.75rem' }}
        >
          <option value="all">All Projects</option>
          {projects.map(p => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>

        {/* Priority filter */}
        <select
          value={priorityFilter}
          onChange={e => setPriorityFilter(e.target.value)}
          className="input"
          style={{ width: 'auto', height: 34, fontSize: '0.8rem', padding: '0 0.75rem' }}
        >
          <option value="all">All Priorities</option>
          <option value="critical">Critical</option>
          <option value="high">High</option>
          <option value="medium">Medium</option>
          <option value="low">Low</option>
        </select>

        {/* Assignee filter */}
        <select
          value={assigneeFilter}
          onChange={e => setAssigneeFilter(e.target.value)}
          className="input"
          style={{ width: 'auto', height: 34, fontSize: '0.8rem', padding: '0 0.75rem' }}
        >
          <option value="all">All Assignees</option>
          {team.map(m => (
            <option key={m.id} value={m.id}>{m.name}</option>
          ))}
        </select>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: '0.25rem' }}>
          <button
            onClick={() => setView('kanban')}
            className={`btn btn-icon btn-sm ${view === 'kanban' ? 'btn-secondary' : 'btn-ghost'}`}
            title="Kanban view"
          >
            <LayoutGrid size={14} />
          </button>
          <button
            onClick={() => setView('list')}
            className={`btn btn-icon btn-sm ${view === 'list' ? 'btn-secondary' : 'btn-ghost'}`}
            title="List view"
          >
            <List size={14} />
          </button>
        </div>
      </div>

      {/* Kanban Board */}
      {view === 'kanban' && (
        <div className="kanban-board" style={{ overflowX: 'auto', paddingBottom: '1rem', minHeight: 'calc(100vh - 220px)' }}>
          {COLUMNS.map(col => {
            const colTasks = filteredTasks.filter(t => t.status === col.status);
            return (
              <div key={col.status} className="kanban-col" style={{ minWidth: 280, maxWidth: 320 }}>
                {/* Column Header */}
                <div className="kanban-header" style={{ borderTop: `3px solid ${col.color}` }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span style={{ fontSize: '0.825rem', fontWeight: 700 }}>{col.label}</span>
                    <span className="badge" style={{ background: 'var(--bg-card)', color: 'var(--text-muted)', fontSize: '0.7rem' }}>
                      {colTasks.length}
                    </span>
                  </div>
                </div>

                {/* Cards */}
                <div className="kanban-cards" style={{ minHeight: 200 }}>
                  {colTasks.map(task => {
                    const project = projects.find(p => p.id === task.projectId);
                    const assignee = team.find(m => m.id === task.assigneeId);
                    const due = daysUntil(task.dueDate);
                    const completedSubtasks = task.subtasks.filter(st => st.completed).length;

                    return (
                      <div key={task.id} className="kanban-card card" style={{ padding: '0.875rem' }}>
                        {/* Top: Project tag & Priority */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                          <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '65%' }}>
                            {project?.name || 'Project'}
                          </span>
                          <span className={`badge ${priorityColor(task.priority)}`} style={{ fontSize: '0.65rem', padding: '0.1rem 0.4rem' }}>
                            {task.priority}
                          </span>
                        </div>

                        {/* Title */}
                        <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.375rem', lineHeight: 1.35 }}>
                          {task.title}
                        </h4>

                        {/* Description snippet */}
                        {task.description && (
                          <p style={{ fontSize: '0.725rem', color: 'var(--text-secondary)', marginBottom: '0.625rem', lineHeight: 1.4, overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                            {task.description}
                          </p>
                        )}

                        {/* Subtasks Progress if any */}
                        {task.subtasks.length > 0 && (
                          <div style={{ marginBottom: '0.625rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.675rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                              <span>Subtasks</span>
                              <span>{completedSubtasks}/{task.subtasks.length}</span>
                            </div>
                            <div className="progress-bar" style={{ height: 4 }}>
                              <div
                                className="progress-fill"
                                style={{ width: `${(completedSubtasks / task.subtasks.length) * 100}%`, background: 'var(--accent)' }}
                              />
                            </div>
                          </div>
                        )}

                        {/* Footer info: due date, hours, assignee, quick move */}
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '0.5rem', borderTop: '1px solid var(--border)', fontSize: '0.7rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: due < 0 ? '#ef4444' : due <= 2 ? '#f59e0b' : 'var(--text-muted)' }}>
                            <Calendar size={12} />
                            <span>{due < 0 ? `${Math.abs(due)}d overdue` : due === 0 ? 'Today' : `${due}d left`}</span>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            {assignee && (
                              <div
                                title={assignee.name}
                                style={{
                                  width: 22,
                                  height: 22,
                                  borderRadius: '50%',
                                  background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  fontSize: '0.65rem',
                                  fontWeight: 700,
                                  color: '#fff',
                                }}
                              >
                                {getInitials(assignee.name)}
                              </div>
                            )}

                            {/* Move status select */}
                            <select
                              value={task.status}
                              onChange={e => updateTaskStatus(task.id, e.target.value as TaskStatus)}
                              style={{
                                background: 'transparent',
                                border: 'none',
                                color: 'var(--text-muted)',
                                fontSize: '0.65rem',
                                cursor: 'pointer',
                                outline: 'none',
                              }}
                              title="Change status"
                            >
                              {COLUMNS.map(c => (
                                <option key={c.status} value={c.status} style={{ background: '#1e2230', color: '#fff' }}>
                                  → {c.label}
                                </option>
                              ))}
                            </select>
                          </div>
                        </div>
                      </div>
                    );
                  })}

                  {colTasks.length === 0 && (
                    <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.75rem', border: '1px dashed var(--border)', borderRadius: 'var(--radius)' }}>
                      No tasks
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* List View */}
      {view === 'list' && (
        <div className="card" style={{ padding: '0.5rem' }}>
          <table className="data-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Task</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Project</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Status</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Priority</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Assignee</th>
                <th style={{ textAlign: 'left', padding: '0.75rem' }}>Due</th>
                <th style={{ textAlign: 'right', padding: '0.75rem' }}>Hours</th>
              </tr>
            </thead>
            <tbody>
              {filteredTasks.map(task => {
                const project = projects.find(p => p.id === task.projectId);
                const assignee = team.find(m => m.id === task.assigneeId);
                const due = daysUntil(task.dueDate);

                return (
                  <tr key={task.id} style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '0.75rem' }}>
                      <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{task.title}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{task.description}</div>
                    </td>
                    <td style={{ padding: '0.75rem', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      {project?.name || '—'}
                    </td>
                    <td style={{ padding: '0.75rem' }}>
                      <select
                        value={task.status}
                        onChange={e => updateTaskStatus(task.id, e.target.value as TaskStatus)}
                        className={`badge ${statusColor(task.status)}`}
                        style={{ border: 'none', cursor: 'pointer', outline: 'none' }}
                      >
                        {COLUMNS.map(c => (
                          <option key={c.status} value={c.status} style={{ background: '#1e2230', color: '#fff' }}>
                            {c.label}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td style={{ padding: '0.75rem' }}>
                      <span className={`badge ${priorityColor(task.priority)}`}>
                        {task.priority}
                      </span>
                    </td>
                    <td style={{ padding: '0.75rem' }}>
                      {assignee ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.78rem' }}>
                          <div style={{ width: 20, height: 20, borderRadius: '50%', background: '#6366f1', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.6rem', color: '#fff', fontWeight: 700 }}>
                            {getInitials(assignee.name)}
                          </div>
                          <span>{assignee.name}</span>
                        </div>
                      ) : '—'}
                    </td>
                    <td style={{ padding: '0.75rem', fontSize: '0.78rem', color: due < 0 ? '#ef4444' : due <= 2 ? '#f59e0b' : 'var(--text-muted)' }}>
                      {task.dueDate}
                    </td>
                    <td style={{ padding: '0.75rem', textAlign: 'right', fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                      {task.actualHours} / {task.estimatedHours}h
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* New Task Modal */}
      {showNewModal && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 500, padding: '1.5rem', background: 'var(--bg-surface)' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>Create New Task</h3>
            <form onSubmit={handleCreateTask} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Task Title *</label>
                <input
                  className="input"
                  placeholder="e.g. Implement real-time CAN bus telemetry parser"
                  value={newTitle}
                  onChange={e => setNewTitle(e.target.value)}
                  required
                />
              </div>

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Description</label>
                <textarea
                  className="input"
                  rows={3}
                  placeholder="Objectives and context..."
                  value={newDesc}
                  onChange={e => setNewDesc(e.target.value)}
                  style={{ resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Project</label>
                  <select className="input" value={newProjectId} onChange={e => setNewProjectId(e.target.value)}>
                    {projects.map(p => (
                      <option key={p.id} value={p.id}>{p.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Assignee</label>
                  <select className="input" value={newAssigneeId} onChange={e => setNewAssigneeId(e.target.value)}>
                    {team.map(m => (
                      <option key={m.id} value={m.id}>{m.name} ({m.role})</option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Priority</label>
                  <select className="input" value={newPriority} onChange={e => setNewPriority(e.target.value as Priority)}>
                    <option value="critical">Critical</option>
                    <option value="high">High</option>
                    <option value="medium">Medium</option>
                    <option value="low">Low</option>
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Due Date</label>
                  <input
                    type="date"
                    className="input"
                    value={newDueDate}
                    onChange={e => setNewDueDate(e.target.value)}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Est. Hours</label>
                  <input
                    type="number"
                    min={1}
                    max={200}
                    className="input"
                    value={newEstHours}
                    onChange={e => setNewEstHours(Number(e.target.value))}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowNewModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create Task</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
