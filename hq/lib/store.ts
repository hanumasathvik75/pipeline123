'use client';

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { 
  Project, Task, Meeting, Lead, Investor, RDProject, TeamMember, 
  UserRole, Experiment, Goal, WikiDoc, CrmContact, ProgressCategory, DailyLog, FinanceData, FundraisingMeta,
  ChatChannel, ChatMessage, ChatReference
} from './types';

interface HQStore {
  // Auth
  currentUser: TeamMember | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<{ success: boolean; error?: string }>;
  logout: () => void;
  changePassword: (currentPassword: string, newPassword: string) => Promise<{ success: boolean; error?: string }>;

  // Data
  projects: Project[];
  tasks: Task[];
  meetings: Meeting[];
  leads: Lead[];
  investors: Investor[];
  rdProjects: RDProject[];
  team: TeamMember[];
  goals: Goal[];
  wikiDocs: WikiDoc[];
  crmContacts: CrmContact[];
  progressCategories: ProgressCategory[];
  progressLogs: DailyLog[];
  pendingUsers: Omit<TeamMember, 'passwordHash'>[];
  financeData: FinanceData | null;
  fundraisingMeta: FundraisingMeta | null;

  // Chat
  chatChannels: ChatChannel[];
  chatMessages: Record<string, ChatMessage[]>; // channelId -> messages
  activeChatChannelId: string | null;
  
  setChatChannels: (channels: ChatChannel[]) => void;
  setChatMessages: (channelId: string, messages: ChatMessage[]) => void;
  setActiveChatChannelId: (id: string | null) => void;
  addChatMessage: (message: ChatMessage) => void;
  addChatChannel: (channel: ChatChannel) => void;

  fetchChatChannels: () => Promise<void>;
  fetchChatMessages: (channelId: string) => Promise<void>;
  sendChatMessage: (content: string, references: ChatReference[], channelId: string) => Promise<{success: boolean, error?: string}>;
  createNewChatChannel: (name: string, description: string, type: 'direct' | 'group', memberIds: string[]) => Promise<{success: boolean, channel?: ChatChannel, error?: string}>;
  deleteChatChannel: (channelId: string) => Promise<{ success: boolean; error?: string }>;
  deleteChatMessage: (messageId: string, channelId: string) => Promise<{ success: boolean; error?: string }>;
  
  // Real-time synchronization
  lastSyncTime: number;
  syncWithServer: () => Promise<void>;
  broadcastUpdates: (partialUpdates?: Record<string, unknown>) => Promise<void>;

  // User management
  fetchPendingUsers: () => Promise<void>;
  approveMember: (id: string, role: UserRole) => Promise<{ success: boolean; error?: string }>;
  rejectMember: (id: string) => Promise<{ success: boolean; error?: string }>;
  updateMember: (id: string, updates: Partial<TeamMember>) => Promise<{ success: boolean; error?: string }>;
  deleteMember: (id: string) => Promise<{ success: boolean; error?: string }>;
  
  // UI state
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  activeModule: string;
  setActiveModule: (m: string) => void;
  mobileMenuOpen: boolean;
  toggleMobileMenu: () => void;
  closeMobileMenu: () => void;

  // CRUD Actions
  // Tasks
  addTask: (task: Task) => void;
  updateTask: (taskId: string, updates: Partial<Task>) => void;
  updateTaskStatus: (taskId: string, status: Task['status']) => void;
  deleteTask: (taskId: string) => void;
  toggleSubtask: (taskId: string, subtaskId: string) => void;
  convertActionItemToTask: (meetingId: string, actionItemId: string) => void;

  // Projects
  addProject: (project: Omit<Project, 'id'>) => void;
  updateProject: (projectId: string, updates: Partial<Project>) => void;
  updateProjectStatus: (projectId: string, status: Project['status']) => void;
  deleteProject: (projectId: string) => void;

  // Meetings
  addMeeting: (meeting: Omit<Meeting, 'id'>) => void;
  updateMeeting: (meetingId: string, updates: Partial<Meeting>) => void;
  deleteMeeting: (meetingId: string) => void;

  // Leads (Legacy)
  addLead: (lead: Omit<Lead, 'id'>) => void;
  updateLeadStage: (leadId: string, stage: Lead['stage']) => void;
  
  // Investors
  addInvestor: (investor: Omit<Investor, 'id'>) => void;
  updateInvestorStage: (investorId: string, stage: Investor['stage']) => void;

