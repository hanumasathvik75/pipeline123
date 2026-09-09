'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import { Hash, Plus, MessageSquare, UserPlus } from 'lucide-react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';

export default function ChatSidebar() {
  const { chatChannels, currentUser, team, createNewChatChannel } = useHQStore();
  const pathname = usePathname();
  const router = useRouter();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDMModal, setShowDMModal] = useState(false);
  const [newChannelName, setNewChannelName] = useState('');

  const groups = chatChannels.filter(c => c.type === 'group');
  const dms = chatChannels.filter(c => c.type === 'direct');

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newChannelName.trim()) return;
    
    // In a real app, you'd select members. For now, we'll just add everyone.
    const allMemberIds = team.map(t => t.id);
    const res = await createNewChatChannel(newChannelName, '', 'group', allMemberIds);
    if (res.success && res.channel) {
      router.push(`/chat/${res.channel.id}`);
    }
    setShowCreateModal(false);
    setNewChannelName('');
  };

  const handleStartDM = async (memberId: string) => {
    // Check if DM already exists
    const existingDM = dms.find(c => c.memberIds.includes(memberId));
    if (existingDM) {
      setShowDMModal(false);
      router.push(`/chat/${existingDM.id}`);
      return;
    }
    // Create new DM
    const res = await createNewChatChannel('dm', '', 'direct', [memberId]);
    if (res.success && res.channel) {
      setShowDMModal(false);
      router.push(`/chat/${res.channel.id}`);
    }
  };

  return (
    <div style={{ 
      width: '250px', borderRight: '1px solid var(--border)', 
      background: 'var(--bg-surface)', display: 'flex', flexDirection: 'column' 
    }}>
      <div style={{ padding: '1rem', borderBottom: '1px solid var(--border)', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <MessageSquare size={18} /> Chat
      </div>
      
      <div style={{ flex: 1, overflowY: 'auto', padding: '1rem 0' }}>
        <div style={{ marginBottom: '1.5rem' }}>
          <div style={{ 
            padding: '0 1rem', fontSize: '0.7rem', fontWeight: 700, 
            color: 'var(--text-muted)', textTransform: 'uppercase', 
            marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
          }}>
            Channels
            {currentUser?.role === 'c_level' && (
              <button onClick={() => setShowCreateModal(true)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)' }}>
                <Plus size={14} />
              </button>
            )}
          </div>
          {groups.map(c => (
            <Link 
              key={c.id} 
              href={`/chat/${c.id}`}
              style={{
                display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem',
                textDecoration: 'none', color: pathname === `/chat/${c.id}` ? 'var(--primary)' : 'var(--text-primary)',
                background: pathname === `/chat/${c.id}` ? 'var(--surface-hover)' : 'transparent',
                fontWeight: pathname === `/chat/${c.id}` ? 600 : 400,
                fontSize: '0.9rem'
              }}
            >
              <Hash size={16} /> {c.name}
            </Link>
          ))}
        </div>

        <div>
          <div style={{ 
            padding: '0 1rem', fontSize: '0.7rem', fontWeight: 700, 
            color: 'var(--text-muted)', textTransform: 'uppercase', 
            marginBottom: '0.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center'
          }}>
            Direct Messages
            <button onClick={() => setShowDMModal(true)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)' }}>
              <Plus size={14} />
            </button>
          </div>
          {dms.map(c => {
            const otherMemberId = c.memberIds.find(id => id !== currentUser?.id);
            const otherMember = team.find(t => t.id === otherMemberId);
            return (
              <Link 
                key={c.id} 
                href={`/chat/${c.id}`}
                style={{
                  display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem',
                  textDecoration: 'none', color: pathname === `/chat/${c.id}` ? 'var(--primary)' : 'var(--text-primary)',
                  background: pathname === `/chat/${c.id}` ? 'var(--surface-hover)' : 'transparent',
                  fontWeight: pathname === `/chat/${c.id}` ? 600 : 400,
                  fontSize: '0.9rem'
                }}
              >
                <div style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--success)' }}></div>
                {otherMember?.name || 'Unknown'}
              </Link>
            )
          })}
        </div>
      </div>

      {showCreateModal && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)', backdropFilter: 'blur(4px)' }} onClick={() => setShowCreateModal(false)} />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 400, padding: '2rem', zIndex: 1 }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1.5rem' }}>Create Channel</h2>
            <form onSubmit={handleCreateGroup} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Channel Name</label>
                <input
                  className="input"
                  type="text"
                  value={newChannelName}
                  onChange={e => setNewChannelName(e.target.value)}
                  placeholder="e.g. engineering"
                  required
                />
              </div>
              <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem' }}>
                <button type="button" onClick={() => setShowCreateModal(false)} className="btn btn-ghost" style={{ flex: 1 }}>Cancel</button>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Create</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showDMModal && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)', backdropFilter: 'blur(4px)' }} onClick={() => setShowDMModal(false)} />
          <div className="card" style={{ position: 'relative', width: '100%', maxWidth: 400, padding: '2rem', zIndex: 1, maxHeight: '80vh', display: 'flex', flexDirection: 'column' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1.5rem' }}>New Message</h2>
            <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {team.filter(t => t.id !== currentUser?.id).map(member => (
                <div 
                  key={member.id} 
                  onClick={() => handleStartDM(member.id)}
                  style={{ 
                    padding: '0.75rem', borderRadius: '8px', cursor: 'pointer',
                    display: 'flex', alignItems: 'center', gap: '0.75rem',
                    background: 'var(--bg-default)', border: '1px solid var(--border)'
                  }}
                  className="hover-bg"
                >
                  <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'var(--primary)', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.8rem', fontWeight: 600 }}>
                    {member.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div style={{ fontWeight: 600 }}>{member.name}</div>
                </div>
              ))}
              {team.filter(t => t.id !== currentUser?.id).length === 0 && (
                <div style={{ color: 'var(--text-muted)', fontSize: '0.9rem', textAlign: 'center', padding: '1rem 0' }}>
                  No other team members found.
                </div>
              )}
            </div>
            <button onClick={() => setShowDMModal(false)} className="btn btn-ghost" style={{ marginTop: '1rem' }}>Cancel</button>
          </div>
        </div>
      )}
    </div>
  );
}
