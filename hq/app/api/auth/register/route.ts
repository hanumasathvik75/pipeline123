import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';

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
