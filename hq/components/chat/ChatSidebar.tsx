'use client';

import { useState, useEffect } from 'react';
import { useHQStore } from '@/lib/store';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';

export default function ChatSidebar() {
  const { chatChannels, currentUser, team, createNewChatChannel, fetchChatChannels, deleteChatChannel } = useHQStore();
  const pathname = usePathname();
  const router = useRouter();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDMModal, setShowDMModal] = useState(false);
  const [newChannelName, setNewChannelName] = useState('');
  const [newChannelDesc, setNewChannelDesc] = useState('');
  const [dmSearch, setDmSearch] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [sidebarError, setSidebarError] = useState<string | null>(null);

  // Poll channels every 5 seconds and fetch on mount
  useEffect(() => {
    if (currentUser?.id) {
      fetchChatChannels();
    }
    const interval = setInterval(() => {
      if (currentUser?.id) {
        fetchChatChannels();
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [currentUser?.id, fetchChatChannels]);

  // Ensure team is loaded for DM selection
  useEffect(() => {
    if (team.length === 0) {
      fetch('/api/users')
        .then(res => res.json())
        .then(data => {
          if (data.users && Array.isArray(data.users)) {
            useHQStore.setState({ team: data.users });
          }
        })
        .catch(err => console.error('Failed to load team members:', err));
    }
  }, [team.length]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await fetchChatChannels();
    setTimeout(() => setIsRefreshing(false), 600);
  };

  const groups = chatChannels.filter(c => c.type === 'group');
  const dms = chatChannels.filter(c => c.type === 'direct');

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newChannelName.trim()) return;

    setIsSubmitting(true);
    setSidebarError(null);

    const allMemberIds = team.map(t => t.id);
    const res = await createNewChatChannel(newChannelName, newChannelDesc, 'group', allMemberIds);
    setIsSubmitting(false);

    if (res.success && res.channel) {
      setShowCreateModal(false);
      setNewChannelName('');
      setNewChannelDesc('');
      router.push(`/chat/${res.channel.id}`);
    } else {
      const errMsg = res.error || 'Failed to create channel';
      setSidebarError(errMsg);
      alert(`Channel creation failed: ${errMsg}`);
    }
  };

  const handleStartDM = async (memberId: string) => {
    setIsSubmitting(true);
    setSidebarError(null);

    // Check if DM already exists with this member
    const existingDM = dms.find(c => c.memberIds.includes(memberId));
    if (existingDM) {
      setIsSubmitting(false);
      setShowDMModal(false);
      router.push(`/chat/${existingDM.id}`);
      return;
    }

    // Create new DM
    const targetMember = team.find(t => t.id === memberId);
    const dmTitle = targetMember ? `dm-${targetMember.name.toLowerCase().replace(/\s+/g, '-')}` : 'direct-message';
    const res = await createNewChatChannel(dmTitle, '', 'direct', [memberId]);
    setIsSubmitting(false);

    if (res.success && res.channel) {
      setShowDMModal(false);
      router.push(`/chat/${res.channel.id}`);
    } else {
      const errMsg = res.error || 'Failed to start direct message';
      setSidebarError(errMsg);
      alert(`Failed to start DM: ${errMsg}`);
    }
  };

  const handleDeleteChannel = async (e: React.MouseEvent, channelId: string, channelName: string) => {
    e.preventDefault();
    e.stopPropagation();

    if (!window.confirm(`Delete channel "${channelName}"?`)) return;

    const res = await deleteChatChannel(channelId);
    if (res.success) {
      if (pathname === `/chat/${channelId}`) {
        router.push('/chat');
      }
    } else {
      alert(res.error || 'Failed to delete channel');
    }
  };

  const filteredTeam = team.filter(t => 
    t.id !== currentUser?.id && 
    (t.name.toLowerCase().includes(dmSearch.toLowerCase()) || 
     (t.designation && t.designation.toLowerCase().includes(dmSearch.toLowerCase())))
  );

  return (
    <div style={{ 
      width: '100%', 
      background: 'var(--bg-surface)', display: 'flex', flexDirection: 'column',
      height: '100%'
    }}>
      {/* Header */}
      <div style={{ 
        padding: '0.875rem 1rem', borderBottom: '1px solid var(--border)', 
        fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'space-between' 
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1rem' }}>
          <></>
          <span>Ignition Chat</span>
        </div>
        <button 
          onClick={handleRefresh} 
          title="Refresh channels"
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', padding: '6px' }}
        >
          <></>
        </button>
      </div>

      {sidebarError && (
        <div style={{ 
          margin: '0.5rem', padding: '0.5rem 0.75rem', background: 'rgba(0, 0, 0, 0.1)', 
          border: '1px solid rgba(0, 0, 0, 0.3)', borderRadius: '6px', fontSize: '0.75rem', 
          color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.375rem' 
        }}>
          <></>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{sidebarError}</span>
        </div>
      )}

      {/* Channel list */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '0.75rem 0' }}>
        
        {/* GROUPS / CHANNELS */}
        <div style={{ marginBottom: '1.5rem' }}>
          <div style={{ 
            padding: '0 1rem', fontSize: '0.7rem', fontWeight: 700, 
            color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em',
            marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
          }}>
            <span>Channels ({groups.length})</span>
            {currentUser?.role === 'c_level' && (
              <button 
                onClick={() => setShowCreateModal(true)} 
                title="Create Group Channel"
                style={{ 
                  background: 'var(--surface-hover)', border: '1px solid var(--border)', 
                  cursor: 'pointer', color: 'var(--text-primary)', borderRadius: '4px',
                  display: 'flex', alignItems: 'center', gap: '2px', padding: '3px 8px', fontSize: '0.7rem', fontWeight: 600
                }}
              >
                <></> Add
              </button>
            )}
          </div>

          {groups.length === 0 ? (
            <div style={{ padding: '0.5rem 1rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              No channels yet.
              {currentUser?.role === 'c_level' && (
                <div style={{ marginTop: '0.375rem' }}>
                  <button 
                    onClick={() => setShowCreateModal(true)}
                    className="btn btn-sm btn-ghost"
                    style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                  >
                    + Create #general
                  </button>
                </div>
              )}
            </div>
          ) : (
            groups.map(c => {
              const isActive = pathname === `/chat/${c.id}`;
              const canDelete = currentUser?.role === 'c_level' || c.createdBy === currentUser?.id;

              return (
                <div 
                  key={c.id} 
                  style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    padding: '0.2rem 0.5rem 0.2rem 0',
                    background: isActive ? 'var(--surface-hover)' : 'transparent',
                    borderLeft: isActive ? '3px solid var(--primary)' : '3px solid transparent',
                  }}
                  className="hover-bg"
                >
                  <Link 
                    href={`/chat/${c.id}`}
                    style={{
                      display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.45rem 0.75rem',
                      textDecoration: 'none', flex: 1, minWidth: 0,
                      color: isActive ? 'var(--primary)' : 'var(--text-primary)',
                      fontWeight: isActive ? 600 : 400,
                      fontSize: '0.875rem'
                    }}
                  >
                    <></> 
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.name}</span>
                  </Link>

                  {canDelete && (
                    <button
                      onClick={(e) => handleDeleteChannel(e, c.id, c.name)}
                      title="Delete channel"
                      style={{
                        background: 'none', border: 'none', cursor: 'pointer',
                        color: 'var(--text-muted)', padding: '6px', borderRadius: '4px',
                        display: 'flex', alignItems: 'center', opacity: 0.7
                      }}
                      className="hover-bg"
                    >
                      <></>
                    </button>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* DIRECT MESSAGES */}
        <div>
          <div style={{ 
            padding: '0 1rem', fontSize: '0.7rem', fontWeight: 700, 
            color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em',
            marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
          }}>
            <span>Direct Messages</span>
            <button 
              onClick={() => setShowDMModal(true)} 
              title="New Direct Message"
              style={{ 
                background: 'var(--surface-hover)', border: '1px solid var(--border)', 
                cursor: 'pointer', color: 'var(--text-primary)', borderRadius: '4px',
                display: 'flex', alignItems: 'center', gap: '2px', padding: '3px 8px', fontSize: '0.7rem', fontWeight: 600
              }}
            >
              <></> New DM
            </button>
          </div>

          {dms.length === 0 ? (
            <div style={{ padding: '0.5rem 1rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              No messages yet.
              <div style={{ marginTop: '0.375rem' }}>
                <button 
                  onClick={() => setShowDMModal(true)}
                  className="btn btn-sm btn-ghost"
                  style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                >
                  + Message someone
                </button>
              </div>
            </div>
          ) : (
            dms.map(c => {
              const otherMemberId = c.memberIds.find(id => id !== currentUser?.id) || c.createdBy;
              const otherMember = team.find(t => t.id === otherMemberId);
              const isActive = pathname === `/chat/${c.id}`;
              const displayName = otherMember?.name || (otherMemberId === currentUser?.id ? `${currentUser?.name} (You)` : 'Team Member');

              return (
                <div
                  key={c.id}
                  style={{
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    padding: '0.2rem 0.5rem 0.2rem 0',
                    background: isActive ? 'var(--surface-hover)' : 'transparent',
                    borderLeft: isActive ? '3px solid var(--primary)' : '3px solid transparent',
                  }}
                  className="hover-bg"
                >
                  <Link 
                    href={`/chat/${c.id}`}
                    style={{
                      display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.45rem 0.75rem',
                      textDecoration: 'none', flex: 1, minWidth: 0,
                      color: isActive ? 'var(--primary)' : 'var(--text-primary)',
                      fontWeight: isActive ? 600 : 400,
                      fontSize: '0.875rem'
                    }}
                  >
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--success)', flexShrink: 0 }} />
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {displayName}
                    </span>
                  </Link>

                  <button
                    onClick={(e) => handleDeleteChannel(e, c.id, displayName)}
                    title="Delete conversation"
                    style={{
                      background: 'none', border: 'none', cursor: 'pointer',
                      color: 'var(--text-muted)', padding: '6px', borderRadius: '4px',
                      display: 'flex', alignItems: 'center', opacity: 0.7
                    }}
                    className="hover-bg"
                  >
                    <></>
                  </button>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* CREATE CHANNEL MODAL */}
      {showCreateModal && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }} onClick={() => setShowCreateModal(false)} />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 420, padding: '1.75rem', zIndex: 1 }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <></> Create Group Channel
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
              Channels are public spaces where your team can communicate.
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
                    placeholder="general, engineering, marketing"
                    style={{ paddingLeft: '24px' }}
                    required
                    autoFocus
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>
                  Description (optional)
                </label>
                <input
                  className="input"
                  type="text"
                  value={newChannelDesc}
                  onChange={e => setNewChannelDesc(e.target.value)}
                  placeholder="What is this channel about?"
                />
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" onClick={() => setShowCreateModal(false)} className="btn btn-ghost" style={{ flex: 1 }} disabled={isSubmitting}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }} disabled={isSubmitting || !newChannelName.trim()}>
                  {isSubmitting ? 'Creating...' : 'Create Channel'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* NEW DIRECT MESSAGE MODAL */}
      {showDMModal && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }} onClick={() => setShowDMModal(false)} />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 440, padding: '1.75rem', zIndex: 1, maxHeight: '80vh', display: 'flex', flexDirection: 'column' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <></> Start a Direct Message
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
              Choose a team member to start a 1-on-1 private conversation.
            </p>

            <div style={{ position: 'relative', marginBottom: '1rem' }}>
              <></>
              <input
                className="input"
                type="text"
                value={dmSearch}
                onChange={e => setDmSearch(e.target.value)}
                placeholder="Search by name or role..."
                style={{ paddingLeft: '32px' }}
                autoFocus
              />
            </div>

            <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.5rem', minHeight: '180px' }}>
              {filteredTeam.map(member => (
                <div 
                  key={member.id} 
                  onClick={() => handleStartDM(member.id)}
                  style={{ 
                    padding: '0.75rem', borderRadius: '8px', cursor: 'pointer',
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    background: 'var(--bg-default)', border: '1px solid var(--border)'
                  }}
                  className="hover-bg"
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div style={{ 
                      width: 36, height: 36, borderRadius: '50%', 
                      background: 'var(--primary)', color: 'white', 
                      display: 'flex', alignItems: 'center', justifyContent: 'center', 
                      fontSize: '0.85rem', fontWeight: 700 
                    }}>
                      {member.name.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{member.name}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{member.designation || member.role}</div>
                    </div>
                  </div>
                  <button className="btn btn-sm btn-ghost" style={{ fontSize: '0.75rem' }}>Message</button>
                </div>
              ))}

              {filteredTeam.length === 0 && (
                <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '2rem 0' }}>
                  {dmSearch ? 'No team members match your search.' : 'No other active team members found.'}
                </div>
              )}
            </div>

            <button onClick={() => setShowDMModal(false)} className="btn btn-ghost" style={{ marginTop: '1rem' }}>
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
