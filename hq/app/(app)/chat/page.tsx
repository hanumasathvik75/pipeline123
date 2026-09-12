'use client';

import { useState, useEffect } from 'react';
import { useHQStore } from '@/lib/store';
import { MessageSquare, Plus, Hash, Users, ArrowRight, Sparkles } from 'lucide-react';
import { useRouter } from 'next/navigation';

export default function ChatPage() {
  const { chatChannels, currentUser, team, createNewChatChannel, fetchChatChannels } = useHQStore();
  const router = useRouter();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newChannelName, setNewChannelName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const groups = chatChannels.filter(c => c.type === 'group');
  const dms = chatChannels.filter(c => c.type === 'direct');

  useEffect(() => {
    if (currentUser?.id) {
      fetchChatChannels();
    }
  }, [currentUser?.id, fetchChatChannels]);

  const handleStartDM = async (memberId: string) => {
    const existingDM = dms.find(c => c.memberIds.includes(memberId));
    if (existingDM) {
      router.push(`/chat/${existingDM.id}`);
      return;
    }

    const member = team.find(t => t.id === memberId);
    const dmTitle = member ? `dm-${member.name.toLowerCase().replace(/\s+/g, '-')}` : 'direct-message';
    const res = await createNewChatChannel(dmTitle, '', 'direct', [memberId]);
    if (res.success && res.channel) {
      router.push(`/chat/${res.channel.id}`);
    } else {
      alert(res.error || 'Failed to start direct message');
    }
  };

  const handleCreateQuickGeneral = async () => {
    setIsSubmitting(true);
    const allMemberIds = team.map(t => t.id);
    const res = await createNewChatChannel('general', 'Company-wide discussion and announcements', 'group', allMemberIds);
    setIsSubmitting(false);
    if (res.success && res.channel) {
      router.push(`/chat/${res.channel.id}`);
    } else {
      alert(res.error || 'Failed to create #general');
    }
  };

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newChannelName.trim()) return;
    setIsSubmitting(true);
    const allMemberIds = team.map(t => t.id);
    const res = await createNewChatChannel(newChannelName, '', 'group', allMemberIds);
    setIsSubmitting(false);
    if (res.success && res.channel) {
      setShowCreateModal(false);
      setNewChannelName('');
      router.push(`/chat/${res.channel.id}`);
    } else {
      alert(res.error || 'Failed to create channel');
    }
  };

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '1rem', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ maxWidth: '720px', width: '100%' }}>
        {/* Welcome Banner */}
        <div style={{ 
          background: 'linear-gradient(135deg, rgba(0, 0, 0, 0.08) 0%, rgba(0, 0, 0, 0.04) 100%)',
          border: '1px solid rgba(0, 0, 0, 0.2)',
          borderRadius: '16px',
          padding: '1.5rem 1rem',
          marginBottom: '1.5rem',
          textAlign: 'center'
        }}>
          <div style={{ 
            width: 44, height: 44, borderRadius: '12px', background: 'var(--primary)', 
            color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', 
            margin: '0 auto 0.75rem auto' 
          }}>
            <MessageSquare size={22} />
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '0.375rem', letterSpacing: '-0.02em' }}>
            Ignition HQ Chat
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '480px', margin: '0 auto 1.25rem auto', lineHeight: 1.4 }}>
            Message colleagues directly, talk in company channels, and reference Tasks, Projects, and Wiki docs using <code style={{ background: 'var(--surface-hover)', padding: '2px 5px', borderRadius: '4px' }}>/</code>.
          </p>

          <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            {currentUser?.role === 'c_level' && (
              <button 
                onClick={() => setShowCreateModal(true)} 
                className="btn btn-primary"
                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', padding: '8px 16px' }}
              >
                <Plus size={16} /> Create Channel
              </button>
            )}
            {groups.length === 0 && currentUser?.role === 'c_level' && (
              <button 
                onClick={handleCreateQuickGeneral}
                disabled={isSubmitting}
                className="btn btn-primary"
                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', padding: '8px 16px' }}
              >
                <Sparkles size={16} /> Quick Setup #general
              </button>
            )}
          </div>
        </div>

        {/* Existing Channels */}
        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Hash size={17} className="text-primary" /> Group Channels ({groups.length})
            </h2>
            {currentUser?.role === 'c_level' && (
              <button 
                onClick={() => setShowCreateModal(true)}
                className="btn btn-sm btn-ghost"
                style={{ fontSize: '0.75rem' }}
              >
                + New
              </button>
            )}
          </div>

          {groups.length === 0 ? (
            <div className="card" style={{ padding: '1.25rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              No group channels created yet. {currentUser?.role === 'c_level' ? 'Click "Create Channel" to start one!' : 'A founder or C-level executive can create channels.'}
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '0.625rem' }}>
              {groups.map(c => (
                <div 
                  key={c.id} 
                  onClick={() => router.push(`/chat/${c.id}`)}
                  className="card hover-bg"
                  style={{ 
                    padding: '0.875rem 1rem', cursor: 'pointer', display: 'flex', 
                    alignItems: 'center', justifyContent: 'space-between',
                    border: '1px solid var(--border)' 
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', overflow: 'hidden' }}>
                    <Hash size={16} style={{ color: 'var(--primary)', flexShrink: 0 }} />
                    <span style={{ fontWeight: 600, fontSize: '0.875rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {c.name}
                    </span>
                  </div>
                  <ArrowRight size={14} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Team Direct Messaging */}
        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <h2 style={{ fontSize: '1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Users size={17} className="text-primary" /> Direct Message Colleagues
            </h2>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{team.length} members</span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '0.625rem' }}>
            {team.filter(t => t.id !== currentUser?.id).map(member => (
              <div 
                key={member.id} 
                onClick={() => handleStartDM(member.id)}
                className="card hover-bg"
                style={{ 
                  padding: '0.75rem 0.875rem', cursor: 'pointer', display: 'flex', 
                  alignItems: 'center', justifyContent: 'space-between',
                  border: '1px solid var(--border)' 
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', overflow: 'hidden' }}>
                  <div style={{ 
                    width: 30, height: 30, borderRadius: '50%', background: 'var(--primary)', 
                    color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', 
                    fontSize: '0.75rem', fontWeight: 700, flexShrink: 0 
                  }}>
                    {member.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div style={{ overflow: 'hidden' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.825rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {member.name}
                    </div>
                    <div style={{ fontSize: '0.675rem', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {member.designation || member.role}
                    </div>
                  </div>
                </div>
                <button className="btn btn-sm btn-ghost" style={{ fontSize: '0.7rem', padding: '2px 6px', flexShrink: 0 }}>Chat</button>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* CREATE MODAL */}
      {showCreateModal && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }} onClick={() => setShowCreateModal(false)} />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 400, padding: '1.5rem', zIndex: 1 }}>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Hash size={18} className="text-primary" /> Create Group Channel
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
              Group channels can be joined and viewed by all members in HQ.
            </p>

            <form onSubmit={handleCreateGroup} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>
                  Channel Name
                </label>
                <div style={{ position: 'relative' }}>
                  <span style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>#</span>
                  <input
                    className="input"
                    type="text"
                    value={newChannelName}
                    onChange={e => setNewChannelName(e.target.value)}
                    placeholder="general, dev, marketing"
                    style={{ paddingLeft: '24px' }}
                    required
                    autoFocus
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" onClick={() => setShowCreateModal(false)} className="btn btn-ghost" style={{ flex: 1 }} disabled={isSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={isSubmitting || !newChannelName.trim()}>
                  {isSubmitting ? 'Creating...' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
