import type { 
  CrmContact, Lead, TeamMember, Project, Task, Meeting, 
  WikiDoc, Investor, RDProject, Goal, DailyLog, FinanceData, FundraisingMeta 
} from './types';

function escapeCSV(val: unknown): string {
  if (val === null || val === undefined) return '""';
  if (typeof val === 'object') {
    return `"${JSON.stringify(val).replace(/"/g, '""')}"`;
  }
  const str = String(val);
  return `"${str.replace(/"/g, '""')}"`;
}

function createCSVString(headers: string[], rows: (string | number | boolean | null | undefined)[][]): string {
  const headerLine = headers.map(h => escapeCSV(h)).join(',');
  const rowLines = rows.map(row => row.map(cell => escapeCSV(cell)).join(','));
  return [headerLine, ...rowLines].join('\r\n');
}

export function downloadCSV(content: string, filename: string) {
  const blob = new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export interface WebsiteDataStore {
  crmContacts: CrmContact[];
  leads: Lead[];
  team: TeamMember[];
  projects: Project[];
  tasks: Task[];
  meetings: Meeting[];
  wikiDocs: WikiDoc[];
  investors: Investor[];
  rdProjects: RDProject[];
  goals: Goal[];
  progressLogs: DailyLog[];
  financeData: FinanceData | null;
  fundraisingMeta: FundraisingMeta | null;
}

export function generateAllDataMasterCSV(data: WebsiteDataStore): string {
  const dateStr = new Date().toISOString().split('T')[0];
  const sections: string[] = [];

  // Master Header
  sections.push([
    `"=== IGNITIONAI HQ - COMPLETE MASTER DATA EXPORT (${dateStr}) ==="`,
    `"Confidential - Admin Only"`,
    `""`,
    `""`,
    `""`,
    `""`,
    `""`,
    `""`,
    `""`,
    `""`
  ].join(','));
  sections.push('');

  // 1. CRM Contacts
  sections.push('"--- [1] CRM CONTACTS DIRECTORY ---"');
  sections.push(createCSVString(
    ['Entity ID', 'Name', 'Company', 'Designation', 'Category / Stage', 'Email', 'Phone', 'Notes', 'Created At'],
    data.crmContacts.map(c => [
      c.id, c.name, c.company, c.designation, c.stage, c.email, c.phone, c.notes, c.createdAt
    ])
  ));
  sections.push('\r\n');

  // 2. Sales Pipeline / Leads
  sections.push('"--- [2] SALES PIPELINE & LEADS ---"');
  sections.push(createCSVString(
    ['Lead ID', 'Company Name', 'Contact Person', 'Opportunity Scope', 'Stage', 'Value (INR/USD)', 'Currency', 'Type', 'Location', 'Assigned Owner ID', 'Notes', 'Next Follow-Up'],
    data.leads.map(l => [
      l.id, l.companyName, l.contactName, l.opportunity, l.stage, l.potentialValue, l.currency, l.type, l.location, l.assignedTo, l.notes, l.nextFollowUp || ''
    ])
  ));
  sections.push('\r\n');

  // 3. Team Members
  sections.push('"--- [3] TEAM DIRECTORY & ROSTER ---"');
  sections.push(createCSVString(
    ['Member ID', 'Full Name', 'Work Email', 'Username', 'Role', 'Designation', 'Department', 'Status', 'Joining Date', 'Skills', 'Active Tasks', 'Overdue Tasks'],
    data.team.map(m => [
      m.id, m.name, m.email || '', m.username, m.role, m.designation || '', m.department, m.status, m.joiningDate, (m.skills || []).join('; '), m.activeTasks || 0, m.overdueTasks || 0
    ])
  ));
  sections.push('\r\n');

  // 4. Projects
  sections.push('"--- [4] ACTIVE PROJECTS & INITIATIVES ---"');
  sections.push(createCSVString(
    ['Project ID', 'Project Name', 'Category', 'Priority', 'Status', 'Progress (%)', 'Start Date', 'Target Date', 'Owner ID', 'Objectives', 'Tags'],
    data.projects.map(p => [
      p.id, p.name, p.category, p.priority, p.status, `${p.progress}%`, p.startDate, p.targetDate, p.ownerId, (p.objectives || []).join('; '), (p.tags || []).join('; ')
    ])
  ));
  sections.push('\r\n');

  // 5. Tasks
  sections.push('"--- [5] TASKS & DELIVERABLES ---"');
  sections.push(createCSVString(
    ['Task ID', 'Task Title', 'Project ID', 'Assignee ID', 'Priority', 'Status', 'Due Date', 'Est Hours', 'Actual Hours', 'Tags', 'Subtasks Completed / Total'],
    data.tasks.map(t => {
      const subtotal = t.subtasks?.length || 0;
      const subdone = t.subtasks?.filter(s => s.completed).length || 0;
      return [
        t.id, t.title, t.projectId, t.assigneeId, t.priority, t.status, t.dueDate, t.estimatedHours, t.actualHours, (t.tags || []).join('; '), `${subdone}/${subtotal}`
      ];
    })
  ));
  sections.push('\r\n');

  // 6. Knowledge Base & Wiki
  sections.push('"--- [6] KNOWLEDGE BASE & TECHNICAL SPECS ---"');
  sections.push(createCSVString(
    ['Doc ID', 'Title', 'Category', 'Read Time', 'Description', 'Google Drive URL', 'Markdown Content', 'Created By', 'Updated At'],
    data.wikiDocs.map(w => [
      w.id, w.title, w.category, w.readTime || '', w.description, w.googleDriveUrl || '', w.content, w.createdBy, w.updatedAt
    ])
  ));
  sections.push('\r\n');

  // 7. Meetings
  sections.push('"--- [7] MEETINGS & MINUTES ---"');
  sections.push(createCSVString(
    ['Meeting ID', 'Title', 'Date', 'Time', 'Duration (min)', 'Type', 'Participants', 'Agenda', 'Notes', 'Action Items Count'],
    data.meetings.map(m => [
      m.id, m.title, m.date, m.time, m.duration, m.type, (m.participants || []).join('; '), (m.agenda || []).join('; '), m.notes, m.actionItems?.length || 0
    ])
  ));
  sections.push('\r\n');

  // 8. Investors & Fundraising
  sections.push('"--- [8] INVESTORS & FUNDRAISING ---"');
  sections.push(createCSVString(
    ['Investor ID', 'Name', 'Firm', 'Stage', 'Round', 'Ticket Size', 'Contact Person', 'Last Contact', 'Next Action', 'Investment Thesis', 'Notes'],
    data.investors.map(inv => [
      inv.id, inv.name, inv.firm || '', inv.stage, inv.round, inv.ticketSize || '', inv.contactPerson || '', inv.lastContact, inv.nextAction, inv.investmentThesis || '', inv.notes
    ])
  ));
  sections.push('\r\n');

  // 9. R&D Projects
  sections.push('"--- [9] R&D & AI MODELS ---"');
  sections.push(createCSVString(
    ['RD ID', 'Title', 'Research Question', 'Stage', 'Progress (%)', 'Lead ID', 'Hypothesis', 'Methodology', 'Experiments Run'],
    data.rdProjects.map(rd => [
      rd.id, rd.title, rd.researchQuestion, rd.stage, `${rd.progress}%`, rd.leadId, rd.hypothesis || '', rd.methodology || '', rd.experiments?.length || 0
    ])
  ));
  sections.push('\r\n');

  // 10. Goals & OKRs
  sections.push('"--- [10] COMPANY GOALS & OKRS ---"');
  sections.push(createCSVString(
    ['Goal ID', 'Quarter', 'Objective', 'Overall Progress (%)', 'Key Results Count', 'Created By'],
    data.goals.map(g => [
      g.id, g.quarter, g.objective, `${g.progress}%`, g.keyResults?.length || 0, g.createdBy
    ])
  ));
  sections.push('\r\n');

  // 11. Daily Progress Logs
  sections.push('"--- [11] ENGINEERING & FIELD PROGRESS LOGS ---"');
  sections.push(createCSVString(
    ['Log ID', 'User ID', 'Date', 'Submitted At', 'Entries Total Hours', 'Entries Summary'],
    data.progressLogs.map(l => {
      const totalHours = l.entries.reduce((sum, e) => sum + (e.hours || 0), 0);
      const summary = l.entries.map(e => `[${e.categoryId}/${e.subComponentId}]: ${e.hours}h - ${e.notes}`).join(' | ');
      return [l.id, l.userId, l.date, l.submittedAt, totalHours, summary];
    })
  ));
  sections.push('\r\n');

  // 12. Financials
  if (data.financeData) {
    sections.push('"--- [12] FINANCIAL OVERVIEW ---"');
    sections.push(createCSVString(
      ['Runway (Months)', 'Monthly Burn (INR)', 'Cash Balance (INR)', 'Committed ARR (INR)', 'Expenses Breakdown', 'Last Updated'],
      [[
        data.financeData.runwayMonths,
        data.financeData.monthlyBurn,
        data.financeData.cashBalance,
        data.financeData.committedArr,
        (data.financeData.expenses || []).map(ex => `${ex.category}: ₹${ex.amount} (${ex.percentage}%)`).join('; '),
        data.financeData.updatedAt
      ]]
    ));
    sections.push('\r\n');
  }

  return sections.join('\r\n');
}

export function exportAllDataMaster(data: WebsiteDataStore) {
  const masterCSV = generateAllDataMasterCSV(data);
  const dateStr = new Date().toISOString().split('T')[0];
  downloadCSV(masterCSV, `ignitionai_complete_website_data_export_${dateStr}.csv`);
}
