/**
 * serverDb.ts — Supabase-backed data layer
 * Replaces the old file-system shared_state.json approach.
 * All operations go through the Supabase admin client (service role).
 */
import bcrypt from 'bcryptjs';
import { supabaseAdmin } from './supabaseAdmin';
import type {
  TeamMember, Project, Task, Meeting, Lead, Investor, RDProject,
  Goal, WikiDoc, CrmContact, ProgressCategory, DailyLog, UserRole, UserStatus,
  ChatChannel, ChatMessage, ChatMember, ChatReference
} from './types';

export interface UserAccount extends TeamMember {
  passwordHash: string;
}

// ── Row-to-domain mappers ─────────────────────────────────────

function rowToUser(row: Record<string, unknown>): UserAccount {
  return {
    id: row.id as string,
    name: row.name as string,
    username: row.username as string,
    email: row.email as string | undefined,
    role: row.role as UserRole,
    status: row.status as UserStatus,
    designation: row.designation as string,
    department: row.department as string,
    joiningDate: row.joining_date as string,
    skills: (row.skills as string[]) || [],
    activeProjects: (row.active_projects as string[]) || [],
    activeTasks: (row.active_tasks as number) || 0,
    completedTasks: (row.completed_tasks as number) || 0,
    overdueTasks: (row.overdue_tasks as number) || 0,
    performanceNotes: row.performance_notes as string | undefined,
    passwordHash: row.password_hash as string,
  };
}

function rowToProject(row: Record<string, unknown>): Project {
  return {
    id: row.id as string,
    name: row.name as string,
    description: row.description as string,
    ownerId: row.owner_id as string,
    memberIds: (row.member_ids as string[]) || [],
    startDate: row.start_date as string,
    targetDate: row.target_date as string,
    priority: row.priority as Project['priority'],
    status: row.status as Project['status'],
    progress: row.progress as number,
    category: row.category as Project['category'],
    objectives: (row.objectives as string[]) || [],
    milestones: (row.milestones as Project['milestones']) || [],
    risks: (row.risks as Project['risks']) || [],
    tags: (row.tags as string[]) || [],
  };
}

function rowToTask(row: Record<string, unknown>): Task {
  return {
    id: row.id as string,
    title: row.title as string,
    description: (row.description as string) || '',
    projectId: (row.project_id as string) || '',
    assigneeId: (row.assignee_id as string) || '',
    reviewerId: row.reviewer_id as string | undefined,
    status: row.status as Task['status'],
    priority: row.priority as Task['priority'],
    dueDate: row.due_date as string,
    estimatedHours: (row.estimated_hours as number) || 4,
    actualHours: (row.actual_hours as number) || 0,
    tags: (row.tags as string[]) || [],
    subtasks: (row.subtasks as Task['subtasks']) || [],
    dependencies: (row.dependencies as string[]) || [],
    deliverables: (row.deliverables as string[]) || [],
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
  };
}

function rowToMeeting(row: Record<string, unknown>): Meeting {
  return {
    id: row.id as string,
    title: row.title as string,
    date: row.date as string,
    time: row.time as string,
    duration: (row.duration as number) || 30,
    participants: (row.participants as string[]) || [],
    agenda: (row.agenda as string[]) || [],
    notes: (row.notes as string) || '',
    actionItems: (row.action_items as Meeting['actionItems']) || [],
    projectId: row.project_id as string | undefined,
    type: row.type as Meeting['type'],
  };
}

function rowToLead(row: Record<string, unknown>): Lead {
  return {
    id: row.id as string,
    companyName: row.company_name as string,
    type: (row.type as string) || '',
    location: (row.location as string) || '',
    contactName: row.contact_name as string,
    contactEmail: row.contact_email as string | undefined,
    contactPhone: row.contact_phone as string | undefined,
    opportunity: (row.opportunity as string) || '',
    stage: row.stage as Lead['stage'],
    potentialValue: (row.potential_value as number) || 0,
    currency: (row.currency as Lead['currency']) || 'INR',
    activities: (row.activities as Lead['activities']) || [],
    notes: (row.notes as string) || '',
    assignedTo: (row.assigned_to as string) || '',
    nextFollowUp: row.next_follow_up as string | undefined,
  };
}

