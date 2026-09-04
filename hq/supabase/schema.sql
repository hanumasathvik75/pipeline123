-- ================================================================
-- IgnitionAI HQ — Production PostgreSQL & Realtime Schema
-- ================================================================

-- 1. Users & Team Members Table
CREATE TABLE IF NOT EXISTS public.hq_users (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'engineering',
  designation TEXT NOT NULL DEFAULT 'Team Member',
  department TEXT NOT NULL DEFAULT 'Engineering',
  joining_date DATE NOT NULL DEFAULT CURRENT_DATE,
  skills TEXT[] DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Projects Table
CREATE TABLE IF NOT EXISTS public.hq_projects (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  owner_id TEXT REFERENCES public.hq_users(id),
  member_ids TEXT[] DEFAULT '{}',
  start_date DATE NOT NULL,
  target_date DATE NOT NULL,
  priority TEXT NOT NULL DEFAULT 'medium',
  status TEXT NOT NULL DEFAULT 'planned',
  progress INT NOT NULL DEFAULT 0,
  category TEXT NOT NULL DEFAULT 'product',
  objectives JSONB DEFAULT '[]',
  milestones JSONB DEFAULT '[]',
  risks JSONB DEFAULT '[]',
  tags TEXT[] DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Tasks Table
CREATE TABLE IF NOT EXISTS public.hq_tasks (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  project_id TEXT REFERENCES public.hq_projects(id) ON DELETE CASCADE,
  assignee_id TEXT REFERENCES public.hq_users(id),
  status TEXT NOT NULL DEFAULT 'planned',
  priority TEXT NOT NULL DEFAULT 'medium',
  due_date DATE,
  estimated_hours INT DEFAULT 4,
  actual_hours INT DEFAULT 0,
  tags TEXT[] DEFAULT '{}',
  subtasks JSONB DEFAULT '[]',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Meetings Table
CREATE TABLE IF NOT EXISTS public.hq_meetings (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  date DATE NOT NULL,
  time TEXT NOT NULL,
  duration INT DEFAULT 30,
  participants TEXT[] DEFAULT '{}',
  agenda JSONB DEFAULT '[]',
  notes TEXT,
  action_items JSONB DEFAULT '[]',
  type TEXT NOT NULL DEFAULT 'internal',
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. CRM Leads Table
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
  notes TEXT,
  assigned_to TEXT REFERENCES public.hq_users(id),
  activities JSONB DEFAULT '[]',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Investor CRM Table
CREATE TABLE IF NOT EXISTS public.hq_investors (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  firm TEXT,
  stage TEXT NOT NULL DEFAULT 'research',
  round TEXT DEFAULT 'Seed',
  ticket_size TEXT,
  investment_thesis TEXT,
  next_action TEXT,
  next_action_date DATE,
  notes TEXT,
  deck_version_sent TEXT,
  interactions JSONB DEFAULT '[]',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Supabase Realtime for instant multi-user synchronization
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_users;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_projects;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_tasks;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_meetings;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_leads;
ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_investors;
