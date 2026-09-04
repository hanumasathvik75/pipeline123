import type {
  Project, Task, Meeting, Lead, Investor, RDProject,
  TeamMember, Goal, WikiDoc, CrmContact, ProgressCategory
} from './types';

// ─── TEAM ───────────────────────────────────────────────────
// Only the founder account is seeded. All other users must register and be approved.
// Password is set via serverDb.ts (bcrypt hashed: Ignition@2026)
export const TEAM_MEMBERS: TeamMember[] = [
  {
    id: 'user-1',
    name: 'Shashank',
    username: 'shashank',
    email: 'shashank@ignitionai.in',
    role: 'c_level',
    status: 'active',
    designation: 'Founder & CEO',
    department: 'Executive',
    joiningDate: '2024-01-01',
    skills: ['Strategy', 'AI/ML', 'Business Development', 'Fundraising'],
    activeProjects: [],
    activeTasks: 0,
    completedTasks: 0,
    overdueTasks: 0,
  },
];

// ─── ALL OTHER DATA STARTS EMPTY ────────────────────────────
export const PROJECTS: Project[] = [];
export const TASKS: Task[] = [];
export const MEETINGS: Meeting[] = [];
export const LEADS: Lead[] = [];
export const INVESTORS: Investor[] = [];
export const RD_PROJECTS: RDProject[] = [];
export const GOALS: Goal[] = [];
export const WIKI_DOCS: WikiDoc[] = [];
export const CRM_CONTACTS: CrmContact[] = [];
export const PROGRESS_CATEGORIES: ProgressCategory[] = [];