function rowToInvestor(row: Record<string, unknown>): Investor {
  return {
    id: row.id as string,
    name: row.name as string,
    firm: row.firm as string | undefined,
    stage: row.stage as Investor['stage'],
    round: (row.round as string) || 'Pre-Seed',
    lastContact: row.last_contact as string,
    nextAction: (row.next_action as string) || '',
    nextActionDate: row.next_action_date as string | undefined,
    ticketSize: row.ticket_size as string | undefined,
    investmentThesis: row.investment_thesis as string | undefined,
    notes: (row.notes as string) || '',
    interactions: (row.interactions as Investor['interactions']) || [],
    contactPerson: row.contact_person as string | undefined,
    deckVersionSent: row.deck_version_sent as string | undefined,
  };
}

function rowToRDProject(row: Record<string, unknown>): RDProject {
  return {
    id: row.id as string,
    title: row.title as string,
    researchQuestion: (row.research_question as string) || '',
    leadId: (row.lead_id as string) || '',
    supervisorId: (row.supervisor_id as string) || '',
    stage: row.stage as RDProject['stage'],
    dataset: row.dataset as string | undefined,
    hypothesis: row.hypothesis as string | undefined,
    methodology: row.methodology as string | undefined,
    experiments: (row.experiments as RDProject['experiments']) || [],
    progress: (row.progress as number) || 0,
    projectId: row.project_id as string | undefined,
  };
}

function rowToGoal(row: Record<string, unknown>): Goal {
  return {
    id: row.id as string,
    quarter: row.quarter as string,
    objective: row.objective as string,
    progress: (row.progress as number) || 0,
    keyResults: (row.key_results as Goal['keyResults']) || [],
    createdBy: (row.created_by as string) || '',
    createdAt: row.created_at as string,
  };
}

function rowToWikiDoc(row: Record<string, unknown>): WikiDoc {
  return {
    id: row.id as string,
    title: row.title as string,
    description: (row.description as string) || '',
    category: row.category as string,
    content: (row.content as string) || '',
    readTime: row.read_time as string | undefined,
    googleDriveUrl: row.google_drive_url as string | undefined,
    createdBy: (row.created_by as string) || '',
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
  };
}

function rowToCrmContact(row: Record<string, unknown>): CrmContact {
  return {
    id: row.id as string,
    name: row.name as string,
    email: (row.email as string) || '',
    phone: (row.phone as string) || '',
    designation: (row.designation as string) || '',
    company: (row.company as string) || '',
    notes: (row.notes as string) || '',
    stage: row.stage as CrmContact['stage'],
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
  };
}

function rowToProgressCategory(row: Record<string, unknown>): ProgressCategory {
  return {
    id: row.id as string,
    name: row.name as string,
    subComponents: (row.sub_components as ProgressCategory['subComponents']) || [],
  };
}

function rowToDailyLog(row: Record<string, unknown>): DailyLog {
  return {
    id: row.id as string,
    userId: row.user_id as string,
    date: row.date as string,
    entries: (row.entries as DailyLog['entries']) || [],
    submittedAt: row.submitted_at as string,
  };
}

function rowToFinanceData(row: Record<string, unknown>): any {
  return {
    id: row.id as string,
    runwayMonths: Number(row.runway_months),
    monthlyBurn: Number(row.monthly_burn),
    cashBalance: Number(row.cash_balance),
    committedArr: Number(row.committed_arr),
    expenses: (row.expenses as any[]) || [],
    updatedAt: row.updated_at as string,
  };
}

function rowToFundraisingMeta(row: Record<string, unknown>): any {
  return {
    id: row.id as string,
    targetAmount: Number(row.target_amount),
    currency: row.currency as string,
    roundName: row.round_name as string,
    pitchDeckVersion: row.pitch_deck_version as string,
    updatedAt: row.updated_at as string,
  };
}

