-- Migration for Chat Tables (Safe, Idempotent, with Full Role Grants)
-- Run this in your Supabase SQL Editor to fix "permission denied" errors

CREATE TABLE IF NOT EXISTS public.hq_chat_channels (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL CHECK (type IN ('direct', 'group')),
    created_by TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.hq_chat_members (
    channel_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (channel_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.hq_chat_messages (
    id TEXT PRIMARY KEY,
    channel_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    content TEXT NOT NULL,
    "references" JSONB DEFAULT '[]'::jsonb NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Ensure postgres is table owner
ALTER TABLE public.hq_chat_channels OWNER TO postgres;
ALTER TABLE public.hq_chat_members OWNER TO postgres;
ALTER TABLE public.hq_chat_messages OWNER TO postgres;

-- 1. CRITICAL: Grant all permissions to all roles (anon, authenticated, service_role, postgres)
-- This fixes the "permission denied for table hq_chat_channels" error
GRANT ALL ON TABLE public.hq_chat_channels TO postgres, anon, authenticated, service_role;
GRANT ALL ON TABLE public.hq_chat_members TO postgres, anon, authenticated, service_role;
GRANT ALL ON TABLE public.hq_chat_messages TO postgres, anon, authenticated, service_role;

-- 2. Disable Row Level Security (matches rest of HQ schema)
ALTER TABLE public.hq_chat_channels DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_chat_members DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.hq_chat_messages DISABLE ROW LEVEL SECURITY;

-- 3. In case RLS is ever re-enabled, set open policies as a fallback
DO $$
BEGIN
  DROP POLICY IF EXISTS "allow_all_channels" ON public.hq_chat_channels;
  DROP POLICY IF EXISTS "allow_all_members" ON public.hq_chat_members;
  DROP POLICY IF EXISTS "allow_all_messages" ON public.hq_chat_messages;
  
  CREATE POLICY "allow_all_channels" ON public.hq_chat_channels FOR ALL USING (true) WITH CHECK (true);
  CREATE POLICY "allow_all_members" ON public.hq_chat_members FOR ALL USING (true) WITH CHECK (true);
  CREATE POLICY "allow_all_messages" ON public.hq_chat_messages FOR ALL USING (true) WITH CHECK (true);
END $$;

-- 4. Safely add to realtime publication
DO $$
BEGIN
  BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.hq_chat_messages;
  EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN others THEN NULL;
  END;
END $$;
