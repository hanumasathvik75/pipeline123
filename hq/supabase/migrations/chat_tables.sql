-- Migration for Chat Tables

CREATE TABLE IF NOT EXISTS hq_chat_channels (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL CHECK (type IN ('direct', 'group')),
    created_by TEXT NOT NULL REFERENCES hq_users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS hq_chat_members (
    channel_id TEXT NOT NULL REFERENCES hq_chat_channels(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES hq_users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('admin', 'member')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (channel_id, user_id)
);

CREATE TABLE IF NOT EXISTS hq_chat_messages (
    id TEXT PRIMARY KEY,
    channel_id TEXT NOT NULL REFERENCES hq_chat_channels(id) ON DELETE CASCADE,
    sender_id TEXT NOT NULL REFERENCES hq_users(id),
    content TEXT NOT NULL,
    "references" JSONB DEFAULT '[]'::jsonb NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS
ALTER TABLE hq_chat_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE hq_chat_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE hq_chat_messages ENABLE ROW LEVEL SECURITY;

-- Note: In a real environment, you'd define proper RLS policies here. 
-- For the scope of this project where everything uses service role initially or permissive policies
-- we can just create permissive policies for now.
CREATE POLICY "Enable all for authenticated users" ON hq_chat_channels FOR ALL USING (true);
CREATE POLICY "Enable all for authenticated users" ON hq_chat_members FOR ALL USING (true);
CREATE POLICY "Enable all for authenticated users" ON hq_chat_messages FOR ALL USING (true);

-- Enable realtime
alter publication supabase_realtime add table hq_chat_messages;
