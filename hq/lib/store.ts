'use client';

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Project, Task, Meeting, Lead, Investor, RDProject, TeamMember, UserRole } from './types';
import { PROJECTS, TASKS, MEETINGS, LEADS, INVESTORS, RD_PROJECTS, TEAM_MEMBERS } from './mockData';

interface HQStore {
  // Auth
  currentUser: TeamMember | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => void;

  // Data
  projects: Project[];
  tasks: Task[];
  meetings: Meeting[];
  leads: Lead[];
  investors: Investor[];
  rdProjects: RDProject[];
  team: TeamMember[];

  // Real-time synchronization
  lastSyncTime: number;
  syncWithServer: () => Promise<void>;
  broadcastUpdates: (partialUpdates?: Record<string, unknown>) => Promise<void>;

  // User management
  addTeamMember: (member: {
    name: string;
    email: string;
    password: string;
    designation: string;
    role: UserRole;
    department: string;
    skills: string[];
  }) => Promise<{ success: boolean; error?: string }>;

  // UI state
  sidebarCollapsed: boolean;
  toggleSidebar: () => void;
  activeModule: string;
  setActiveModule: (m: string) => void;

  // Task actions
  updateTaskStatus: (taskId: string, status: Task['status']) => void;
  updateProjectStatus: (projectId: string, status: Project['status']) => void;
  toggleSubtask: (taskId: string, subtaskId: string) => void;
  addTask: (task: Task) => void;
  convertActionItemToTask: (meetingId: string, actionItemId: string) => void;
  updateLeadStage: (leadId: string, stage: Lead['stage']) => void;
  updateInvestorStage: (investorId: string, stage: Investor['stage']) => void;
  addLead: (lead: Omit<Lead, 'id'>) => void;
  addInvestor: (investor: Omit<Investor, 'id'>) => void;
  addExperiment: (projectId: string, exp: Omit<Experiment, 'id' | 'number'>) => void;
}

export const useHQStore = create<HQStore>()(
  persist(
    (set, get) => ({
      currentUser: null,
      isAuthenticated: false,
      lastSyncTime: 0,

      login: async (email: string, password: string) => {
        try {
          const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
          });

          if (res.ok) {
            const data = await res.json();
            if (data.success && data.user) {
              set({ currentUser: data.user, isAuthenticated: true });
              get().syncWithServer();
              return true;
            }
          }
        } catch (e) {
          console.error('Login error, falling back to local list:', e);
        }

        // Fallback for seed accounts
        const user = get().team.find(m => m.email.toLowerCase() === email.toLowerCase()) ||
          TEAM_MEMBERS.find(m => m.email.toLowerCase() === email.toLowerCase());
        if (user) {
          set({ currentUser: user, isAuthenticated: true });
          return true;
        }
        return false;
      },

      logout: () => set({ currentUser: null, isAuthenticated: false }),

      projects: PROJECTS,
      tasks: TASKS,
      meetings: MEETINGS,
      leads: LEADS,
      investors: INVESTORS,
      rdProjects: RD_PROJECTS,
      team: TEAM_MEMBERS,

      syncWithServer: async () => {
        try {
          const res = await fetch('/api/sync');
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
              lastSyncTime: data.lastUpdated || Date.now(),
            });
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
            body: JSON.stringify({
              tasks: state.tasks,
              projects: state.projects,
              meetings: state.meetings,
              leads: state.leads,
              investors: state.investors,
              ...partialUpdates,
            }),
          });
        } catch (err) {
          console.warn('Broadcast failed:', err);
        }
      },

      addTeamMember: async (memberData) => {
        try {
          const res = await fetch('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(memberData),
          });

          const data = await res.json();
          if (res.ok && data.success) {
            set(s => ({
              team: [...s.team.filter(t => t.id !== data.user.id), data.user],
            }));
            get().syncWithServer();
            return { success: true };
          }
          return { success: false, error: data.error || 'Failed to create user' };
        } catch (err: unknown) {
          const msg = err instanceof Error ? err.message : 'Network error';
          return { success: false, error: msg };
        }
      },

      sidebarCollapsed: false,
      toggleSidebar: () => set(s => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      activeModule: 'dashboard',
      setActiveModule: (m) => set({ activeModule: m }),

      updateTaskStatus: (taskId, status) => {
        const updated = get().tasks.map(t =>
          t.id === taskId ? { ...t, status, updatedAt: new Date().toISOString().split('T')[0] } : t
        );
        set({ tasks: updated });
        get().broadcastUpdates({ tasks: updated });
      },

      updateProjectStatus: (projectId, status) => {
        const updated = get().projects.map(p =>
          p.id === projectId ? { ...p, status } : p
        );
        set({ projects: updated });
        get().broadcastUpdates({ projects: updated });
      },

      toggleSubtask: (taskId, subtaskId) => {
        const updated = get().tasks.map(t =>
          t.id === taskId
            ? { ...t, subtasks: t.subtasks.map(st => st.id === subtaskId ? { ...st, completed: !st.completed } : st) }
            : t
        );
        set({ tasks: updated });
        get().broadcastUpdates({ tasks: updated });
      },

      addTask: (task) => {
        const updated = [task, ...get().tasks];
        set({ tasks: updated });
        get().broadcastUpdates({ tasks: updated });
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

        const updatedTasks = [newTask, ...state.tasks];
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

        set({ tasks: updatedTasks, meetings: updatedMeetings });
        get().broadcastUpdates({ tasks: updatedTasks, meetings: updatedMeetings });
      },

      updateLeadStage: (leadId, stage) => {
        const updated = get().leads.map(l => l.id === leadId ? { ...l, stage } : l);
        set({ leads: updated });
        get().broadcastUpdates({ leads: updated });
      },

      updateInvestorStage: (investorId, stage) => {
        const updated = get().investors.map(i => i.id === investorId ? { ...i, stage } : i);
        set({ investors: updated });
        get().broadcastUpdates({ investors: updated });
      },

      addLead: (leadData) => {
        const newLead: Lead = { ...leadData, id: `lead-${Date.now()}` };
        const updated = [newLead, ...get().leads];
        set({ leads: updated });
        get().broadcastUpdates({ leads: updated });
      },

      addInvestor: (investorData) => {
        const newInvestor: Investor = { ...investorData, id: `inv-${Date.now()}` };
        const updated = [newInvestor, ...get().investors];
        set({ investors: updated });
        get().broadcastUpdates({ investors: updated });
      },

      addExperiment: (projectId, expData) => {
        const updated = get().rdProjects.map(p => {
          if (p.id !== projectId) return p;
          const newExp = { ...expData, id: `exp-${Date.now()}`, number: p.experiments.length + 1 };
          return { ...p, experiments: [newExp, ...p.experiments] };
        });
        set({ rdProjects: updated });
        get().broadcastUpdates({ rdProjects: updated });
      },
    }),
    {
      name: 'ignitionai-hq-session',
      partialize: (state) => ({
        currentUser: state.currentUser,
        isAuthenticated: state.isAuthenticated,
        sidebarCollapsed: state.sidebarCollapsed,
      }),
    }
  )
);
