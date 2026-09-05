'use client';

import { useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { useHQStore } from '@/lib/store';
import {
  Search, Bell, Plus, ChevronDown, X, CheckSquare, FolderKanban,
  Calendar, Building2, TrendingUp, Sparkles, Menu
} from 'lucide-react';
import { getInitials } from '@/lib/utils';
import type { Priority, ProjectStatus, LeadStage, InvestorStage } from '@/lib/types';

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'Command Center',
  '/projects': 'Projects',
  '/tasks': 'Tasks',
  '/crm': 'Business CRM',
  '/fundraising': 'Fundraising',
  '/meetings': 'Meetings',
  '/rd': 'R&D Workspace',
  '/team': 'Team',
  '/goals': 'Goals & OKRs',
  '/wiki': 'Knowledge Base',
  '/finance': 'Finance',
};

type QuickTab = 'task' | 'project' | 'meeting' | 'lead' | 'investor';

export default function TopBar() {
  const pathname = usePathname();
  const router = useRouter();
  const {
    currentUser, projects, team,
    addTask, addProject, addMeeting, addLead, addInvestor
  } = useHQStore();

  const [search, setSearch] = useState('');
  const [hasNotif] = useState(true);
  const [isQuickOpen, setIsQuickOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<QuickTab>('task');
  const [toast, setToast] = useState<string | null>(null);

  // Quick form states
  const [taskTitle, setTaskTitle] = useState('');
  const [taskProjId, setTaskProjId] = useState('');
  const [taskPriority, setTaskPriority] = useState<Priority>('high');

  const [projName, setProjName] = useState('');
  const [projCategory, setProjCategory] = useState<'product' | 'rd' | 'bizdev' | 'operations'>('product');

  const [meetingTitle, setMeetingTitle] = useState('');
  const [meetingDate, setMeetingDate] = useState(new Date().toISOString().split('T')[0]);
  const [meetingTime, setMeetingTime] = useState('14:00');

  const [leadCompany, setLeadCompany] = useState('');
  const [leadValue, setLeadValue] = useState('5000000');

  const [invName, setInvName] = useState('');
  const [invFirm, setInvFirm] = useState('');

  const title = PAGE_TITLES[pathname || ''] ?? 'IgnitionAI HQ';

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3500);
  };

  const handleQuickSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (activeTab === 'task') {
      if (!taskTitle.trim()) return;
      const targetProj = taskProjId || projects[0]?.id || 'proj-1';
      addTask({
        id: `task-${Date.now()}`,
        title: taskTitle.trim(),
        description: 'Quick created from HQ Command TopBar',
        projectId: targetProj,
        assigneeId: currentUser?.id || team[0]?.id || 'user-1',
        status: 'planned',
        priority: taskPriority,
        dueDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
        estimatedHours: 4,
        actualHours: 0,
        tags: ['quick-action'],
        subtasks: [],
        dependencies: [],
        deliverables: [],
        createdAt: new Date().toISOString().split('T')[0],
        updatedAt: new Date().toISOString().split('T')[0],
      });
      showToast(`Task "${taskTitle.trim()}" created!`);
      setTaskTitle('');
    } else if (activeTab === 'project') {
      if (!projName.trim()) return;
      addProject({
        name: projName.trim(),
        description: 'Initiative created from HQ Command TopBar',
        category: projCategory,
        priority: 'high',
        status: 'in_progress',
        ownerId: currentUser?.id || team[0]?.id || 'user-1',
        memberIds: [currentUser?.id || team[0]?.id || 'user-1'],
        startDate: new Date().toISOString().split('T')[0],
        targetDate: new Date(Date.now() + 30 * 86400000).toISOString().split('T')[0],
        progress: 10,
        objectives: ['Initial deployment and operational validation'],
        milestones: [],
        risks: [],
        tags: [projCategory, 'active'],
      });
      showToast(`Project "${projName.trim()}" created!`);
      setProjName('');
    } else if (activeTab === 'meeting') {
      if (!meetingTitle.trim()) return;
      addMeeting({
        title: meetingTitle.trim(),
        date: meetingDate,
        time: meetingTime,
        duration: 30,
        type: 'internal',
        participants: [currentUser?.id || team[0]?.id || 'user-1'],
        agenda: ['Strategic review and unblocking execution items'],
        notes: 'Session created via Quick Action.',
        actionItems: [],
      });
      showToast(`Meeting "${meetingTitle.trim()}" scheduled!`);
      setMeetingTitle('');
    } else if (activeTab === 'lead') {
      if (!leadCompany.trim()) return;
      addLead({
        companyName: leadCompany.trim(),
        contactName: 'Executive Contact',
        opportunity: 'Enterprise Telematics / Virtual Sensor Integration',
        potentialValue: Number(leadValue) || 5000000,
        stage: 'lead',
        type: 'Enterprise',
        location: 'Global',
        currency: 'INR',
        contactEmail: '',
        contactPhone: '',
        assignedTo: currentUser?.id || team[0]?.id || 'user-1',
        notes: 'Created via Quick Action.',
        nextFollowUp: new Date(Date.now() + 3 * 86400000).toISOString().split('T')[0],
        activities: [],
      });
      showToast(`Lead "${leadCompany.trim()}" added to CRM!`);
      setLeadCompany('');
    } else if (activeTab === 'investor') {
      if (!invName.trim()) return;
      addInvestor({
        name: invName.trim(),
        firm: invFirm.trim() || 'Venture Partner',
        ticketSize: '$500K - $1M',
        stage: 'research',
        round: 'Seed',
        investmentThesis: 'Physical-AI telematics platform',
        nextAction: 'Intro Deck send',
        nextActionDate: new Date(Date.now() + 2 * 86400000).toISOString().split('T')[0],
        notes: 'Added from Quick Action',
        lastContact: new Date().toISOString().split('T')[0],
        interactions: [],
      });
      showToast(`Investor "${invName.trim()}" added to pipeline!`);
      setInvName('');
      setInvFirm('');
    }

    setIsQuickOpen(false);
  };

  return (
    <>
      <header className="topbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1 }}>
          <button 
            className="btn btn-ghost btn-icon mobile-only" 
            onClick={() => useHQStore.getState().toggleSidebar()}
            style={{ marginRight: '0.25rem' }}
          >
            <Menu size={18} />
          </button>
          <h2 style={{ fontSize: '0.95rem', fontWeight: 700, letterSpacing: '-0.02em', whiteSpace: 'nowrap' }}>
            {title}
          </h2>
          <span className="desktop-only" style={{ color: 'var(--border-hover)', userSelect: 'none' }}>/</span>
          <span className="desktop-only" style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>IgnitionAI HQ</span>
        </div>

        {/* Search */}
        <div className="desktop-only" style={{ position: 'relative', width: 240 }}>
          <Search size={13} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            className="input"
            placeholder="Search anything…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ paddingLeft: '2.25rem', height: 34, fontSize: '0.8rem' }}
          />
          <kbd style={{
            position: 'absolute', right: '0.5rem', top: '50%', transform: 'translateY(-50%)',
            fontSize: '0.6rem', color: 'var(--text-muted)', background: 'var(--bg-hover)',
            border: '1px solid var(--border)', borderRadius: 4, padding: '0 4px', lineHeight: '18px',
          }}>⌘K</kbd>
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <button
            className="btn btn-primary btn-sm"
            onClick={() => setIsQuickOpen(true)}
            style={{ gap: '0.375rem', fontSize: '0.8rem', height: 34, padding: '0 0.875rem' }}
          >
            <Plus size={14} />
            <span className="desktop-only">New</span>
          </button>

          <button
            className="btn btn-ghost btn-icon"
            style={{ position: 'relative', height: 34, width: 34 }}
            onClick={() => showToast('All systems operational. Telemetry stream healthy.')}
          >
            <Bell size={16} />
            {hasNotif && (
              <span style={{
                position: 'absolute', top: 6, right: 6,
                width: 6, height: 6, borderRadius: '50%',
                background: '#ef4444', border: '1.5px solid var(--bg-surface)',
              }} />
            )}
          </button>

          {currentUser && (
            <button
              className="btn btn-ghost"
              onClick={() => router.push('/team')}
              style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.25rem 0.5rem', height: 34, fontSize: '0.8rem' }}
            >
              <div
                className="avatar"
                style={{ width: 24, height: 24, fontSize: '0.6rem', background: 'linear-gradient(135deg, #6366f1, #818cf8)' }}
              >
                {getInitials(currentUser.name)}
              </div>
              <span className="desktop-only" style={{ fontWeight: 600 }}>{currentUser.name}</span>
              <ChevronDown className="desktop-only" size={12} style={{ color: 'var(--text-muted)' }} />
            </button>
          )}
        </div>
      </header>

      {/* Toast */}
      {toast && (
        <div style={{
          position: 'fixed', bottom: '1.5rem', right: '1.5rem',
          background: 'var(--accent)', color: '#fff',
          padding: '0.75rem 1.25rem', borderRadius: 'var(--radius)',
          boxShadow: '0 10px 25px rgba(0,0,0,0.5)', zIndex: 2000,
          display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', fontWeight: 600
        }}>
          <Sparkles size={16} />
          {toast}
        </div>
      )}

      {/* Global Quick Action Modal */}
      {isQuickOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.65)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1500,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 480, padding: '1.5rem', background: 'var(--bg-surface)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>Quick Create</h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Instantly launch an action across the IgnitionAI OS.</p>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsQuickOpen(false)}>
                <X size={16} />
              </button>
            </div>

            {/* Tabs */}
            <div style={{ display: 'flex', gap: '0.375rem', marginBottom: '1.25rem', background: 'var(--bg-elevated)', padding: '0.25rem', borderRadius: 8 }}>
              {[
                { id: 'task', label: 'Task', icon: CheckSquare },
                { id: 'project', label: 'Project', icon: FolderKanban },
                { id: 'meeting', label: 'Meeting', icon: Calendar },
                { id: 'lead', label: 'Lead', icon: Building2 },
                { id: 'investor', label: 'Investor', icon: TrendingUp },
              ].filter(tab => {
                if (currentUser?.role === 'intern') {
                  return ['task', 'lead'].includes(tab.id);
                }
                return true;
              }).map(tab => {
                const Icon = tab.icon;
                const active = activeTab === tab.id;
                return (
                  <button
                    key={tab.id}
                    type="button"
                    onClick={() => setActiveTab(tab.id as QuickTab)}
                    className="btn btn-sm"
                    style={{
                      flex: 1,
                      gap: '0.3rem',
                      fontSize: '0.75rem',
                      height: 30,
                      background: active ? 'var(--bg-card)' : 'transparent',
                      color: active ? 'var(--accent)' : 'var(--text-secondary)',
                      boxShadow: active ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                    }}
                  >
                    <Icon size={13} />
                    {tab.label}
                  </button>
                );
              })}
            </div>

            {/* Form */}
            <form onSubmit={handleQuickSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              {activeTab === 'task' && (
                <>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      Task Title *
                    </label>
                    <input
                      className="input"
                      placeholder="e.g. Optimize virtual sensor Kalman filter"
                      value={taskTitle}
                      onChange={e => setTaskTitle(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>
                  <div className="grid-2">
                    <div>
                      <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                        Project
                      </label>
                      <select className="input" value={taskProjId} onChange={e => setTaskProjId(e.target.value)}>
                        {projects.map(p => (
                          <option key={p.id} value={p.id}>{p.name}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                        Priority
                      </label>
                      <select className="input" value={taskPriority} onChange={e => setTaskPriority(e.target.value as Priority)}>
                        <option value="critical">Critical</option>
                        <option value="high">High</option>
                        <option value="medium">Medium</option>
                        <option value="low">Low</option>
                      </select>
                    </div>
                  </div>
                </>
              )}

              {activeTab === 'project' && (
                <>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      Project Name *
                    </label>
                    <input
                      className="input"
                      placeholder="e.g. OEM Telematics Pilot v2"
                      value={projName}
                      onChange={e => setProjName(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      Category Pillar
                    </label>
                    <select className="input" value={projCategory} onChange={e => setProjCategory(e.target.value as any)}>
                      <option value="product">Product & Engineering</option>
                      <option value="rd">R&D / Physical-AI</option>
                      <option value="bizdev">Business Development</option>
                      <option value="operations">Operations</option>
                    </select>
                  </div>
                </>
              )}

              {activeTab === 'meeting' && (
                <>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      Meeting Topic *
                    </label>
                    <input
                      className="input"
                      placeholder="e.g. Seed VC Partner Sync"
                      value={meetingTitle}
                      onChange={e => setMeetingTitle(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>
                  <div className="grid-2">
                    <div>
                      <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                        Date *
                      </label>
                      <input
                        type="date"
                        className="input"
                        value={meetingDate}
                        onChange={e => setMeetingDate(e.target.value)}
                        required
                      />
                    </div>
                    <div>
                      <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                        Time
                      </label>
                      <input
                        type="time"
                        className="input"
                        value={meetingTime}
                        onChange={e => setMeetingTime(e.target.value)}
                      />
                    </div>
                  </div>
                </>
              )}

              {activeTab === 'lead' && (
                <>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      Company / OEM Name *
                    </label>
                    <input
                      className="input"
                      placeholder="e.g. Tata Motors / Mahindra"
                      value={leadCompany}
                      onChange={e => setLeadCompany(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      Estimated Value (INR)
                    </label>
                    <input
                      type="number"
                      className="input"
                      value={leadValue}
                      onChange={e => setLeadValue(e.target.value)}
                    />
                  </div>
                </>
              )}

              {activeTab === 'investor' && (
                <>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      Investor / Partner Name *
                    </label>
                    <input
                      className="input"
                      placeholder="e.g. Hemant Taneja"
                      value={invName}
                      onChange={e => setInvName(e.target.value)}
                      required
                      autoFocus
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
                      VC Firm
                    </label>
                    <input
                      className="input"
                      placeholder="e.g. General Catalyst"
                      value={invFirm}
                      onChange={e => setInvFirm(e.target.value)}
                    />
                  </div>
                </>
              )}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setIsQuickOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  <Plus size={14} />
                  Create Record
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
