export type Priority = 'critical' | 'high' | 'medium' | 'low';
export type ProjectStatus = 'backlog' | 'planned' | 'in_progress' | 'review' | 'testing' | 'done' | 'on_hold';
export type TaskStatus = 'backlog' | 'planned' | 'in_progress' | 'review' | 'testing' | 'done' | 'blocked';
export type LeadStage = 'lead' | 'contacted' | 'interested' | 'meeting' | 'pilot' | 'proposal' | 'negotiation' | 'customer';
export type InvestorStage = 'research' | 'application' | 'intro' | 'first_meeting' | 'partner_meeting' | 'due_diligence' | 'term_sheet' | 'closed' | 'pass';
export type RDStage = 'hypothesis' | 'methodology' | 'dataset' | 'model_dev' | 'results' | 'published';
export type UserRole = 'c_level' | 'executive' | 'intern' | 'founder' | 'engineering' | 'ml_rd' | 'coo' | 'bizdev';
export type UserStatus = 'active' | 'pending' | 'rejected';

export interface User {
  id: string;
  name: string;
  username: string;
  email?: string;
  role: UserRole;
  status: UserStatus;
  designation?: string;
  avatar?: string;
  department: string;
  joiningDate: string;
  skills: string[];
  managerId?: string;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  memberIds: string[];
  startDate: string;
  targetDate: string;
  priority: Priority;
  status: ProjectStatus;
  progress: number;
  objectives: string[];
  milestones: Milestone[];
  risks: Risk[];
  tags: string[];
  category: 'product' | 'rd' | 'bizdev' | 'operations' | 'fundraising' | 'internal';
}

export interface Milestone {
  id: string;
  title: string;
  dueDate: string;
  completed: boolean;
}

export interface Risk {
  id: string;
  description: string;
  severity: 'low' | 'medium' | 'high';
  mitigation: string;
}

export interface Task {
  id: string;
  title: string;
  description: string;
  projectId: string;
  assigneeId: string;
  reviewerId?: string;
  status: TaskStatus;
  priority: Priority;
  dueDate: string;
  estimatedHours: number;
  actualHours: number;
  tags: string[];
  subtasks: SubTask[];
  dependencies: string[];
  deliverables: string[];
  createdAt: string;
  updatedAt: string;
}

export interface SubTask {
  id: string;
  title: string;
  completed: boolean;
}

export interface Meeting {
  id: string;
  title: string;
  date: string;
  time: string;
  duration: number;
  participants: string[];
  agenda: string[];
  notes: string;
  actionItems: ActionItem[];
  projectId?: string;
  type: 'internal' | 'external' | 'investor' | 'mentor';
}

export interface ActionItem {
  id: string;
  description: string;
  assigneeId: string;
  dueDate: string;
  completed: boolean;
  convertedToTask: boolean;
  taskId?: string;
}

export interface Lead {
  id: string;
  companyName: string;
  type: string;
  location: string;
  contactName: string;
  contactEmail?: string;
  contactPhone?: string;
  opportunity: string;
  stage: LeadStage;
  potentialValue: number;
  currency: 'INR' | 'USD';
  activities: Activity[];
  notes: string;
  assignedTo: string;
  nextFollowUp?: string;
}

export interface Activity {
  id: string;
  date: string;
  description: string;
  type: 'call' | 'email' | 'meeting' | 'demo' | 'note';
}

export interface Investor {
  id: string;
  name: string;
  firm?: string;
  stage: InvestorStage;
  round: string;
  lastContact: string;
  nextAction: string;
  nextActionDate?: string;
  ticketSize?: string;
  investmentThesis?: string;
  notes: string;
  interactions: Activity[];
  contactPerson?: string;
  deckVersionSent?: string;
}

export interface RDProject {
  id: string;
  title: string;
  researchQuestion: string;
  leadId: string;
  supervisorId: string;
  stage: RDStage;
  dataset?: string;
  hypothesis?: string;
  methodology?: string;
  experiments: Experiment[];
  progress: number;
  projectId?: string;
}

export interface Experiment {
  id: string;
  number: number;
  model: string;
  features: string[];
  rmse?: number;
  r2?: number;
  accuracy?: number;
  dataset: string;
  result: string;
  isBest: boolean;
  date: string;
  parameters: Record<string, string | number>;
}

export interface TeamMember extends User {
  activeProjects: string[];
  activeTasks: number;
  completedTasks: number;
  overdueTasks: number;
  performanceNotes?: string;
}

export interface DashboardStats {
  projects: { active: number; atRisk: number; delayed: number; onTrack: number };
  tasks: { inProgress: number; blocked: number; dueThisWeek: number; completed: number };
  team: { active: number; overdueTasks: number };
  crm: { totalLeads: number; contacted: number; meetings: number; pilots: number };
  fundraising: { investors: number; activeDiscussions: number };
}

// ── Goals / OKRs ─────────────────────────────────────────────
export interface KeyResult {
  id: string;
  title: string;
  target: string;
  current: string;
  progress: number; // 0–100
}

export interface Goal {
  id: string;
  quarter: string;
  objective: string;
  progress: number; // 0–100
  keyResults: KeyResult[];
  createdBy: string;
  createdAt: string;
}

// ── Knowledge Base / Wiki ────────────────────────────────────
export interface WikiDoc {
  id: string;
  title: string;
  description: string;
  category: string;
  content: string;
  readTime?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

// ── CRM Contacts ─────────────────────────────────────────────
export interface CrmContact {
  id: string;
  name: string;
  email: string;
  phone: string;
  designation: string;
  company: string;
  notes: string;
  stage: LeadStage;
  createdAt: string;
  updatedAt: string;
}

// ── Progress Logger ──────────────────────────────────────────
export interface ProgressSubComponent {
  id: string;
  name: string;
}

export interface ProgressCategory {
  id: string;
  name: string;
  subComponents: ProgressSubComponent[];
}

export interface LogEntry {
  subComponentId: string;
  categoryId: string;
  hours: number;
  notes: string;
}

export interface DailyLog {
  id: string;
  userId: string;
  date: string; // YYYY-MM-DD
  entries: LogEntry[];
  submittedAt: string;
}

export interface FinanceExpense {
  category: string;
  amount: number;
  percentage: number;
}

export interface FinanceData {
  id: string;
  runwayMonths: number;
  monthlyBurn: number;
  cashBalance: number;
  committedArr: number;
  expenses: FinanceExpense[];
  updatedAt: string;
}

export interface FundraisingMeta {
  id: string;
  targetAmount: number;
  currency: string;
  roundName: string;
  pitchDeckVersion: string;
  updatedAt: string;
}
