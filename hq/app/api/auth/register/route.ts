import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';
import bcrypt from 'bcryptjs';

const SUPABASE_CONFIGURED =
  !!process.env.NEXT_PUBLIC_SUPABASE_URL && !!process.env.SUPABASE_SERVICE_ROLE_KEY;

// In-memory pending registrations for local demo mode
// (persists until server restarts — good enough for local preview)
const demoPendingUsers: Record<string, { name: string; username: string; designation: string; department: string; skills: string[] }> = {};

export async function POST(request: Request) {
  try {
    const { name, username, password, confirmPassword, designation, department, skills } = await request.json();

    if (!name || !username || !password || !confirmPassword || !designation) {
      return NextResponse.json(
        { error: 'All fields are required' },
        { status: 400 }
      );
    }

    if (password !== confirmPassword) {
      return NextResponse.json(
        { error: 'Passwords do not match' },
        { status: 400 }
      );
    }

    // --- LOCAL DEMO MODE (no Supabase configured) ------------------------
    if (!SUPABASE_CONFIGURED) {
      // Check username not already taken (reserved: shashank)
      const reserved = ['shashank'];
      if (reserved.includes(username.toLowerCase()) || demoPendingUsers[username.toLowerCase()]) {
        return NextResponse.json(
          { error: 'Username is already taken or pending approval' },
          { status: 409 }
        );
      }

      // Store in-memory pending user
      demoPendingUsers[username.toLowerCase()] = {
        name: name.trim(),
        username: username.trim().toLowerCase(),
        designation: designation.trim(),
        department: department?.trim() || 'General',
        skills: Array.isArray(skills) ? skills : [],
      };

      console.log(`[DEMO] Registration request submitted for: ${username}`);
      return NextResponse.json({ success: true, demo: true }, { status: 201 });
    }

    // --- PRODUCTION MODE (Supabase configured) ---------------------------
    const isAvailable = await serverDb.checkUsernameAvailable(username);
    if (!isAvailable) {
      return NextResponse.json(
        { error: 'Username is already taken or pending approval' },
        { status: 409 }
      );
    }

    const passwordHash = await serverDb.hashPassword(password);

    await serverDb.addPendingUser({
      id: `user-${Date.now()}`,
      name: name.trim(),
      username: username.trim(),
      passwordHash,
      designation: designation.trim(),
      department: department?.trim() || 'General',
      skills: Array.isArray(skills) ? skills : [],
    });

    return NextResponse.json({ success: true }, { status: 201 });
  } catch (error) {
    console.error('Registration error:', error);
    return NextResponse.json(
      { error: 'Internal server error during registration' },
      { status: 500 }
    );
  }
}
