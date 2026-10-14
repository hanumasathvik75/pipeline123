import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

const SUPABASE_CONFIGURED =
  !!process.env.NEXT_PUBLIC_SUPABASE_URL && !!process.env.SUPABASE_SERVICE_ROLE_KEY;

export async function POST(request: Request) {
  try {
    const { username, password } = await request.json();

    if (!username || !password) {
      return NextResponse.json(
        { error: 'Username and password are required' },
        { status: 400 }
      );
    }

    // --- LOCAL DEMO MODE (no Supabase configured) ------------------------
    if (!SUPABASE_CONFIGURED) {
      const isMatchingUser = username.toLowerCase() === 'shashank';
      const isMatchingPassword = password === 'Ignition@2026';

      if (!isMatchingUser || !isMatchingPassword) {
        return NextResponse.json(
          { error: 'Invalid credentials. Demo login: shashank / Ignition@2026' },
          { status: 401 }
        );
      }

      return NextResponse.json({
        success: true,
        demo: true,
        user: {
          id: 'user-1',
          name: 'Shashank',
          username: 'shashank',
          email: 'shashank@ignitionai.in',
          role: 'c_level',
          status: 'active',
          designation: 'Founder & CEO',
          department: 'Executive',
          joiningDate: '2024-01-01',
          skills: ['Strategy', 'AI/ML', 'Business Development', 'Fundraising'],
          activeProjects: [],
          activeTasks: 0,
          completedTasks: 0,
          overdueTasks: 0,
        },
      });
    }

    // --- PRODUCTION MODE (Supabase configured) ---------------------------
    const user = await serverDb.findUserByUsername(username);

    if (!user) {
      return NextResponse.json({ error: 'Invalid credentials' }, { status: 401 });
    }

    if (user.status !== 'active') {
      return NextResponse.json(
        { error: `Account is ${user.status}. Please contact an administrator.` },
        { status: 403 }
      );
    }

    const isValid = await serverDb.verifyPassword(password, user.passwordHash);
    if (!isValid) {
      return NextResponse.json({ error: 'Invalid credentials' }, { status: 401 });
    }

    const { passwordHash, ...safeUser } = user;
    return NextResponse.json({ success: true, user: safeUser });
  } catch (error) {
    console.error('Login error:', error);
    return NextResponse.json(
      { error: 'Internal server error during login' },
      { status: 500 }
    );
  }
}