function rowToChatChannel(row: Record<string, unknown>): ChatChannel {
  return {
    id: row.id as string,
    name: row.name as string,
    description: row.description as string | undefined,
    type: row.type as 'direct' | 'group',
    createdBy: row.created_by as string,
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
    memberIds: [], // We will join this separately or return it from a joined query
  };
}

function rowToChatMessage(row: Record<string, unknown>): ChatMessage {
  return {
    id: row.id as string,
    channelId: row.channel_id as string,
    senderId: row.sender_id as string,
    content: row.content as string,
    references: (row.references as ChatReference[]) || [],
    createdAt: row.created_at as string,
    updatedAt: row.updated_at as string,
  };
}

// ── Main serverDb object ──────────────────────────────────────

export const serverDb = {
  // ── Auth ────────────────────────────────────────────────────
  async verifyPassword(plain: string, hash: string): Promise<boolean> {
    return bcrypt.compare(plain, hash);
  },

  async hashPassword(plain: string): Promise<string> {
    return bcrypt.hash(plain, 10);
  },

  // ── Users ────────────────────────────────────────────────────
  async findUserByUsername(username: string): Promise<UserAccount | null> {
    const { data, error } = await supabaseAdmin
      .from('hq_users')
      .select('*')
      .ilike('username', username)
      .single();
    if (error || !data) return null;
    return rowToUser(data);
  },

  async findUserById(id: string): Promise<UserAccount | null> {
    const { data, error } = await supabaseAdmin
      .from('hq_users')
      .select('*')
      .eq('id', id)
      .single();
    if (error || !data) return null;
    return rowToUser(data);
  },

  async getAllUsers(): Promise<TeamMember[]> {
    const { data } = await supabaseAdmin
      .from('hq_users')
      .select('*')
      .eq('status', 'active')
      .order('created_at', { ascending: true });
    return (data || []).map((row: any) => {
      const { passwordHash, ...user } = rowToUser(row);
      return user;
    });
  },

  async addUser(user: Omit<UserAccount, 'activeTasks' | 'completedTasks' | 'overdueTasks' | 'activeProjects'>): Promise<TeamMember> {
    const { data, error } = await supabaseAdmin
      .from('hq_users')
      .insert({
        id: user.id,
        name: user.name,
        username: user.username,
        email: user.email || null,
        password_hash: user.passwordHash,
        role: user.role,
        status: user.status,
        designation: user.designation || 'Team Member',
        department: user.department || 'General',
        joining_date: user.joiningDate || new Date().toISOString().split('T')[0],
        skills: user.skills || [],
      })
      .select()
      .single();
    if (error) throw new Error(error.message);
    const { passwordHash, ...safeUser } = rowToUser(data);
    return safeUser;
  },

  async updateUser(id: string, updates: Partial<UserAccount>): Promise<TeamMember | null> {
    const payload: Record<string, unknown> = {};
    if (updates.name) payload.name = updates.name;
    if (updates.role) payload.role = updates.role;
    if (updates.designation) payload.designation = updates.designation;
    if (updates.department) payload.department = updates.department;
    if (updates.skills) payload.skills = updates.skills;
    if (updates.passwordHash) payload.password_hash = updates.passwordHash;
    if (updates.status) payload.status = updates.status;
    if (updates.performanceNotes !== undefined) payload.performance_notes = updates.performanceNotes;
    payload.updated_at = new Date().toISOString();

    const { data, error } = await supabaseAdmin
      .from('hq_users')
      .update(payload)
      .eq('id', id)
      .select()
      .single();
    if (error || !data) return null;
    const { passwordHash, ...safeUser } = rowToUser(data);
    return safeUser;
  },

  async deleteUser(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_users').delete().eq('id', id);
    return !error;
  },

  async checkUsernameAvailable(username: string): Promise<boolean> {
    const { count: activeCount } = await supabaseAdmin
      .from('hq_users').select('id', { count: 'exact', head: true }).ilike('username', username);
    const { count: pendingCount } = await supabaseAdmin
      .from('hq_pending_users').select('id', { count: 'exact', head: true }).ilike('username', username);
    return (activeCount || 0) === 0 && (pendingCount || 0) === 0;
  },

  // ── Pending Users ────────────────────────────────────────────
  async addPendingUser(user: { id: string; name: string; username: string; passwordHash: string; designation: string; department?: string; skills?: string[] }): Promise<void> {
    const { error } = await supabaseAdmin.from('hq_pending_users').insert({
      id: user.id,
      name: user.name,
      username: user.username,
      password_hash: user.passwordHash,
      designation: user.designation,
      department: user.department || 'General',
      skills: user.skills || [],
    });
    if (error) throw new Error(error.message);
  },

  async getPendingUsers(): Promise<Omit<UserAccount, 'passwordHash'>[]> {
    const { data } = await supabaseAdmin
      .from('hq_pending_users')
      .select('*')
      .order('requested_at', { ascending: true });
    return (data || []).map((row: any) => ({
      id: row.id as string,
      name: row.name as string,
      username: row.username as string,
      role: 'intern' as UserRole,
      status: 'pending' as UserStatus,
      designation: row.designation as string,
      department: (row.department as string) || 'General',
      joiningDate: new Date().toISOString().split('T')[0],
      skills: (row.skills as string[]) || [],
      activeProjects: [],
      activeTasks: 0,
      completedTasks: 0,
      overdueTasks: 0,
    }));
  },

  async approveUser(id: string, role: UserRole = 'executive'): Promise<TeamMember | null> {
    const { data: pending } = await supabaseAdmin
      .from('hq_pending_users').select('*').eq('id', id).single();
    if (!pending) return null;

    const { data, error } = await supabaseAdmin.from('hq_users').insert({
      id: pending.id,
      name: pending.name,
      username: pending.username,
      password_hash: pending.password_hash,
      role,
      status: 'active',
      designation: pending.designation,
      department: pending.department || 'General',
      joining_date: new Date().toISOString().split('T')[0],
      skills: pending.skills || [],
    }).select().single();

    if (error) throw new Error(error.message);
    await supabaseAdmin.from('hq_pending_users').delete().eq('id', id);
    const { passwordHash, ...safeUser } = rowToUser(data);
    return safeUser;
  },

  async rejectUser(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_pending_users').delete().eq('id', id);
    return !error;
  },

  async getPendingUserStatus(username: string): Promise<'active' | 'pending' | 'not_found'> {
    const { count: activeCount } = await supabaseAdmin
      .from('hq_users').select('id', { count: 'exact', head: true }).ilike('username', username);
    if ((activeCount || 0) > 0) return 'active';
    const { count: pendingCount } = await supabaseAdmin
      .from('hq_pending_users').select('id', { count: 'exact', head: true }).ilike('username', username);
    if ((pendingCount || 0) > 0) return 'pending';
    return 'not_found';
  },

  // ── Projects ─────────────────────────────────────────────────
  async getProjects(): Promise<Project[]> {
    const { data } = await supabaseAdmin.from('hq_projects').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToProject);
  },

  async upsertProject(project: Project): Promise<Project> {
    const { data, error } = await supabaseAdmin.from('hq_projects').upsert({
      id: project.id,
      name: project.name,
      description: project.description,
      owner_id: project.ownerId || null,
      member_ids: project.memberIds,
      start_date: project.startDate,
      target_date: project.targetDate,
      priority: project.priority,
      status: project.status,
      progress: project.progress,
      category: project.category,
      objectives: project.objectives,
      milestones: project.milestones,
      risks: project.risks,
      tags: project.tags,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToProject(data);
  },

  async deleteProject(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_projects').delete().eq('id', id);
    return !error;
  },

  // ── Tasks ────────────────────────────────────────────────────
  async getTasks(): Promise<Task[]> {
    const { data } = await supabaseAdmin.from('hq_tasks').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToTask);
  },

  async upsertTask(task: Task): Promise<Task> {
    const { data, error } = await supabaseAdmin.from('hq_tasks').upsert({
      id: task.id,
      title: task.title,
      description: task.description,
      project_id: task.projectId || null,
      assignee_id: task.assigneeId || null,
      reviewer_id: task.reviewerId || null,
      status: task.status,
      priority: task.priority,
      due_date: task.dueDate || null,
      estimated_hours: task.estimatedHours,
      actual_hours: task.actualHours,
      tags: task.tags,
      subtasks: task.subtasks,
      dependencies: task.dependencies,
      deliverables: task.deliverables,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToTask(data);
  },

  async deleteTask(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_tasks').delete().eq('id', id);
    return !error;
  },

  // ── Meetings ─────────────────────────────────────────────────
  async getMeetings(): Promise<Meeting[]> {
    const { data } = await supabaseAdmin.from('hq_meetings').select('*').order('date', { ascending: false });
    return (data || []).map(rowToMeeting);
  },

  async upsertMeeting(meeting: Meeting): Promise<Meeting> {
    const { data, error } = await supabaseAdmin.from('hq_meetings').upsert({
      id: meeting.id,
      title: meeting.title,
      date: meeting.date,
      time: meeting.time,
      duration: meeting.duration,
      participants: meeting.participants,
      agenda: meeting.agenda,
      notes: meeting.notes,
      action_items: meeting.actionItems,
      project_id: meeting.projectId || null,
      type: meeting.type,
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToMeeting(data);
  },

  async deleteMeeting(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_meetings').delete().eq('id', id);
    return !error;
  },

  // ── CRM Contacts ─────────────────────────────────────────────
  async getCrmContacts(): Promise<CrmContact[]> {
    const { data } = await supabaseAdmin.from('hq_crm_contacts').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToCrmContact);
  },

  async upsertCrmContact(contact: CrmContact): Promise<CrmContact> {
    const { data, error } = await supabaseAdmin.from('hq_crm_contacts').upsert({
      id: contact.id,
      name: contact.name,
      email: contact.email,
      phone: contact.phone,
      designation: contact.designation,
      company: contact.company,
      notes: contact.notes,
      stage: contact.stage,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToCrmContact(data);
  },

  async deleteCrmContact(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_crm_contacts').delete().eq('id', id);
    return !error;
  },

  async bulkInsertCrmContacts(contacts: Omit<CrmContact, 'createdAt' | 'updatedAt'>[]): Promise<number> {
    const rows = contacts.map(c => ({
      id: c.id,
      name: c.name,
      email: c.email,
      phone: c.phone,
      designation: c.designation,
      company: c.company,
      notes: c.notes,
      stage: c.stage,
    }));
    const { error, count } = await supabaseAdmin.from('hq_crm_contacts').insert(rows);
    if (error) throw new Error(error.message);
    return count || contacts.length;
  },

  // ── Leads (investor pipeline) ─────────────────────────────────
  async getLeads(): Promise<Lead[]> {
    const { data } = await supabaseAdmin.from('hq_leads').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToLead);
  },

  async upsertLead(lead: Lead): Promise<Lead> {
    const { data, error } = await supabaseAdmin.from('hq_leads').upsert({
      id: lead.id,
      company_name: lead.companyName,
      type: lead.type,
      location: lead.location,
      contact_name: lead.contactName,
      contact_email: lead.contactEmail,
      contact_phone: lead.contactPhone,
      opportunity: lead.opportunity,
      stage: lead.stage,
      potential_value: lead.potentialValue,
      currency: lead.currency,
      notes: lead.notes,
      assigned_to: lead.assignedTo || null,
      next_follow_up: lead.nextFollowUp || null,
      activities: lead.activities,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToLead(data);
  },

  async deleteLead(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_leads').delete().eq('id', id);
    return !error;
  },

  // ── Investors ─────────────────────────────────────────────────
  async getInvestors(): Promise<Investor[]> {
    const { data } = await supabaseAdmin.from('hq_investors').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToInvestor);
  },

  async upsertInvestor(investor: Investor): Promise<Investor> {
    const { data, error } = await supabaseAdmin.from('hq_investors').upsert({
      id: investor.id,
      name: investor.name,
      firm: investor.firm,
      stage: investor.stage,
      round: investor.round,
      last_contact: investor.lastContact || null,
      next_action: investor.nextAction,
      next_action_date: investor.nextActionDate || null,
      ticket_size: investor.ticketSize,
      investment_thesis: investor.investmentThesis,
      notes: investor.notes,
      contact_person: investor.contactPerson,
      deck_version_sent: investor.deckVersionSent,
      interactions: investor.interactions,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToInvestor(data);
  },

  async deleteInvestor(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_investors').delete().eq('id', id);
    return !error;
  },

  // ── R&D Projects ──────────────────────────────────────────────
  async getRDProjects(): Promise<RDProject[]> {
    const { data } = await supabaseAdmin.from('hq_rd_projects').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToRDProject);
  },

  async upsertRDProject(rdProject: RDProject): Promise<RDProject> {
    const { data, error } = await supabaseAdmin.from('hq_rd_projects').upsert({
      id: rdProject.id,
      title: rdProject.title,
      research_question: rdProject.researchQuestion,
      lead_id: rdProject.leadId || null,
      supervisor_id: rdProject.supervisorId || null,
      stage: rdProject.stage,
      dataset: rdProject.dataset,
      hypothesis: rdProject.hypothesis,
      methodology: rdProject.methodology,
      experiments: rdProject.experiments,
      progress: rdProject.progress,
      project_id: rdProject.projectId || null,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToRDProject(data);
  },

  // ── Goals / OKRs ──────────────────────────────────────────────
  async getGoals(): Promise<Goal[]> {
    const { data } = await supabaseAdmin.from('hq_goals').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToGoal);
  },

  async upsertGoal(goal: Goal): Promise<Goal> {
    const { data, error } = await supabaseAdmin.from('hq_goals').upsert({
      id: goal.id,
      quarter: goal.quarter,
      objective: goal.objective,
      progress: goal.progress,
      key_results: goal.keyResults,
      created_by: goal.createdBy,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToGoal(data);
  },

  async deleteGoal(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_goals').delete().eq('id', id);
    return !error;
  },

  // ── Wiki Docs ─────────────────────────────────────────────────
  async getWikiDocs(): Promise<WikiDoc[]> {
    const { data } = await supabaseAdmin.from('hq_wiki_docs').select('*').order('created_at', { ascending: false });
    return (data || []).map(rowToWikiDoc);
  },

  async upsertWikiDoc(doc: WikiDoc): Promise<WikiDoc> {
    const { data, error } = await supabaseAdmin.from('hq_wiki_docs').upsert({
      id: doc.id,
      title: doc.title,
      description: doc.description,
      category: doc.category,
      content: doc.content,
      read_time: doc.readTime,
      google_drive_url: doc.googleDriveUrl,
      created_by: doc.createdBy,
      updated_at: new Date().toISOString(),
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToWikiDoc(data);
  },

  async deleteWikiDoc(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_wiki_docs').delete().eq('id', id);
    return !error;
  },

  // ── Progress Categories ───────────────────────────────────────
  async getProgressCategories(): Promise<ProgressCategory[]> {
    const { data } = await supabaseAdmin.from('hq_progress_categories').select('*').order('created_at', { ascending: true });
    return (data || []).map(rowToProgressCategory);
  },

  async upsertProgressCategory(cat: ProgressCategory): Promise<ProgressCategory> {
    const { data, error } = await supabaseAdmin.from('hq_progress_categories').upsert({
      id: cat.id,
      name: cat.name,
      sub_components: cat.subComponents,
    }, { onConflict: 'id' }).select().single();
    if (error) throw new Error(error.message);
    return rowToProgressCategory(data);
  },

  async deleteProgressCategory(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_progress_categories').delete().eq('id', id);
    return !error;
  },

  // ── Daily Progress Logs ───────────────────────────────────────
  async getProgressLogs(userId?: string): Promise<DailyLog[]> {
    let query = supabaseAdmin.from('hq_progress_logs').select('*').order('date', { ascending: false });
    if (userId) query = query.eq('user_id', userId);
    const { data } = await query;
    return (data || []).map(rowToDailyLog);
  },

  async upsertProgressLog(log: DailyLog): Promise<DailyLog> {
    const { data, error } = await supabaseAdmin.from('hq_progress_logs').upsert({
      id: log.id,
      user_id: log.userId,
      date: log.date,
      entries: log.entries,
      submitted_at: new Date().toISOString(),
    }, { onConflict: 'user_id,date' }).select().single();
    if (error) throw new Error(error.message);
    return rowToDailyLog(data);
  },

  async deleteProgressLog(id: string): Promise<boolean> {
    const { error } = await supabaseAdmin.from('hq_progress_logs').delete().eq('id', id);
    return !error;
  },

  // ── Full state pull (for sync API) ───────────────────────────
  async getFullState() {
    const [
      users, projects, tasks, meetings, leads, investors, rdProjects,
      goals, wikiDocs, crmContacts, progressCategories, progressLogs
    ] = await Promise.all([
      this.getAllUsers(),
      this.getProjects(),
      this.getTasks(),
      this.getMeetings(),
      this.getLeads(),
      this.getInvestors(),
      this.getRDProjects(),
      this.getGoals(),
      this.getWikiDocs(),
      this.getCrmContacts(),
      this.getProgressCategories(),
      this.getProgressLogs(),
    ]);
    return {
      users, projects, tasks, meetings, leads, investors, rdProjects,
      goals, wikiDocs, crmContacts, progressCategories, progressLogs,
      lastUpdated: Date.now(),
    };
  },

  // ── Finance ──────────────────────────────────────────────────
  async getFinanceData(): Promise<any | null> {
    const { data, error } = await supabaseAdmin
      .from('hq_finance')
      .select('*')
      .eq('id', 'hq-finance-1')
      .single();
    
    if (error || !data) {
      // Auto-seed if it doesn't exist
      const defaultData = {
        id: 'hq-finance-1',
        runway_months: 14,
        monthly_burn: 420000,
        cash_balance: 5880000,
        committed_arr: 2600000,
        expenses: [
          { category: "Engineering & ML Payroll", amount: 280000, percentage: 67 },
          { category: "Cloud Infrastructure & GPU Compute", amount: 65000, percentage: 15 },
          { category: "Hardware Testbeds & OBD-II Simulators", amount: 35000, percentage: 8 },
          { category: "Legal, Compliance & Patents", amount: 25000, percentage: 6 },
          { category: "SaaS Tooling & Admin", amount: 15000, percentage: 4 }
        ]
      };
      await supabaseAdmin.from('hq_finance').insert(defaultData);
      return rowToFinanceData({ ...defaultData, updated_at: new Date().toISOString() });
    }
    
    return rowToFinanceData(data);
  },

  async updateFinanceData(updates: any): Promise<boolean> {
    const dbUpdates: any = { updated_at: new Date().toISOString() };
    if (updates.runwayMonths !== undefined) dbUpdates.runway_months = updates.runwayMonths;
    if (updates.monthlyBurn !== undefined) dbUpdates.monthly_burn = updates.monthlyBurn;
    if (updates.cashBalance !== undefined) dbUpdates.cash_balance = updates.cashBalance;
    if (updates.committedArr !== undefined) dbUpdates.committed_arr = updates.committedArr;
    if (updates.expenses !== undefined) dbUpdates.expenses = updates.expenses;

    const { error } = await supabaseAdmin
      .from('hq_finance')
      .update(dbUpdates)
      .eq('id', 'hq-finance-1');
    
    if (error) {
      console.error('Update finance error:', error);
      return false;
    }
    return true;
  },

  // ── Fundraising Meta ─────────────────────────────────────────
  async getFundraisingMeta(): Promise<any | null> {
    const { data, error } = await supabaseAdmin
      .from('hq_fundraising_meta')
      .select('*')
      .eq('id', 'hq-fundraising-1')
      .single();
      
    if (error || !data) {
      // Auto-seed if it doesn't exist
      const defaultMeta = {
        id: 'hq-fundraising-1',
        target_amount: 15000000,
        currency: 'USD',
        round_name: 'Series A',
        pitch_deck_version: 'v2.4 Final'
      };
      await supabaseAdmin.from('hq_fundraising_meta').insert(defaultMeta);
      return rowToFundraisingMeta({ ...defaultMeta, updated_at: new Date().toISOString() });
    }
    
    return rowToFundraisingMeta(data);
  },

  async updateFundraisingMeta(updates: any): Promise<boolean> {
    const dbUpdates: any = { updated_at: new Date().toISOString() };
    if (updates.targetAmount !== undefined) dbUpdates.target_amount = updates.targetAmount;
    if (updates.currency !== undefined) dbUpdates.currency = updates.currency;
    if (updates.roundName !== undefined) dbUpdates.round_name = updates.roundName;
    if (updates.pitchDeckVersion !== undefined) dbUpdates.pitch_deck_version = updates.pitchDeckVersion;

    const { error } = await supabaseAdmin
      .from('hq_fundraising_meta')
      .update(dbUpdates)
      .eq('id', 'hq-fundraising-1');
    
    if (error) {
      console.error('Update fundraising meta error:', error);
      return false;
    }
    return true;
  },

  // ── Chat ─────────────────────────────────────────────────────
  async getChatChannelsForUser(userId: string): Promise<ChatChannel[]> {
    // 1. Get channel IDs the user is a member of
    const { data: memberData } = await supabaseAdmin
      .from('hq_chat_members')
      .select('channel_id')
      .eq('user_id', userId);
      
    if (!memberData || memberData.length === 0) return [];
    
    const channelIds = memberData.map(m => m.channel_id);
    
    // 2. Get the channels
    const { data: channels } = await supabaseAdmin
      .from('hq_chat_channels')
      .select('*')
      .in('id', channelIds);
      
    if (!channels) return [];

    // 3. Populate memberIds for these channels
    const { data: allMembers } = await supabaseAdmin
      .from('hq_chat_members')
      .select('channel_id, user_id')
      .in('channel_id', channelIds);

    return channels.map(row => {
      const channel = rowToChatChannel(row);
      if (allMembers) {
        channel.memberIds = allMembers
          .filter(m => m.channel_id === channel.id)
          .map(m => m.user_id);
      }
      return channel;
    });
  },

  async createChatChannel(channel: Omit<ChatChannel, 'createdAt' | 'updatedAt'>, memberIds: string[]): Promise<ChatChannel> {
    const { data, error } = await supabaseAdmin.from('hq_chat_channels').insert({
      id: channel.id,
      name: channel.name,
      description: channel.description || null,
      type: channel.type,
      created_by: channel.createdBy,
    }).select().single();

    if (error) throw new Error(error.message);
    
    // Insert members
    const members = memberIds.map(userId => ({
      channel_id: channel.id,
      user_id: userId,
      role: userId === channel.createdBy ? 'admin' : 'member'
    }));
    
    await supabaseAdmin.from('hq_chat_members').insert(members);

    const newChannel = rowToChatChannel(data);
    newChannel.memberIds = memberIds;
    return newChannel;
  },

  async getChatMessages(channelId: string, limit = 50): Promise<ChatMessage[]> {
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    
    const { data } = await supabaseAdmin
      .from('hq_chat_messages')
      .select('*')
      .eq('channel_id', channelId)
      .gte('created_at', sevenDaysAgo.toISOString())
      .order('created_at', { ascending: false })
      .limit(limit);
      
    return (data || []).map(rowToChatMessage).reverse(); // Return chronological
  },

  async insertChatMessage(message: Omit<ChatMessage, 'createdAt' | 'updatedAt'>): Promise<ChatMessage> {
    const { data, error } = await supabaseAdmin.from('hq_chat_messages').insert({
      id: message.id,
      channel_id: message.channelId,
      sender_id: message.senderId,
      content: message.content,
      references: message.references,
    }).select().single();

    if (error) throw new Error(error.message);
    return rowToChatMessage(data);
  }
};
