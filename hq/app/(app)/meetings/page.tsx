'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { formatDate, getInitials, avatarColor } from '@/lib/utils';
import type { Meeting } from '@/lib/types';
import {
  Calendar, Clock, Users, CheckCircle2, ArrowRight, Plus, ChevronDown,
  ChevronRight, ListTodo, FileText, CheckSquare, Sparkles, ExternalLink, Check, X
} from 'lucide-react';

const TYPE_COLORS: Record<string, { label: string; color: string; bg: string }> = {
  internal: { label: 'Internal Sync', color: '#818cf8', bg: 'rgba(99, 102, 241, 0.12)' },
  external: { label: 'Client / External', color: '#34d399', bg: 'rgba(52, 211, 153, 0.12)' },
  investor: { label: 'Investor', color: '#fbbf24', bg: 'rgba(251, 191, 36, 0.12)' },
  mentor: { label: 'Advisory / Mentor', color: '#c084fc', bg: 'rgba(192, 132, 252, 0.12)' },
};

export default function MeetingsPage() {
  const { meetings, team, projects, convertActionItemToTask, addMeeting } = useHQStore();
  const [filterType, setFilterType] = useState<string>('all');
  const [expandedId, setExpandedId] = useState<string | null>(meetings[0]?.id || null);
  const [convertedToast, setConvertedToast] = useState<string | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newMeeting, setNewMeeting] = useState({
    title: '',
    date: new Date().toISOString().split('T')[0],
    time: '14:00',
    duration: 45,
    type: 'internal' as Meeting['type'],
    projectId: '',
    participants: [] as string[],
    agenda: '',
    notes: '',
  });

  const todayStr = new Date().toISOString().split('T')[0];

  const filteredMeetings = meetings.filter(m => {
    if (filterType === 'all') return true;
    return m.type === filterType;
  });

  const upcomingMeetings = filteredMeetings.filter(m => m.date >= todayStr);
  const pastMeetings = filteredMeetings.filter(m => m.date < todayStr);

  const handleConvertToTask = (meetingId: string, actionItemId: string, actionDesc: string) => {
    convertActionItemToTask(meetingId, actionItemId);
    setConvertedToast(`Converted "${actionDesc.slice(0, 30)}..." to an active task!`);
    setTimeout(() => setConvertedToast(null), 4000);
  };

  return (
    <div className="page animate-fade-in">
      {/* Toast */}
      {convertedToast && (
        <div style={{
          position: 'fixed', bottom: '1.5rem', right: '1.5rem',
          background: 'var(--accent)', color: '#fff',
          padding: '0.75rem 1.25rem', borderRadius: 'var(--radius)',
          boxShadow: '0 10px 25px rgba(0,0,0,0.5)', zIndex: 1000,
          display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', fontWeight: 600
        }}>
          <Sparkles size={16} />
          {convertedToast}
        </div>
      )}

      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">Meetings & Action Items</h1>
          <p className="page-subtitle">
            Executive control for decisions, agendas, and auto-converting meeting action items into project tasks.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setIsAddModalOpen(true)}>
          <Plus size={15} />
          Schedule Meeting
        </button>
      </div>

      {/* Filter Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        {['all', 'internal', 'external', 'investor', 'mentor'].map(type => (
          <button
            key={type}
            onClick={() => setFilterType(type)}
            className="btn btn-sm"
            style={{
              background: filterType === type ? 'var(--accent-muted)' : 'var(--bg-elevated)',
              color: filterType === type ? '#818cf8' : 'var(--text-secondary)',
              border: `1px solid ${filterType === type ? 'rgba(99,102,241,0.3)' : 'var(--border)'}`,
            }}
          >
            {type === 'all' ? 'All Meetings' : TYPE_COLORS[type]?.label || type}
          </button>
        ))}
      </div>

      {/* Main Grid: Left is meetings list, Right is expanded detail view */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 1fr) minmax(400px, 1.4fr)', gap: '1.25rem', alignItems: 'start' }}>
        {/* Meeting List Column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Upcoming section */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <Clock size={14} style={{ color: '#38bdf8' }} />
              <h3 style={{ fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)' }}>
                Upcoming ({upcomingMeetings.length})
              </h3>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
              {upcomingMeetings.map(m => renderMeetingCard(m))}
              {upcomingMeetings.length === 0 && (
                <div className="card" style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  No upcoming meetings
                </div>
              )}
            </div>
          </div>

          {/* Past section */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <Calendar size={14} style={{ color: 'var(--text-muted)' }} />
              <h3 style={{ fontSize: '0.85rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-muted)' }}>
                Recent & Past ({pastMeetings.length})
              </h3>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.625rem' }}>
              {pastMeetings.map(m => renderMeetingCard(m))}
              {pastMeetings.length === 0 && (
                <div className="card" style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                  No past meetings
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Selected Meeting Details View */}
        <div>
          {renderMeetingDetail()}
        </div>
      </div>

      {/* Schedule Meeting Modal */}
      {isAddModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 540, padding: '1.75rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-primary)' }}>Schedule Executive Meeting</h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Sync with founders, venture investors, OEM partners, or internal leads.</p>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsAddModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={(e) => {
              e.preventDefault();
              if (!newMeeting.title.trim() || !newMeeting.date) return;

              const agendaList = newMeeting.agenda
                ? newMeeting.agenda.split('\n').map(a => a.trim()).filter(Boolean)
                : ['Review key deliverables & roadmaps', 'Align on timeline and unblock dependencies'];

              const selectedParticipants = newMeeting.participants.length > 0
                ? newMeeting.participants
                : team.slice(0, 3).map(m => m.id);

              addMeeting({
                title: newMeeting.title.trim(),
                date: newMeeting.date,
                time: newMeeting.time || '14:00',
                duration: Number(newMeeting.duration) || 45,
                type: newMeeting.type,
                projectId: newMeeting.projectId || undefined,
                participants: selectedParticipants,
                agenda: agendaList,
                notes: newMeeting.notes.trim() || 'Session agenda established. Awaiting live notes and action item capture.',
                actionItems: [],
              });

              setIsAddModalOpen(false);
              setNewMeeting({
                title: '',
                date: new Date().toISOString().split('T')[0],
                time: '14:00',
                duration: 45,
                type: 'internal',
                projectId: '',
                participants: [],
                agenda: '',
                notes: '',
              });
            }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Meeting Title *
                </label>
                <input
                  className="input"
                  placeholder="e.g. Seed VC Partner Pitch & Physical-AI Demo"
                  value={newMeeting.title}
                  onChange={e => setNewMeeting({ ...newMeeting, title: e.target.value })}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Date *
                  </label>
                  <input
                    type="date"
                    className="input"
                    value={newMeeting.date}
                    onChange={e => setNewMeeting({ ...newMeeting, date: e.target.value })}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Time
                  </label>
                  <input
                    type="time"
                    className="input"
                    value={newMeeting.time}
                    onChange={e => setNewMeeting({ ...newMeeting, time: e.target.value })}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Duration
                  </label>
                  <select
                    className="input"
                    value={newMeeting.duration}
                    onChange={e => setNewMeeting({ ...newMeeting, duration: Number(e.target.value) })}
                  >
                    <option value={15}>15 mins</option>
                    <option value={30}>30 mins</option>
                    <option value={45}>45 mins</option>
                    <option value={60}>60 mins</option>
                    <option value={90}>90 mins</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Meeting Type
                  </label>
                  <select
                    className="input"
                    value={newMeeting.type}
                    onChange={e => setNewMeeting({ ...newMeeting, type: e.target.value as Meeting['type'] })}
                  >
                    <option value="internal">Internal Sync</option>
                    <option value="investor">Investor Pitch / Diligence</option>
                    <option value="external">Client / OEM Partner</option>
                    <option value="mentor">Advisory / Mentor</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    Related Initiative
                  </label>
                  <select
                    className="input"
                    value={newMeeting.projectId}
                    onChange={e => setNewMeeting({ ...newMeeting, projectId: e.target.value })}
                  >
                    <option value="">General / Company-wide</option>
                    {projects.map(p => (
                      <option key={p.id} value={p.id}>{p.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Select Attendees
                </label>
                <div style={{
                  display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.375rem',
                  maxHeight: '120px', overflowY: 'auto', padding: '0.5rem',
                  background: 'var(--bg-elevated)', borderRadius: 8, border: '1px solid var(--border)'
                }}>
                  {team.map(m => {
                    const isChecked = newMeeting.participants.includes(m.id);
                    return (
                      <label key={m.id} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => {
                            const updated = isChecked
                              ? newMeeting.participants.filter(id => id !== m.id)
                              : [...newMeeting.participants, m.id];
                            setNewMeeting({ ...newMeeting, participants: updated });
                          }}
                        />
                        <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{m.name}</span>
                      </label>
                    );
                  })}
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                  Agenda (one point per line)
                </label>
                <textarea
                  className="input"
                  rows={2}
                  placeholder="Key discussion topics..."
                  value={newMeeting.agenda}
                  onChange={e => setNewMeeting({ ...newMeeting, agenda: e.target.value })}
                  style={{ resize: 'vertical' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsAddModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  <Calendar size={14} />
                  Schedule & Invite
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );

  function renderMeetingCard(m: Meeting) {
    const isSelected = expandedId === m.id;
    const typeMeta = TYPE_COLORS[m.type] || TYPE_COLORS.internal;
    const participantMembers = m.participants.map(id => team.find(u => u.id === id)).filter(Boolean);
    const pendingActions = m.actionItems.filter(a => !a.completed).length;

    return (
      <div
        key={m.id}
        onClick={() => setExpandedId(m.id)}
        className="card"
        style={{
          padding: '1rem',
          cursor: 'pointer',
          border: isSelected ? '1px solid var(--accent)' : '1px solid var(--border)',
          background: isSelected ? 'rgba(99, 102, 241, 0.05)' : 'var(--bg-card)',
          transition: 'all 0.15s ease',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
          <span
            style={{
              fontSize: '0.68rem',
              fontWeight: 600,
              padding: '0.15rem 0.5rem',
              borderRadius: 4,
              color: typeMeta.color,
              background: typeMeta.bg,
            }}
          >
            {typeMeta.label}
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.725rem', color: 'var(--text-muted)' }}>
            <Calendar size={12} />
            <span>{m.date}</span>
            <span>·</span>
            <span>{m.time}</span>
          </div>
        </div>

        <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.5rem', lineHeight: 1.3 }}>
          {m.title}
        </h4>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '0.75rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border)' }}>
          {/* Avatars */}
          <div style={{ display: 'flex', alignItems: 'center', marginLeft: 4 }}>
            {participantMembers.slice(0, 4).map((p, idx) => (
              <div
                key={p?.id || idx}
                title={p?.name}
                style={{
                  width: 22, height: 22, borderRadius: '50%',
                  background: '#6366f1', color: '#fff',
                  fontSize: '0.6rem', fontWeight: 700,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  marginLeft: idx === 0 ? 0 : -6,
                  border: '2px solid var(--bg-card)',
                }}
              >
                {p ? getInitials(p.name) : '?'}
              </div>
            ))}
            {participantMembers.length > 4 && (
              <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', marginLeft: 4 }}>
                +{participantMembers.length - 4}
              </span>
            )}
          </div>

          {/* Action items badge */}
          {m.actionItems.length > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.7rem', color: pendingActions > 0 ? '#f59e0b' : '#10b981' }}>
              <ListTodo size={12} />
              <span>{pendingActions} action{pendingActions === 1 ? '' : 's'} pending</span>
            </div>
          )}
        </div>
      </div>
    );
  }

  function renderMeetingDetail() {
    const meeting = meetings.find(m => m.id === expandedId);
    if (!meeting) {
      return (
        <div className="card" style={{ padding: '2.5rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          Select a meeting from the list to view agenda, notes, and action items.
        </div>
      );
    }

    const typeMeta = TYPE_COLORS[meeting.type] || TYPE_COLORS.internal;
    const participantMembers = meeting.participants.map(id => team.find(u => u.id === id)).filter(Boolean);

    return (
      <div className="card animate-fade-in" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {/* Title Header */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <span
              style={{
                fontSize: '0.72rem',
                fontWeight: 600,
                padding: '0.2rem 0.6rem',
                borderRadius: 4,
                color: typeMeta.color,
                background: typeMeta.bg,
              }}
            >
              {typeMeta.label}
            </span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              {formatDate(meeting.date)} · {meeting.time} ({meeting.duration} mins)
            </span>
          </div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)', lineHeight: 1.3 }}>
            {meeting.title}
          </h2>
        </div>

        {/* Participants */}
        <div>
          <h4 style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>
            Participants ({participantMembers.length})
          </h4>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
            {participantMembers.map(p => (
              <div
                key={p?.id}
                style={{
                  display: 'flex', alignItems: 'center', gap: '0.4rem',
                  padding: '0.3rem 0.6rem', borderRadius: 'var(--radius)',
                  background: 'var(--bg-elevated)', border: '1px solid var(--border)',
                  fontSize: '0.75rem'
                }}
              >
                <div style={{ width: 18, height: 18, borderRadius: '50%', background: '#6366f1', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.55rem', fontWeight: 700 }}>
                  {p ? getInitials(p.name) : '?'}
                </div>
                <span>{p?.name}</span>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>({p?.role})</span>
              </div>
            ))}
          </div>
        </div>

        {/* Agenda */}
        {meeting.agenda.length > 0 && (
          <div>
            <h4 style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>
              Agenda Topics
            </h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
              {meeting.agenda.map((item, idx) => (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                  <div style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--accent)' }} />
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Meeting Notes / Executive Summary */}
        <div>
          <h4 style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>
            Meeting Notes & Key Takeaways
          </h4>
          <div style={{
            background: 'var(--bg-elevated)', border: '1px solid var(--border)',
            borderRadius: 'var(--radius)', padding: '1rem', fontSize: '0.825rem',
            color: 'var(--text-secondary)', lineHeight: 1.6, whiteSpace: 'pre-line'
          }}>
            {meeting.notes}
          </div>
        </div>

        {/* Action Items — with Convert to Task feature */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.625rem' }}>
            <h4 style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Action Items ({meeting.actionItems.length})
            </h4>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
              Auto-syncs into global task Kanban
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {meeting.actionItems.map(item => {
              const assignee = team.find(m => m.id === item.assigneeId);

              return (
                <div
                  key={item.id}
                  style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    padding: '0.75rem', borderRadius: 'var(--radius)',
                    background: item.completed ? 'rgba(16, 185, 129, 0.05)' : 'var(--bg-elevated)',
                    border: `1px solid ${item.completed ? 'rgba(16, 185, 129, 0.2)' : 'var(--border)'}`,
                    gap: '0.75rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', flex: 1, minWidth: 0 }}>
                    <CheckCircle2
                      size={16}
                      style={{ color: item.completed ? '#10b981' : 'var(--text-muted)', flexShrink: 0 }}
                    />
                    <div style={{ minWidth: 0 }}>
                      <div style={{
                        fontSize: '0.825rem', fontWeight: 500,
                        color: item.completed ? 'var(--text-muted)' : 'var(--text-primary)',
                        textDecoration: item.completed ? 'line-through' : 'none'
                      }}>
                        {item.description}
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                        <span>Assigned to: <strong>{assignee?.name || 'Unassigned'}</strong></span>
                        <span>·</span>
                        <span>Due: {item.dueDate}</span>
                      </div>
                    </div>
                  </div>

                  {/* Convert to Task CTA or Converted Badge */}
                  <div>
                    {item.convertedToTask ? (
                      <span className="badge" style={{ background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8', fontSize: '0.7rem', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                        <Check size={11} /> Converted to Task
                      </span>
                    ) : (
                      <button
                        onClick={() => handleConvertToTask(meeting.id, item.id, item.description)}
                        className="btn btn-sm btn-primary"
                        style={{ fontSize: '0.72rem', padding: '0.3rem 0.6rem', whiteSpace: 'nowrap' }}
                      >
                        <Sparkles size={12} />
                        Convert to Task
                      </button>
                    )}
                  </div>
                </div>
              );
            })}

            {meeting.actionItems.length === 0 && (
              <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                No action items recorded for this meeting.
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }
}
