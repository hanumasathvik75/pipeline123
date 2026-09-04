import fs from 'fs';
import path from 'path';
import { PROJECTS, TASKS, MEETINGS, LEADS, INVESTORS, RD_PROJECTS, TEAM_MEMBERS } from './mockData';
import type { Project, Task, Meeting, Lead, Investor, RDProject, TeamMember } from './types';

export interface UserAccount extends TeamMember {
  passwordHash: string;
}

interface ServerState {
  users: UserAccount[];
  projects: Project[];
  tasks: Task[];
  meetings: Meeting[];
  leads: Lead[];
  investors: Investor[];
  rdProjects: RDProject[];
  lastUpdated: number;
}

const DATA_DIR = path.join(process.cwd(), 'data');
const DATA_FILE = path.join(DATA_DIR, 'shared_state.json');

// Default initial password for seeded team members: "ignition2026"
const INITIAL_USERS: UserAccount[] = TEAM_MEMBERS.map(m => ({
  ...m,
  passwordHash: 'ignition2026', // In production, bcrypt hash; simplified for rapid cross-env zero-dependency portability
}));

function getInitialState(): ServerState {
  return {
    users: INITIAL_USERS,
    projects: PROJECTS,
    tasks: TASKS,
    meetings: MEETINGS,
    leads: LEADS,
    investors: INVESTORS,
    rdProjects: RD_PROJECTS,
    lastUpdated: Date.now(),
  };
}

let inMemoryState: ServerState | null = null;

function loadState(): ServerState {
  if (inMemoryState) return inMemoryState;

  try {
    if (fs.existsSync(DATA_FILE)) {
      const data = fs.readFileSync(DATA_FILE, 'utf-8');
      inMemoryState = JSON.parse(data);
      return inMemoryState!;
    }
  } catch (err) {
    console.warn('Failed to load shared_state.json, using defaults', err);
  }

  inMemoryState = getInitialState();
  saveState(inMemoryState);
  return inMemoryState;
}

function saveState(state: ServerState) {
  inMemoryState = state;
  try {
    if (!fs.existsSync(DATA_DIR)) {
      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
    fs.writeFileSync(DATA_FILE, JSON.stringify(state, null, 2), 'utf-8');
  } catch (err) {
    console.warn('Failed to write shared_state.json to disk', err);
  }
}

export const serverDb = {
  getState(): ServerState {
    return loadState();
  },

  updateTasks(tasks: Task[]) {
    const s = loadState();
    s.tasks = tasks;
    s.lastUpdated = Date.now();
    saveState(s);
    return s;
  },

  updateProjects(projects: Project[]) {
    const s = loadState();
    s.projects = projects;
    s.lastUpdated = Date.now();
    saveState(s);
    return s;
  },

  updateMeetings(meetings: Meeting[]) {
    const s = loadState();
    s.meetings = meetings;
    s.lastUpdated = Date.now();
    saveState(s);
    return s;
  },

  updateLeads(leads: Lead[]) {
    const s = loadState();
    s.leads = leads;
    s.lastUpdated = Date.now();
    saveState(s);
    return s;
  },

  updateInvestors(investors: Investor[]) {
    const s = loadState();
    s.investors = investors;
    s.lastUpdated = Date.now();
    saveState(s);
    return s;
  },

  addUser(user: UserAccount) {
    const s = loadState();
    // Check if user already exists
    const existingIndex = s.users.findIndex(u => u.email.toLowerCase() === user.email.toLowerCase());
    if (existingIndex >= 0) {
      s.users[existingIndex] = { ...s.users[existingIndex], ...user };
    } else {
      s.users.push(user);
    }
    s.lastUpdated = Date.now();
    saveState(s);
    return user;
  },

  findUserByEmail(email: string): UserAccount | undefined {
    const s = loadState();
    return s.users.find(u => u.email.toLowerCase() === email.toLowerCase());
  },

  getAllUsers(): TeamMember[] {
    const s = loadState();
    return s.users.map(({ passwordHash, ...user }) => user);
  }
};