  // R&D
  addRDProject: (rdProject: Omit<RDProject, 'id'>) => void;
  addExperiment: (projectId: string, exp: Omit<Experiment, 'id' | 'number'>) => void;

  // Goals
  addGoal: (goal: Omit<Goal, 'id' | 'createdAt'>) => void;
  updateGoal: (goalId: string, updates: Partial<Goal>) => void;
  deleteGoal: (goalId: string) => void;

  // Wiki
  addWikiDoc: (doc: Omit<WikiDoc, 'id' | 'createdAt' | 'updatedAt'>) => void;
  updateWikiDoc: (docId: string, updates: Partial<WikiDoc>) => void;
  deleteWikiDoc: (docId: string) => void;

  // CRM Contacts
  addCrmContact: (contact: Omit<CrmContact, 'id' | 'createdAt' | 'updatedAt'>) => void;
  updateCrmContact: (contactId: string, updates: Partial<CrmContact>) => void;
  deleteCrmContact: (contactId: string) => void;
  importCrmContacts: (contacts: Omit<CrmContact, 'createdAt' | 'updatedAt'>[]) => void;

  // Progress
  addProgressCategory: (category: Omit<ProgressCategory, 'id'>) => void;
  updateProgressCategory: (categoryId: string, updates: Partial<ProgressCategory>) => void;
  deleteProgressCategory: (categoryId: string) => void;
  addProgressLog: (log: Omit<DailyLog, 'id' | 'submittedAt'>) => void;

  // Finance
  updateFinanceData: (updates: Partial<FinanceData>) => Promise<{ success: boolean; error?: string }>;

  // Fundraising
  updateFundraisingMeta: (updates: Partial<FundraisingMeta>) => Promise<{ success: boolean; error?: string }>;
}

