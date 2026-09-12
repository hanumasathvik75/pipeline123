'use client';

import { useState, useEffect } from 'react';
import { useHQStore } from '@/lib/store';
import { getInitials } from '@/lib/utils';
import type { UserRole } from '@/lib/types';
import Link from 'next/link';
import {
  Users, UserCheck, AlertCircle, Briefcase, Mail, ArrowUpRight,
  Search, Shield, CheckCircle2, Award, Plus, Lock, Sparkles, X
} from 'lucide-react';

const ROLE_LABELS: Record<string, { label: string; color: string; bg: string }> = {
  c_level: { label: 'Cofounder', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
  executive: { label: 'Executive Team', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
  founder: { label: 'Cofounder', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
  coo: { label: 'Operations & BD Lead', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
  engineering: { label: 'Lead Engineer', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
  ml_rd: { label: 'Lead AI Scientist', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
  bizdev: { label: 'Business Development', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
  intern: { label: 'Intern', color: '#000000', bg: 'rgba(0, 0, 0, 0.12)' },
};

export default function TeamPage() {
  const { team, tasks, projects, currentUser, pendingUsers, fetchPendingUsers, approveMember, rejectMember } = useHQStore();
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [showAddModal, setShowAddModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [activeTab, setActiveTab] = useState<'directory' | 'pending'>('directory');

  useEffect(() => {
    if (activeTab === 'pending' && currentUser?.role === 'c_level') {
      fetchPendingUsers();
    }
  }, [activeTab, currentUser, fetchPendingUsers]);

  // Form state
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [designation, setDesignation] = useState('');
  const [role, setRole] = useState<UserRole>('intern');
  const [department, setDepartment] = useState('Engineering');
  const [skills, setSkills] = useState('');

  const filteredTeam = team.filter(m => {
    const matchesSearch = m.name.toLowerCase().includes(search.toLowerCase()) ||
      (m.designation && m.designation.toLowerCase().includes(search.toLowerCase())) ||
      (m.skills && m.skills.some(s => s.toLowerCase().includes(search.toLowerCase()))) ||
      (m.department && m.department.toLowerCase().includes(search.toLowerCase()));
    const matchesRole = roleFilter === 'all' || m.role === roleFilter;
    return matchesSearch && matchesRole;
  });

  const handleCreateMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim() || !password.trim() || !designation.trim()) {
      setFeedback({ message: 'Please fill in all required fields.', type: 'error' });
      return;
    }

    setIsSubmitting(true);
    setFeedback(null);

    const skillsArray = skills.split(',').map(s => s.trim()).filter(Boolean);

    try {
      const resp = await fetch('/api/users/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: name.trim(),
          username: email.trim().toLowerCase(), // Using email as username for login
          password: password.trim(),
          confirmPassword: password.trim(),
          email: email.trim().toLowerCase(),
          designation: designation.trim(),
          role,
          department: department.trim(),
          skills: skillsArray.length > 0 ? skillsArray : ['AI/ML', 'Engineering'],
        }),
      });
      const data = await resp.json();
      const res = { success: resp.ok && data.success, error: data.error };
      setIsSubmitting(false);

    if (res.success) {
      setFeedback({
        message: `Member ${name} (${designation}) added successfully! They can now log in with their credentials.`,
        type: 'success'
      });
      setName('');
      setEmail('');
      setPassword('');
      setDesignation('');
      setSkills('');
      setTimeout(() => {
        setShowAddModal(false);
        setFeedback(null);
      }, 2500);
    } else {
      setFeedback({ message: res.error || 'Failed to create team member', type: 'error' });
    }
  } catch (err: any) {
    setIsSubmitting(false);
    setFeedback({ message: err.message || 'Network error', type: 'error' });
  }
  };

  return (
    <div className="page animate-fade-in">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Team & Organizational Directory</h1>
          <p className="page-subtitle">
            Core leadership, engineering team, AI/ML researchers, and telematics interns driving IgnitionAI.
          </p>
        </div>
        {currentUser?.role !== 'intern' && (
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            <Plus size={15} />
            Add Team Member
          </button>
        )}
      </div>

      {/* KPI Stats */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
            <Users size={20} />
          </div>
          <div>
            <div className="stat-value">{team.length}</div>
            <div className="stat-label">Full Roster</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
            <Briefcase size={20} />
          </div>
          <div>
            <div className="stat-value">{projects.length}</div>
            <div className="stat-label">Total Active Initiatives</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
            <CheckCircle2 size={20} />
          </div>
          <div>
            <div className="stat-value">{tasks.filter(t => t.status === 'done').length}</div>
            <div className="stat-label">Tasks Completed</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(0, 0, 0, 0.12)', color: '#000000' }}>
            <AlertCircle size={20} />
          </div>
          <div>
            <div className="stat-value">{team.reduce((sum, m) => sum + (m.overdueTasks || 0), 0)}</div>
            <div className="stat-label">Overdue Tasks</div>
          </div>
        </div>
      </div>

      {currentUser?.role === 'c_level' && (
        <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--border)', marginBottom: '1.5rem', paddingBottom: '0' }}>
          <button
            onClick={() => setActiveTab('directory')}
            style={{
              background: 'none',
              border: 'none',
              padding: '0.75rem 1rem',
              fontSize: '0.9rem',
              fontWeight: 600,
              color: activeTab === 'directory' ? 'var(--accent)' : 'var(--text-secondary)',
              borderBottom: activeTab === 'directory' ? '2px solid var(--accent)' : '2px solid transparent',
              cursor: 'pointer',
            }}
          >
            Team Directory
          </button>
          <button
            onClick={() => setActiveTab('pending')}
            style={{
              background: 'none',
              border: 'none',
              padding: '0.75rem 1rem',
              fontSize: '0.9rem',
              fontWeight: 600,
              color: activeTab === 'pending' ? 'var(--accent)' : 'var(--text-secondary)',
              borderBottom: activeTab === 'pending' ? '2px solid var(--accent)' : '2px solid transparent',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            Pending Approvals
            {pendingUsers.length > 0 && (
              <span style={{ background: '#000000', color: '#fff', fontSize: '0.7rem', padding: '0.1rem 0.4rem', borderRadius: '1rem' }}>
                {pendingUsers.length}
              </span>
            )}
          </button>
        </div>
      )}

      {activeTab === 'directory' ? (
        <>
          {/* Search & Filters */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '1.5rem' }}>
            <div style={{ position: 'relative', flex: '0 0 280px' }}>
          <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="input"
            placeholder="Search member, designation, skill…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }}
          />
        </div>

        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {['all', 'c_level', 'executive', 'intern'].map(r => (
            <button
              key={r}
              onClick={() => setRoleFilter(r)}
              className={roleFilter === r ? 'btn btn-primary' : 'btn'}
              style={{ fontSize: '0.75rem', padding: '0.25rem 0.75rem' }}
            >
              {r === 'all' ? 'All Roles' : ROLE_LABELS[r]?.label || r}
            </button>
          ))}
        </div>
      </div>

      {/* Team Cards Grid */}
      <div className="grid-auto">
        {filteredTeam.map(member => {
          const roleMeta = ROLE_LABELS[member.role] || ROLE_LABELS.intern;
          const memberTasks = tasks.filter(t => t.assigneeId === member.id);
          const inProgressCount = memberTasks.filter(t => t.status === 'in_progress').length;

          return (
            <Link key={member.id} href={`/team/${member.id}`} style={{ textDecoration: 'none' }}>
              <div
                className="card"
                style={{
                  padding: '1.25rem',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '1rem',
                  height: '100%',
                  cursor: 'pointer',
                  transition: 'transform 0.15s ease, border-color 0.15s ease',
                }}
              >
                {/* Header with Avatar & Details */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div
                    style={{
                      width: 48,
                      height: 48,
                      borderRadius: '50%',
                      background: 'linear-gradient(135deg, #000000, #000000)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '1rem',
                      fontWeight: 700,
                      color: '#fff',
                      flexShrink: 0,
                    }}
                  >
                    {getInitials(member.name)}
                  </div>

                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {member.name}
                      </h3>
                      <ArrowUpRight size={14} style={{ color: 'var(--text-muted)' }} />
                    </div>

                    {/* Explicit Designation */}
                    <div style={{ fontSize: '0.78rem', fontWeight: 600, color: '#000000', marginTop: '0.15rem' }}>
                      {member.designation || roleMeta.label}
                    </div>

                    <span
                      style={{
                        fontSize: '0.65rem',
                        fontWeight: 600,
                        padding: '0.1rem 0.45rem',
                        borderRadius: 4,
                        color: roleMeta.color,
                        background: roleMeta.bg,
                        display: 'inline-block',
                        marginTop: '0.3rem',
                      }}
                    >
                      {roleMeta.label}
                    </span>
                  </div>
                </div>

                {/* Department & Email */}
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                  <div>{member.department} · Joined {member.joiningDate}</div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', color: 'var(--text-secondary)' }}>
                    <Mail size={12} />
                    <span>{member.email}</span>
                  </div>
                </div>

                {/* Skills tags */}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                  {member.skills.slice(0, 4).map((s, i) => (
                    <span key={i} className="badge" style={{ fontSize: '0.65rem' }}>
                      {s}
                    </span>
                  ))}
                  {member.skills.length > 4 && (
                    <span className="badge" style={{ fontSize: '0.65rem', background: 'transparent' }}>
                      +{member.skills.length - 4}
                    </span>
                  )}
                </div>

                {/* Activity Stats Footer */}
                <div style={{
                  display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)',
                  paddingTop: '0.75rem', borderTop: '1px solid var(--border)',
                  textAlign: 'center', marginTop: 'auto'
                }}>
                  <div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                      {member.activeTasks}
                    </div>
                    <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Tasks</div>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#000000' }}>
                      {inProgressCount}
                    </div>
                    <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Active</div>
                  </div>

                  <div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: member.overdueTasks > 0 ? '#000000' : '#000000' }}>
                      {member.overdueTasks}
                    </div>
                    <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Overdue</div>
                  </div>
                </div>
              </div>
            </Link>
          );
        })}
      </div>
      </>
      ) : (
        <div className="grid-auto">
          {pendingUsers.length === 0 ? (
            <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '4rem 1rem', color: 'var(--text-muted)' }}>
              <UserCheck size={32} style={{ margin: '0 auto 1rem', opacity: 0.5 }} />
              <p>No pending registration requests.</p>
            </div>
          ) : (
            pendingUsers.map(user => (
              <div key={user.id} className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'var(--bg-elevated)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
                    {getInitials(user.name)}
                  </div>
                  <div>
                    <h3 style={{ fontSize: '0.95rem', fontWeight: 700 }}>{user.name}</h3>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{user.username}</div>
                  </div>
                </div>
                
                <div style={{ background: 'var(--bg-base)', padding: '0.75rem', borderRadius: 8, fontSize: '0.8rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span style={{ color: 'var(--text-muted)' }}>Role:</span>
                    <span style={{ fontWeight: 600 }}>{user.role}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                    <span style={{ color: 'var(--text-muted)' }}>Designation:</span>
                    <span>{user.designation}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted)' }}>Department:</span>
                    <span>{user.department}</span>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem', marginTop: 'auto' }}>
                  <button 
                    onClick={() => approveMember(user.id, user.role)}
                    className="btn btn-primary" style={{ flex: 1, padding: '0.5rem', fontSize: '0.8rem' }}>
                    Approve
                  </button>
                  <button 
                    onClick={() => rejectMember(user.id)}
                    className="btn" style={{ flex: 1, padding: '0.5rem', fontSize: '0.8rem', color: '#000000', border: '1px solid rgba(0, 0, 0, 0.3)' }}>
                    Reject
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Add Team Member Modal */}
      {showAddModal && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.75)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 520, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>Add New Team Member</h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Provision credentials, title, designation, and department access.</p>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setShowAddModal(false)}>
                <X size={16} />
              </button>
            </div>

            {feedback && (
              <div style={{
                padding: '0.75rem 1rem', borderRadius: 'var(--radius)', marginBottom: '1rem',
                fontSize: '0.8rem',
                background: feedback.type === 'success' ? 'rgba(0, 0, 0, 0.1)' : 'rgba(0, 0, 0, 0.1)',
                border: `1px solid ${feedback.type === 'success' ? 'rgba(0, 0, 0, 0.3)' : 'rgba(0, 0, 0, 0.3)'}`,
                color: feedback.type === 'success' ? '#000000' : '#000000',
              }}>
                {feedback.message}
              </div>
            )}

            <form onSubmit={handleCreateMember} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                    Full Name *
                  </label>
                  <input
                    className="input"
                    placeholder="e.g. Vikramaditya Rao"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                    Designation (Title) *
                  </label>
                  <input
                    className="input"
                    placeholder="e.g. Chief Technology Officer"
                    value={designation}
                    onChange={e => setDesignation(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                    Work Email *
                  </label>
                  <input
                    type="email"
                    className="input"
                    placeholder="name@ignitionai.in"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                    Initial Password *
                  </label>
                  <input
                    type="text"
                    className="input"
                    placeholder="SecretPass123"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                    System Role
                  </label>
                  <select className="input" value={role} onChange={e => setRole(e.target.value as UserRole)}>
                    <option value="c_level">Founder & CEO (Full Access)</option>
                    <option value="executive">Executive Team (Manager)</option>
                    <option value="intern">Intern / Team Member</option>
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                    Department
                  </label>
                  <input
                    className="input"
                    placeholder="e.g. R&D / ML or Engineering"
                    value={department}
                    onChange={e => setDepartment(e.target.value)}
                  />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.35rem' }}>
                  Core Skills (comma separated)
                </label>
                <input
                  className="input"
                  placeholder="e.g. PyTorch, CAN Bus, Battery SOH, Embedded C"
                  value={skills}
                  onChange={e => setSkills(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.75rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowAddModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                  <Sparkles size={14} />
                  {isSubmitting ? 'Provisioning…' : 'Create & Provision Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
