import type { Priority, ProjectStatus, TaskStatus, LeadStage, InvestorStage, RDStage, UserRole } from './types';

export type {
  Priority, ProjectStatus, TaskStatus, LeadStage, InvestorStage, RDStage, UserRole,
  User, Project, Milestone, Risk, Task, SubTask, Meeting, ActionItem,
  Lead, Activity, Investor, RDProject, Experiment, TeamMember, DashboardStats,
} from './types';

export { TEAM_MEMBERS, PROJECTS, TASKS, MEETINGS, LEADS, INVESTORS, RD_PROJECTS } from './mockData';