export const useHQStore = create<HQStore>()(
  persist(
    (set, get) => ({
      currentUser: null,
      isAuthenticated: false,
      lastSyncTime: 0,
      
      projects: [],
      tasks: [],
      meetings: [],
      leads: [],
      investors: [],
      rdProjects: [],
      team: [],
      goals: [],
      wikiDocs: [],
      crmContacts: [],
      progressCategories: [],
      progressLogs: [],
      pendingUsers: [],
      financeData: null,
      fundraisingMeta: null,

      chatChannels: [],
      chatMessages: {},
      activeChatChannelId: null,

      setChatChannels: (channels) => set({ chatChannels: channels }),
      setChatMessages: (channelId, messages) => set(s => ({
          chatMessages: { ...s.chatMessages, [channelId]: messages }
      })),
      setActiveChatChannelId: (id) => set({ activeChatChannelId: id }),
      addChatMessage: (message) => set(s => {
          const current = s.chatMessages[message.channelId] || [];
          // Avoid duplicates if real-time double triggers
          if (current.some(m => m.id === message.id)) return s;
          return {
              chatMessages: {
                  ...s.chatMessages,
                  [message.channelId]: [...current, message]
              }
          };
      }),
      addChatChannel: (channel) => set(s => {
          if (s.chatChannels.some(c => c.id === channel.id)) return s;
          return { chatChannels: [...s.chatChannels, channel] };
      }),

      fetchChatChannels: async () => {
          const user = get().currentUser;
          if (!user) return;
          try {
              const res = await fetch(`/api/chat/channels?userId=${user.id}`);
              const data = await res.json();
              if (res.ok && Array.isArray(data.channels)) {
                  set({ chatChannels: data.channels });
              } else if (data.error) {
                  console.error('Failed to fetch chat channels:', data.error);
              }
          } catch (e) {
              console.error('Failed to fetch chat channels', e);
          }
      },

      fetchChatMessages: async (channelId: string) => {
          try {
              const res = await fetch(`/api/chat/messages?channelId=${channelId}`);
              const data = await res.json();
              if (res.ok && Array.isArray(data.messages)) {
                  set(s => ({
                      chatMessages: { ...s.chatMessages, [channelId]: data.messages }
                  }));
              }
          } catch (e) {
              console.error('Failed to fetch chat messages', e);
          }
      },

      sendChatMessage: async (content, references, channelId) => {
          const user = get().currentUser;
          if (!user) return { success: false, error: 'Not logged in' };
          
          const tempId = `msg-${Date.now()}`;
          const message: ChatMessage = {
              id: tempId,
              channelId,
              senderId: user.id,
              content,
              references: references || [],
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString()
          };
          
          // Optimistic update
          get().addChatMessage(message);
          
          try {
              const res = await fetch('/api/chat/messages', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify(message)
              });
              const data = await res.json();
              if (!res.ok) {
                  return { success: false, error: data.error || 'Failed to send message' };
              }
              return { success: true };
          } catch (e: any) {
              return { success: false, error: e.message || 'Network error' };
          }
      },

      createNewChatChannel: async (name, description, type, memberIds) => {
          const user = get().currentUser;
          if (!user) return { success: false, error: 'Not logged in' };
          
          const formattedName = type === 'group' 
            ? name.trim().toLowerCase().replace(/[^a-z0-9_-]/g, '-').replace(/-+/g, '-')
            : name.trim();
          const channelId = `chan-${Date.now()}`;
          const uniqueMemberIds = Array.from(new Set([...memberIds, user.id]));
          const channel: Omit<ChatChannel, 'createdAt' | 'updatedAt'> = {
              id: channelId,
              name: formattedName || 'channel',
              description,
              type,
              createdBy: user.id,
              memberIds: uniqueMemberIds
          };
          
          try {
              const res = await fetch('/api/chat/channels', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({ channel, memberIds: channel.memberIds })
              });
              const data = await res.json();
              if (res.ok && data.channel) {
                  get().addChatChannel(data.channel);
                  return { success: true, channel: data.channel };
              }
              return { success: false, error: data.error || 'Failed to create channel' };
          } catch (e: any) {
              return { success: false, error: e.message || 'Network error' };
          }
      },

      deleteChatChannel: async (channelId) => {
        const user = get().currentUser;
        if (!user) return { success: false, error: 'Not logged in' };
        try {
          const res = await fetch(`/api/chat/channels?channelId=${channelId}&userId=${user.id}&userRole=${user.role}`, {
            method: 'DELETE',
          });
          const data = await res.json();
          if (res.ok && data.success) {
            set(s => ({
              chatChannels: s.chatChannels.filter(c => c.id !== channelId),
            }));
            return { success: true };
          }
          return { success: false, error: data.error || 'Failed to delete channel' };
        } catch (e: any) {
          return { success: false, error: e.message || 'Network error' };
        }
      },

      deleteChatMessage: async (messageId, channelId) => {
        const user = get().currentUser;
        if (!user) return { success: false, error: 'Not logged in' };
        try {
          const res = await fetch(`/api/chat/messages?messageId=${messageId}&userId=${user.id}`, {
            method: 'DELETE',
          });
          const data = await res.json();
          if (res.ok && data.success) {
            set(s => ({
              chatMessages: {
                ...s.chatMessages,
                [channelId]: (s.chatMessages[channelId] || []).filter(m => m.id !== messageId),
              },
            }));
            return { success: true };
          }
          return { success: false, error: data.error || 'Failed to delete message' };
        } catch (e: any) {
          return { success: false, error: e.message || 'Network error' };
        }
      },

      login: async (username: string, password: string) => {
        try {
          const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
          });

          const data = await res.json();
          if (res.ok && data.success && data.user) {
            set({ currentUser: data.user, isAuthenticated: true });
            get().syncWithServer();
            return { success: true };
          }
          return { success: false, error: data.error || 'Invalid credentials' };
        } catch (e: any) {
          console.error('Login error:', e);
          return { success: false, error: e.message || 'Network error' };
        }
      },

      logout: () => set({ currentUser: null, isAuthenticated: false }),

      changePassword: async (currentPassword, newPassword) => {
        const username = get().currentUser?.username;
        if (!username) return { success: false, error: 'Not logged in' };
        
        try {
          const res = await fetch('/api/auth/password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, currentPassword, newPassword }),
          });
          const data = await res.json();
          return { success: res.ok && data.success, error: data.error };
        } catch (e: any) {
          return { success: false, error: e.message || 'Network error' };
        }
      },

      syncWithServer: async () => {
        try {
          const [res, financeRes, fundraisingRes] = await Promise.all([
            fetch('/api/sync', { cache: 'no-store' }),
            fetch('/api/finance', { cache: 'no-store' }),
            fetch('/api/fundraising/meta', { cache: 'no-store' })
          ]);
          
          if (res.ok) {
            const data = await res.json();
            set({
              tasks: data.tasks || get().tasks,
              projects: data.projects || get().projects,
              meetings: data.meetings || get().meetings,
              leads: data.leads || get().leads,
              investors: data.investors || get().investors,
              rdProjects: data.rdProjects || get().rdProjects,
              team: data.users || get().team,
              goals: data.goals || get().goals,
              wikiDocs: data.wikiDocs || get().wikiDocs,
              crmContacts: data.crmContacts || get().crmContacts,
              progressCategories: data.progressCategories || get().progressCategories,
              progressLogs: data.progressLogs || get().progressLogs,
              lastSyncTime: data.lastUpdated || Date.now(),
            });
            
            // Also fetch pending users if c_level
            if (get().currentUser?.role === 'c_level') {
                get().fetchPendingUsers();
            }
            
            // Fetch chat
            get().fetchChatChannels();
          }
          
          if (financeRes.ok) {
            const financeData = await financeRes.json();
            if (financeData.financeData) {
              set({ financeData: financeData.financeData });
            }
          }

          if (fundraisingRes.ok) {
            const fundraisingData = await fundraisingRes.json();
            if (fundraisingData.fundraisingMeta) {
              set({ fundraisingMeta: fundraisingData.fundraisingMeta });
            }
          }
        } catch (err) {
          console.warn('Sync failed, using cached store state:', err);
        }
      },

      broadcastUpdates: async (partialUpdates = {}) => {
        try {
          const state = get();
          await fetch('/api/sync', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(partialUpdates),
          });
        } catch (err) {
          console.warn('Broadcast failed:', err);
        }
      },

      fetchPendingUsers: async () => {
         try {
          const res = await fetch('/api/users/pending');
          if (res.ok) {
            const data = await res.json();
            set({ pendingUsers: data.users || [] });
          }
        } catch (err) {
          console.warn('Failed to fetch pending users:', err);
        }
      },

      approveMember: async (id, role) => {
        try {
          const res = await fetch('/api/users/pending', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, action: 'approve', role }),
          });
          const data = await res.json();
          if (res.ok && data.success) {
            set(s => ({
              pendingUsers: s.pendingUsers.filter(u => u.id !== id),
              team: [...s.team, data.user],
            }));
            return { success: true };
          }
          return { success: false, error: data.error };
        } catch (err: any) {
          return { success: false, error: err.message || 'Network error' };
        }
      },

      rejectMember: async (id) => {
          try {
          const res = await fetch('/api/users/pending', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, action: 'reject' }),
          });
          const data = await res.json();
          if (res.ok && data.success) {
            set(s => ({
              pendingUsers: s.pendingUsers.filter(u => u.id !== id),
            }));
            return { success: true };
          }
          return { success: false, error: data.error };
        } catch (err: any) {
          return { success: false, error: err.message || 'Network error' };
        }
      },

      updateMember: async (id, updates) => {
        try {
          const res = await fetch(`/api/users?id=${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates),
          });
          const data = await res.json();
          if (res.ok && data.success) {
            set(s => ({
              team: s.team.map(m => m.id === id ? { ...m, ...updates } : m),
            }));
            return { success: true };
          }
          return { success: false, error: data.error };
        } catch (err: any) {
           return { success: false, error: err.message || 'Network error' };
        }
      },

      deleteMember: async (id) => {
         try {
          const res = await fetch(`/api/users?id=${id}`, {
            method: 'DELETE',
          });
          const data = await res.json();
          if (res.ok && data.success) {
            set(s => ({
              team: s.team.filter(m => m.id !== id),
            }));
            return { success: true };
          }
          return { success: false, error: data.error };
        } catch (err: any) {
           return { success: false, error: err.message || 'Network error' };
        }
      },

      sidebarCollapsed: false,
      toggleSidebar: () => set(s => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      activeModule: 'dashboard',
      setActiveModule: (m) => set({ activeModule: m }),
      mobileMenuOpen: false,
      toggleMobileMenu: () => set(s => ({ mobileMenuOpen: !s.mobileMenuOpen })),
      closeMobileMenu: () => set({ mobileMenuOpen: false }),

      // --- Tasks ---
      addTask: (task) => {
        const updated = [task, ...get().tasks];
        set({ tasks: updated });
        get().broadcastUpdates({ tasks: [task] }); // Broadcast only the new task for upsert
      },
      updateTask: (taskId, updates) => {
        let updatedTask: Task | null = null;
        const updated = get().tasks.map(t => {
            if (t.id === taskId) {
                updatedTask = { ...t, ...updates, updatedAt: new Date().toISOString() };
                return updatedTask;
            }
            return t;
        });
        set({ tasks: updated });
        if(updatedTask) get().broadcastUpdates({ tasks: [updatedTask] });
      },
      updateTaskStatus: (taskId, status) => get().updateTask(taskId, { status }),
      deleteTask: (taskId) => {
          // Soft delete in store, but backend delete is not implemented in sync yet.
          // Ideally need a dedicated endpoint, but for now just remove from UI.
          const updated = get().tasks.filter(t => t.id !== taskId);
          set({ tasks: updated });
          // Note: broadcastUpdates currently upserts, it doesn't delete. 
          // Needs separate /api/tasks API for proper delete.
      },
      toggleSubtask: (taskId, subtaskId) => {
         const task = get().tasks.find(t => t.id === taskId);
         if(task) {
             const subtasks = task.subtasks.map(st => st.id === subtaskId ? { ...st, completed: !st.completed } : st);
             get().updateTask(taskId, { subtasks });
         }
      },
      convertActionItemToTask: (meetingId, actionItemId) => {
        const state = get();
        const meeting = state.meetings.find(m => m.id === meetingId);
        if (!meeting) return;
        const item = meeting.actionItems.find(a => a.id === actionItemId);
        if (!item || item.convertedToTask) return;

        const newTaskId = `task-${Date.now()}`;
        const newTask: Task = {
          id: newTaskId,
          title: item.description,
          description: `Action item from meeting "${meeting.title}"`,
          projectId: meeting.projectId || 'proj-1',
          assigneeId: item.assigneeId,
          status: 'planned',
          priority: 'high',
          dueDate: item.dueDate,
          estimatedHours: 4,
          actualHours: 0,
          tags: ['action-item', meeting.type],
          subtasks: [],
          dependencies: [],
          deliverables: [],
          createdAt: new Date().toISOString().split('T')[0],
          updatedAt: new Date().toISOString().split('T')[0],
        };

        const updatedMeetings = state.meetings.map(m =>
          m.id === meetingId
            ? {
                ...m,
                actionItems: m.actionItems.map(a =>
                  a.id === actionItemId
                    ? { ...a, convertedToTask: true, taskId: newTaskId }
                    : a
                ),
              }
            : m
        );
        
        get().addTask(newTask);
        set({ meetings: updatedMeetings });
        get().broadcastUpdates({ meetings: updatedMeetings.filter(m => m.id === meetingId) });
      },

      // --- Projects ---
      addProject: (projectData) => {
        const newProject: Project = {
          ...projectData,
          id: `proj-${Date.now()}`,
          progress: projectData.progress ?? 0,
          objectives: projectData.objectives ?? [],
          milestones: projectData.milestones ?? [],
          risks: projectData.risks ?? [],
          tags: projectData.tags ?? ['active'],
        };
        const updated = [newProject, ...get().projects];
        set({ projects: updated });
        get().broadcastUpdates({ projects: [newProject] });
      },
      updateProject: (projectId, updates) => {
        let updatedProject: Project | null = null;
        const updated = get().projects.map(p => {
             if (p.id === projectId) {
                updatedProject = { ...p, ...updates };
                return updatedProject;
            }
            return p;
        });
        set({ projects: updated });
        if(updatedProject) get().broadcastUpdates({ projects: [updatedProject] });
      },
      updateProjectStatus: (projectId, status) => get().updateProject(projectId, { status }),
      deleteProject: (projectId) => {
          set(s => ({ projects: s.projects.filter(p => p.id !== projectId) }));
      },

      // --- Meetings ---
      addMeeting: (meetingData) => {
        const newMeeting: Meeting = {
          ...meetingData,
          id: `meet-${Date.now()}`,
          actionItems: meetingData.actionItems ?? [],
          agenda: meetingData.agenda ?? [],
          participants: meetingData.participants ?? [],
        };
        const updated = [newMeeting, ...get().meetings];
        set({ meetings: updated });
        get().broadcastUpdates({ meetings: [newMeeting] });
      },
      updateMeeting: (meetingId, updates) => {
         let updatedMeeting: Meeting | null = null;
         const updated = get().meetings.map(m => {
             if (m.id === meetingId) {
                updatedMeeting = { ...m, ...updates };
                return updatedMeeting;
            }
            return m;
        });
        set({ meetings: updated });
        if(updatedMeeting) get().broadcastUpdates({ meetings: [updatedMeeting] });
      },
      deleteMeeting: (meetingId) => set(s => ({ meetings: s.meetings.filter(m => m.id !== meetingId) })),

      // --- Leads ---
      addLead: (leadData) => {
        const newLead: Lead = { ...leadData, id: `lead-${Date.now()}` };
        const updated = [newLead, ...get().leads];
        set({ leads: updated });
        get().broadcastUpdates({ leads: [newLead] });
      },
      updateLeadStage: (leadId, stage) => {
        const updated = get().leads.map(l => l.id === leadId ? { ...l, stage } : l);
        set({ leads: updated });
        get().broadcastUpdates({ leads: updated.filter(l => l.id === leadId) });
      },

      // --- Investors ---
      addInvestor: (investorData) => {
        const newInvestor: Investor = { ...investorData, id: `inv-${Date.now()}` };
        const updated = [newInvestor, ...get().investors];
        set({ investors: updated });
        get().broadcastUpdates({ investors: [newInvestor] });
      },
      updateInvestorStage: (investorId, stage) => {
        const updated = get().investors.map(i => i.id === investorId ? { ...i, stage } : i);
        set({ investors: updated });
        get().broadcastUpdates({ investors: updated.filter(i => i.id === investorId) });
      },

      // --- R&D ---
      addRDProject: (rdProjectData) => {
        const newRDProject: RDProject = {
          ...rdProjectData,
          id: `rd-${Date.now()}`,
          experiments: rdProjectData.experiments ?? [],
          progress: rdProjectData.progress ?? 0,
        };
        const updated = [newRDProject, ...get().rdProjects];
        set({ rdProjects: updated });
        get().broadcastUpdates({ rdProjects: [newRDProject] });
      },
      addExperiment: (projectId, expData) => {
        let updatedProject: RDProject | null = null;
        const updated = get().rdProjects.map(p => {
          if (p.id !== projectId) return p;
          const newExp: Experiment = { ...expData, id: `exp-${Date.now()}`, number: p.experiments.length + 1 };
          updatedProject = { ...p, experiments: [newExp, ...p.experiments] };
          return updatedProject;
        });
        set({ rdProjects: updated });
        if(updatedProject) get().broadcastUpdates({ rdProjects: [updatedProject] });
      },

      // --- Goals ---
      addGoal: (goalData) => {
          const newGoal: Goal = { ...goalData, id: `goal-${Date.now()}`, createdAt: new Date().toISOString() };
          set(s => ({ goals: [newGoal, ...s.goals] }));
          get().broadcastUpdates({ goals: [newGoal] });
      },
      updateGoal: (goalId, updates) => {
          let updatedGoal: Goal | null = null;
          set(s => ({
              goals: s.goals.map(g => {
                  if(g.id === goalId) {
                      updatedGoal = { ...g, ...updates };
                      return updatedGoal;
                  }
                  return g;
              })
          }));
          if(updatedGoal) get().broadcastUpdates({ goals: [updatedGoal] });
      },
      deleteGoal: (goalId) => set(s => ({ goals: s.goals.filter(g => g.id !== goalId) })),

      // --- Wiki Docs ---
      addWikiDoc: (docData) => {
           const newDoc: WikiDoc = { ...docData, id: `wiki-${Date.now()}`, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
           set(s => ({ wikiDocs: [newDoc, ...s.wikiDocs] }));
           get().broadcastUpdates({ wikiDocs: [newDoc] });
      },
      updateWikiDoc: (docId, updates) => {
           let updatedDoc: WikiDoc | null = null;
           set(s => ({
               wikiDocs: s.wikiDocs.map(w => {
                   if(w.id === docId) {
                       updatedDoc = { ...w, ...updates, updatedAt: new Date().toISOString() };
                       return updatedDoc;
                   }
                   return w;
               })
           }));
           if(updatedDoc) get().broadcastUpdates({ wikiDocs: [updatedDoc] });
      },
      deleteWikiDoc: (docId) => set(s => ({ wikiDocs: s.wikiDocs.filter(w => w.id !== docId) })),

      // --- CRM Contacts ---
      addCrmContact: (contactData) => {
          const newContact: CrmContact = { ...contactData, id: `crm-${Date.now()}`, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
          set(s => ({ crmContacts: [newContact, ...s.crmContacts] }));
          get().broadcastUpdates({ crmContacts: [newContact] });
      },
      updateCrmContact: (contactId, updates) => {
          let updatedContact: CrmContact | null = null;
           set(s => ({
               crmContacts: s.crmContacts.map(c => {
                   if(c.id === contactId) {
                       updatedContact = { ...c, ...updates, updatedAt: new Date().toISOString() };
                       return updatedContact;
                   }
                   return c;
               })
           }));
           if(updatedContact) get().broadcastUpdates({ crmContacts: [updatedContact] });
      },
      deleteCrmContact: (contactId) => set(s => ({ crmContacts: s.crmContacts.filter(c => c.id !== contactId) })),
      importCrmContacts: (contactsData) => {
          const newContacts = contactsData.map(c => ({
              ...c, 
              createdAt: new Date().toISOString(), 
              updatedAt: new Date().toISOString() 
          })) as CrmContact[];
          
          set(s => ({ crmContacts: [...newContacts, ...s.crmContacts] }));
          get().broadcastUpdates({ crmContacts: newContacts });
      },

      // --- Progress Logger ---
      addProgressCategory: (catData) => {
          const newCat: ProgressCategory = { ...catData, id: `pcat-${Date.now()}` };
          set(s => ({ progressCategories: [...s.progressCategories, newCat] }));
          get().broadcastUpdates({ progressCategories: [newCat] });
      },
      updateProgressCategory: (catId, updates) => {
           let updatedCat: ProgressCategory | null = null;
           set(s => ({
               progressCategories: s.progressCategories.map(c => {
                   if(c.id === catId) {
                       updatedCat = { ...c, ...updates };
                       return updatedCat;
                   }
                   return c;
               })
           }));
           if(updatedCat) get().broadcastUpdates({ progressCategories: [updatedCat] });
      },
      deleteProgressCategory: (catId) => set(s => ({ progressCategories: s.progressCategories.filter(c => c.id !== catId) })),
      addProgressLog: (logData) => {
          const newLog: DailyLog = { ...logData, id: `plog-${Date.now()}`, submittedAt: new Date().toISOString() };
          set(s => {
              // Ensure uniqueness by user+date (simplified)
              const existingIdx = s.progressLogs.findIndex(l => l.userId === newLog.userId && l.date === newLog.date);
              if(existingIdx >= 0) {
                  const arr = [...s.progressLogs];
                  arr[existingIdx] = newLog;
                  return { progressLogs: arr };
              }
              return { progressLogs: [newLog, ...s.progressLogs] };
          });
          get().broadcastUpdates({ progressLogs: [newLog] });
      },
      updateFinanceData: async (updates) => {
        try {
          const res = await fetch('/api/finance', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates),
          });
          if (res.ok) {
            set(s => ({
              financeData: s.financeData ? { ...s.financeData, ...updates, updatedAt: new Date().toISOString() } : null
            }));
            get().broadcastUpdates({ financeData: updates });
            return { success: true };
          }
          const data = await res.json();
          return { success: false, error: data.error };
        } catch (e: any) {
          return { success: false, error: e.message };
        }
      },
      updateFundraisingMeta: async (updates) => {
        try {
          const res = await fetch('/api/fundraising/meta', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates),
          });
          if (res.ok) {
            set(s => ({
              fundraisingMeta: s.fundraisingMeta ? { ...s.fundraisingMeta, ...updates, updatedAt: new Date().toISOString() } : null
            }));
            get().broadcastUpdates({ fundraisingMeta: updates });
            return { success: true };
          }
          const data = await res.json();
          return { success: false, error: data.error };
        } catch (e: any) {
          return { success: false, error: e.message };
        }
      },
    }),
    {
      name: 'ignitionai-hq-session',
      partialize: (state) => ({
        currentUser: state.currentUser,
        isAuthenticated: state.isAuthenticated,
        sidebarCollapsed: state.sidebarCollapsed,
        // Don't persist large data arrays in local storage indefinitely;
        // The server is the source of truth, but we persist for fast reload.
        projects: state.projects,
        tasks: state.tasks,
        meetings: state.meetings,
        team: state.team,
        wikiDocs: state.wikiDocs,
        lastSyncTime: state.lastSyncTime,
        chatChannels: state.chatChannels,
      }),
    }
  )
);
