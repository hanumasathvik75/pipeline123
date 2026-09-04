import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || '';
const supabaseServiceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || '';

if (!supabaseUrl || !supabaseServiceKey) {
  console.warn(
    'Missing Supabase environment variables.\n' +
    'Please set NEXT_PUBLIC_SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY in your .env.local file or Vercel dashboard.'
  );
}

// Server-side client with service role key (bypasses RLS — only used in API routes)
export const supabaseAdmin = supabaseUrl && supabaseServiceKey 
  ? createClient(supabaseUrl, supabaseServiceKey, { auth: { persistSession: false } })
  : ({} as any);
