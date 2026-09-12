'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { priorityColor, statusColor, statusLabel, daysUntil, getInitials, avatarColor } from '@/lib/utils';
import type { Task, TaskStatus, Priority } from '@/lib/types';


const COLUMNS: { status: TaskStatus; label: string; color: string }[] = [
  { status: 'backlog', label: 'Backlog', color: '#000000' },
  { status: 'planned', label: 'Planned', color: '#000000' },
  { status: 'in_progress', label: 'In Progress', color: '#000000' },
  { status: 'review', label: 'Review', color: '#000000' },
  { status: 'testing', label: 'Testing', color: '#000000' },
  { status: 'blocked', label: 'Blocked', color: '#000000' },
  { status: 'done', label: 'Done', color: '#000000' },
];

export default function TasksPage() {
  const { tasks, projects, team, updateTaskStatus, toggleSubtask, addTask, updateTask, deleteTask } = useHQStore();
  const [view, setView] = useState<'kanban' | 'list'>('kanban');
  const [search, setSearch] = useState('');
  const [projectFilter, setProjectFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');
  const [assigneeFilter, setAssigneeFilter] = useState('all');
  const [showNewModal, setShowNewModal] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [isEditing, setIsEditing] = useState(false);

  // New/Edit task form state
  const [taskForm, setTaskForm] = useState({
    title: '',
    description: '',
    projectId: projects[0]?.id || '',
    assigneeId: team[0]?.id || '',
    priority: 'medium' as Priority,
    dueDate: '',
    estimatedHours: 4,
  });

  const filteredTasks = tasks.filter(task => {
    const matchesSearch = task.title.toLowerCase().includes(search.toLowerCase()) ||
      task.description.toLowerCase().includes(search.toLowerCase());
    const matchesProject = projectFilter === 'all' || task.projectId === projectFilter;
    const matchesPriority = priorityFilter === 'all' || task.priority === priorityFilter;
    const matchesAssignee = assigneeFilter === 'all' || task.assigneeId === assigneeFilter;
    return matchesSearch && matchesProject && matchesPriority && matchesAssignee;
  });

  const openNewModal = () => {
    setTaskForm({
      title: '',
      description: '',
      projectId: projects[0]?.id || '',
      assigneeId: team[0]?.id || '',
      priority: 'medium',
      dueDate: '',
      estimatedHours: 4,
    });
    setIsEditing(false);
    setSelectedTask(null);
    setShowNewModal(true);
  };

  const openTaskDetail = (task: Task) => {
    setSelectedTask(task);
    setIsEditing(false);
  };

  const startEditing = () => {
    if (!selectedTask) return;
    setTaskForm({
      title: selectedTask.title,
      description: selectedTask.description,
      projectId: selectedTask.projectId,
      assigneeId: selectedTask.assigneeId,
      priority: selectedTask.priority,
      dueDate: selectedTask.dueDate || '',
      estimatedHours: selectedTask.estimatedHours,
    });
    setIsEditing(true);
  };

  const handleDelete = () => {
    if (!selectedTask) return;
    if (window.confirm('Are you sure you want to delete this task?')) {
      deleteTask(selectedTask.id);
      setSelectedTask(null);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskForm.title.trim()) return;

    if (isEditing && selectedTask) {
       updateTask(selectedTask.id, {
          title: taskForm.title.trim(),
          description: taskForm.description.trim(),
          projectId: taskForm.projectId,
          assigneeId: taskForm.assigneeId,
          priority: taskForm.priority,
          dueDate: taskForm.dueDate,
          estimatedHours: Number(taskForm.estimatedHours) || 0,
       });
       setSelectedTask({ ...selectedTask, ...taskForm, estimatedHours: Number(taskForm.estimatedHours) || 0 });
       setIsEditing(false);
    } else {
      const task: Task = {
        id: `task-${Date.now()}`,
        title: taskForm.title.trim(),
        description: taskForm.description.trim() || 'No description provided',
        projectId: taskForm.projectId || (projects.length > 0 ? projects[0].id : ''),
        assigneeId: taskForm.assigneeId || (team.length > 0 ? team[0].id : ''),
        status: 'planned',
        priority: taskForm.priority,
        dueDate: taskForm.dueDate || new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
        estimatedHours: Number(taskForm.estimatedHours) || 4,
        actualHours: 0,
        tags: ['task'],
        subtasks: [],
        dependencies: [],
        deliverables: [],
        createdAt: new Date().toISOString().split('T')[0],
        updatedAt: new Date().toISOString().split('T')[0],
      };

      addTask(task);
      setShowNewModal(false);
    }
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
        <button className="btn btn-primary" onClick={openNewModal}>
          <></>
          New Task
        </button>
      </div>

      {/* Filter bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: '0 0 240px' }}>
          <></>
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
            <></>
          </button>
          <button
            onClick={() => setView('list')}
            className={`btn btn-icon btn-sm ${view === 'list' ? 'btn-secondary' : 'btn-ghost'}`}
            title="List view"
          >
            <></>
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
                      <div key={task.id} className="kanban-card card hover-effect" style={{ padding: '0.875rem' }} onClick={() => openTaskDetail(task)}>
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
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: due < 0 ? '#000000' : due <= 2 ? '#000000' : 'var(--text-muted)' }}>
                            <></>
                            <span>{due < 0 ? `${Math.abs(due)}d overdue` : due === 0 ? 'Today' : `${due}d left`}</span>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }} onClick={(e) => e.stopPropagation()}>
                            {assignee && (
                              <div
                                title={assignee.name}
                                style={{
                                  width: 22,
                                  height: 22,
                                  borderRadius: '50%',
                                  background: 'linear-gradient(135deg, #000000, #000000)',
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
                                <option key={c.status} value={c.status} style={{ background: '#000000', color: '#fff' }}>
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
                  <tr key={task.id} style={{ borderBottom: '1px solid var(--border)', cursor: 'pointer' }} onClick={() => openTaskDetail(task)}>
                    <td style={{ padding: '0.75rem' }}>
                      <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>{task.title}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{task.description}</div>
                    </td>
                    <td style={{ padding: '0.75rem', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      {project?.name || '—'}
                    </td>
                    <td style={{ padding: '0.75rem' }} onClick={(e) => e.stopPropagation()}>
                      <select
                        value={task.status}
                        onChange={e => updateTaskStatus(task.id, e.target.value as TaskStatus)}
                        className={`badge ${statusColor(task.status)}`}
                        style={{ border: 'none', cursor: 'pointer', outline: 'none' }}
                      >
                        {COLUMNS.map(c => (
                          <option key={c.status} value={c.status} style={{ background: '#000000', color: '#fff' }}>
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
                          <div style={{ width: 20, height: 20, borderRadius: '50%', background: '#000000', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.6rem', color: '#fff', fontWeight: 700 }}>
                            {getInitials(assignee.name)}
                          </div>
                          <span>{assignee.name}</span>
                        </div>
                      ) : '—'}
                    </td>
                    <td style={{ padding: '0.75rem', fontSize: '0.78rem', color: due < 0 ? '#000000' : due <= 2 ? '#000000' : 'var(--text-muted)' }}>
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

      {/* Task Detail / Edit Modal */}
      {(showNewModal || selectedTask) && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: (showNewModal || isEditing) ? 500 : 700, padding: '1.5rem', background: 'var(--bg-surface)', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700 }}>
                {showNewModal ? 'Create New Task' : isEditing ? 'Edit Task' : selectedTask?.title}
              </h3>
              <div style={{ display: 'flex', gap: '0.25rem' }}>
                {!showNewModal && !isEditing && (
                  <>
                    <button className="btn btn-icon btn-ghost btn-sm" onClick={startEditing}>
                      <></>
                    </button>
                    <button className="btn btn-icon btn-ghost btn-sm" onClick={handleDelete} style={{ color: '#000000' }}>
                      <></>
                    </button>
                  </>
                )}
                <button className="btn btn-icon btn-ghost btn-sm" onClick={() => { setShowNewModal(false); setSelectedTask(null); setIsEditing(false); }}>
                  <></>
                </button>
              </div>
            </div>
            
            {(showNewModal || isEditing) ? (
              <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Task Title *</label>
                  <input
                    className="input"
                    placeholder="e.g. Implement real-time CAN bus telemetry parser"
                    value={taskForm.title}
                    onChange={e => setTaskForm({ ...taskForm, title: e.target.value })}
                    required
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Description</label>
                  <textarea
                    className="input"
                    rows={3}
                    placeholder="Objectives and context..."
                    value={taskForm.description}
                    onChange={e => setTaskForm({ ...taskForm, description: e.target.value })}
                    style={{ resize: 'vertical' }}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Project</label>
                    <select className="input" value={taskForm.projectId} onChange={e => setTaskForm({ ...taskForm, projectId: e.target.value })}>
                      <option value="">No Project</option>
                      {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select>
                  </div>
                  <div>
                    <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Assignee</label>
                    <select className="input" value={taskForm.assigneeId} onChange={e => setTaskForm({ ...taskForm, assigneeId: e.target.value })}>
                      {team.map(m => (
                        <option key={m.id} value={m.id}>{m.name} ({m.role})</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Priority</label>
                    <select className="input" value={taskForm.priority} onChange={e => setTaskForm({ ...taskForm, priority: e.target.value as Priority })}>
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
                      value={taskForm.dueDate}
                      onChange={e => setTaskForm({ ...taskForm, dueDate: e.target.value })}
                    />
                  </div>
                  <div>
                    <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>Est. Hours</label>
                    <input
                      type="number"
                      min={1}
                      max={200}
                      className="input"
                      value={taskForm.estimatedHours}
                      onChange={e => setTaskForm({ ...taskForm, estimatedHours: Number(e.target.value) })}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                  <button type="button" className="btn btn-secondary" onClick={() => { setShowNewModal(false); setIsEditing(false); }}>Cancel</button>
                  <button type="submit" className="btn btn-primary">{isEditing ? 'Save Changes' : 'Create Task'}</button>
                </div>
              </form>
            ) : (
               selectedTask && (
                 <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
                    <div>
                      <div style={{ marginBottom: '1.5rem' }}>
                        <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Description</div>
                        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.9rem' }}>{selectedTask.description}</p>
                      </div>

                      {selectedTask.subtasks && selectedTask.subtasks.length > 0 && (
                        <div>
                           <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.75rem' }}>Subtasks</div>
                           <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                             {selectedTask.subtasks.map(st => (
                               <div key={st.id} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                 <input 
                                   type="checkbox" 
                                   checked={st.completed}
                                   onChange={() => toggleSubtask(selectedTask.id, st.id)}
                                   style={{ width: 16, height: 16, accentColor: 'var(--accent)' }}
                                 />
                                 <span style={{ fontSize: '0.85rem', color: st.completed ? 'var(--text-muted)' : 'var(--text-primary)', textDecoration: st.completed ? 'line-through' : 'none' }}>
                                   {st.title}
                                 </span>
                               </div>
                             ))}
                           </div>
                        </div>
                      )}
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', paddingLeft: '1.5rem', borderLeft: '1px solid var(--border)' }}>
                       <div>
                         <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Project</div>
                         <div style={{ fontSize: '0.85rem' }}>{projects.find(p => p.id === selectedTask.projectId)?.name || 'None'}</div>
                       </div>
                       
                       <div>
                         <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Assignee</div>
                         <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            {team.find(m => m.id === selectedTask.assigneeId) ? (
                              <>
                                <div style={{ width: 24, height: 24, borderRadius: '50%', background: 'linear-gradient(135deg, #000000, #000000)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.65rem', color: '#fff', fontWeight: 700 }}>
                                  {getInitials(team.find(m => m.id === selectedTask.assigneeId)!.name)}
                                </div>
                                <span style={{ fontSize: '0.85rem' }}>{team.find(m => m.id === selectedTask.assigneeId)!.name}</span>
                              </>
                            ) : 'Unassigned'}
                         </div>
                       </div>

                       <div>
                         <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Status</div>
                         <select
                            value={selectedTask.status}
                            onChange={e => {
                                updateTaskStatus(selectedTask.id, e.target.value as TaskStatus);
                                setSelectedTask({ ...selectedTask, status: e.target.value as TaskStatus });
                            }}
                            className={`badge ${statusColor(selectedTask.status)}`}
                            style={{ border: 'none', cursor: 'pointer', outline: 'none', padding: '0.25rem 0.5rem' }}
                          >
                            {COLUMNS.map(c => (
                              <option key={c.status} value={c.status} style={{ background: '#000000', color: '#fff' }}>
                                {c.label}
                              </option>
                            ))}
                          </select>
                       </div>

                       <div>
                         <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Priority</div>
                         <span className={`badge ${priorityColor(selectedTask.priority)}`}>{selectedTask.priority}</span>
                       </div>

                       <div>
                         <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Due Date</div>
                         <div style={{ fontSize: '0.85rem', color: daysUntil(selectedTask.dueDate) < 0 ? '#000000' : 'inherit' }}>
                           {selectedTask.dueDate || 'No Date'}
                         </div>
                       </div>
                       
                       <div>
                         <div style={{ fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>Estimated Hours</div>
                         <div style={{ fontSize: '0.85rem' }}>{selectedTask.estimatedHours}h</div>
                       </div>
                    </div>
                 </div>
               )
            )}
          </div>
        </div>
      )}
    </div>
  );
}
