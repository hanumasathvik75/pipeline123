-- ================================================================
-- IgnitionAI HQ — Full Supabase Schema v2
-- Run this in Supabase SQL Editor
-- ================================================================

-- ── 1. Users (active, approved) ──────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_users (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  username TEXT UNIQUE NOT NULL,
  email TEXT,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'intern' CHECK (role IN ('c_level', 'executive', 'intern')),
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'pending', 'rejected')),
  designation TEXT NOT NULL DEFAULT 'Team Member',
  department TEXT NOT NULL DEFAULT 'General',
  joining_date DATE NOT NULL DEFAULT CURRENT_DATE,
  skills TEXT[] DEFAULT '{}',
  active_projects TEXT[] DEFAULT '{}',
  active_tasks INT DEFAULT 0,
  completed_tasks INT DEFAULT 0,
  overdue_tasks INT DEFAULT 0,
  performance_notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 2. Pending Users (awaiting admin approval) ───────────────
CREATE TABLE IF NOT EXISTS public.hq_pending_users (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  designation TEXT NOT NULL DEFAULT 'Team Member',
  department TEXT NOT NULL DEFAULT 'General',
  skills TEXT[] DEFAULT '{}',
  requested_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 3. Projects ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_projects (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  owner_id TEXT REFERENCES public.hq_users(id) ON DELETE SET NULL,
  member_ids TEXT[] DEFAULT '{}',
  start_date DATE NOT NULL DEFAULT CURRENT_DATE,
  target_date DATE NOT NULL DEFAULT CURRENT_DATE,
  priority TEXT NOT NULL DEFAULT 'medium',
  status TEXT NOT NULL DEFAULT 'planned',
  progress INT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
  category TEXT NOT NULL DEFAULT 'product',
  objectives JSONB DEFAULT '[]',
  milestones JSONB DEFAULT '[]',
  risks JSONB DEFAULT '[]',
  tags TEXT[] DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 4. Tasks ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_tasks (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  project_id TEXT REFERENCES public.hq_projects(id) ON DELETE SET NULL,
  assignee_id TEXT REFERENCES public.hq_users(id) ON DELETE SET NULL,
  reviewer_id TEXT REFERENCES public.hq_users(id) ON DELETE SET NULL,
  status TEXT NOT NULL DEFAULT 'planned',
  priority TEXT NOT NULL DEFAULT 'medium',
  due_date DATE,
  estimated_hours INT DEFAULT 4,
  actual_hours INT DEFAULT 0,
  tags TEXT[] DEFAULT '{}',
  subtasks JSONB DEFAULT '[]',
  dependencies TEXT[] DEFAULT '{}',
  deliverables TEXT[] DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 5. Meetings ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_meetings (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  date DATE NOT NULL,
  time TEXT NOT NULL,
  duration INT DEFAULT 30,
  participants TEXT[] DEFAULT '{}',
  agenda JSONB DEFAULT '[]',
  notes TEXT DEFAULT '',
  action_items JSONB DEFAULT '[]',
  project_id TEXT,
  type TEXT NOT NULL DEFAULT 'internal',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 6. CRM Leads (legacy pipeline) ───────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_leads (
  id TEXT PRIMARY KEY,
  company_name TEXT NOT NULL,
  type TEXT,
  location TEXT,
  contact_name TEXT NOT NULL,
  contact_email TEXT,
  contact_phone TEXT,
  opportunity TEXT,
  stage TEXT NOT NULL DEFAULT 'lead',
  potential_value NUMERIC DEFAULT 0,
  currency TEXT DEFAULT 'INR',
  notes TEXT DEFAULT '',
  assigned_to TEXT,
  next_follow_up DATE,
  activities JSONB DEFAULT '[]',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 7. CRM Contacts (new CSV-importable system) ───────────────
CREATE TABLE IF NOT EXISTS public.hq_crm_contacts (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT,
  phone TEXT,
  designation TEXT,
  company TEXT,
  notes TEXT DEFAULT '',
  stage TEXT NOT NULL DEFAULT 'lead',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 8. Investors ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_investors (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  firm TEXT,
  stage TEXT NOT NULL DEFAULT 'research',
  round TEXT DEFAULT 'Pre-Seed',
  last_contact DATE,
  next_action TEXT DEFAULT '',
  next_action_date DATE,
  ticket_size TEXT,
  investment_thesis TEXT,
  notes TEXT DEFAULT '',
  contact_person TEXT,
  deck_version_sent TEXT,
  interactions JSONB DEFAULT '[]',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 9. R&D Projects ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_rd_projects (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  research_question TEXT,
  lead_id TEXT,
  supervisor_id TEXT,
  stage TEXT NOT NULL DEFAULT 'hypothesis',
  dataset TEXT,
  hypothesis TEXT,
  methodology TEXT,
  experiments JSONB DEFAULT '[]',
  progress INT DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
  project_id TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 10. Goals / OKRs ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_goals (
  id TEXT PRIMARY KEY,
  quarter TEXT NOT NULL,
  objective TEXT NOT NULL,
  progress INT DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
  key_results JSONB DEFAULT '[]',
  created_by TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 11. Knowledge Base / Wiki ─────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_wiki_docs (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  category TEXT NOT NULL DEFAULT 'General',
  content TEXT DEFAULT '',
  read_time TEXT DEFAULT '5 min read',
  google_drive_url TEXT,
  created_by TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 12. Progress Categories ───────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_progress_categories (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  sub_components JSONB DEFAULT '[]',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ── 13. Daily Progress Logs ───────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_progress_logs (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES public.hq_users(id) ON DELETE CASCADE,
  date DATE NOT NULL,
  entries JSONB DEFAULT '[]',
  submitted_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (user_id, date)
);

-- ── 14. Finance Data ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_finance (
  id TEXT PRIMARY KEY,
  runway_months NUMERIC DEFAULT 0,
  monthly_burn NUMERIC DEFAULT 0,
  cash_balance NUMERIC DEFAULT 0,
  committed_arr NUMERIC DEFAULT 0,
  expenses JSONB DEFAULT '[]',
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed initial finance data
INSERT INTO public.hq_finance (
  id, runway_months, monthly_burn, cash_balance, committed_arr, expenses
) VALUES (
  'hq-finance-1', 14, 420000, 5880000, 2600000,
  '[
    {"category": "Engineering & ML Payroll", "amount": 280000, "percentage": 67},
    {"category": "Cloud Infrastructure & GPU Compute", "amount": 65000, "percentage": 15},
    {"category": "Hardware Testbeds & OBD-II Simulators", "amount": 35000, "percentage": 8},
    {"category": "Legal, Compliance & Patents", "amount": 25000, "percentage": 6},
    {"category": "SaaS Tooling & Admin", "amount": 15000, "percentage": 4}
  ]'::jsonb
) ON CONFLICT (id) DO NOTHING;

-- ── 15. Fundraising Meta ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.hq_fundraising_meta (
  id TEXT PRIMARY KEY,
  target_amount NUMERIC DEFAULT 0,
  currency TEXT DEFAULT 'USD',
  round_name TEXT DEFAULT 'Seed',
  pitch_deck_version TEXT DEFAULT 'v1.0',
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed initial fundraising meta
INSERT INTO public.hq_fundraising_meta (
  id, target_amount, currency, round_name, pitch_deck_version
) VALUES (
  'hq-fundraising-1', 750000, 'USD', 'Seed', 'v2.4'
) ON CONFLICT (id) DO NOTHING;

-- ── Seed founder account ─────────────────────────────────────
-- Password: Ignition@2026 (bcrypt hashed)
INSERT INTO public.hq_users (
  id, name, username, email, password_hash, role, status,
  designation, department, joining_date, skills
) VALUES (
  'user-1',
  'Shashank',
  'shashank',
  'shashank@ignitionai.in',
  '$2b$10$FjBa.qpJksQjIJ18aFavPeTmrVQwb74BJtFgZK926koyTmxNhdAgW',
  'c_level',
  'active',
  'Founder & CEO',
  'Executive',
  '2024-01-01',
  ARRAY['Strategy', 'AI/ML', 'Business Development', 'Fundraising']
) ON CONFLICT (id) DO NOTHING;

-- ── Enable Realtime ──────────────────────────────────────────
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_users;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_projects;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_tasks;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_meetings;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_crm_contacts;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_goals;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_wiki_docs;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_progress_logs;

-- ── Disable RLS (custom session auth via our API routes) ─────
ALTER TABLE public.hq_users DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_pending_users DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_projects DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_tasks DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_meetings DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_crm_contacts DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_leads DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_investors DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_rd_projects DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_goals DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_wiki_docs DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_progress_categories DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_progress_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_finance DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_fundraising_meta DISABLE ROW LEVEL SECURITY;
