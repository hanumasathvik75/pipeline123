import { NextResponse } from 'next/server';
import { serverDb, UserAccount } from '@/lib/serverDb';
import type { UserRole } from '@/lib/types';

export async function GET() {
  try {
    const users = serverDb.getAllUsers();
    return NextResponse.json({ users });
  } catch (error) {
    console.error('Failed to get users:', error);
    return NextResponse.json({ error: 'Failed to retrieve users' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { name, email, password, designation, role, department, skills } = body;

    if (!name || !email || !password) {
      return NextResponse.json(
        { error: 'Name, email, and password are required' },
        { status: 400 }
      );
    }

    const existing = serverDb.findUserByEmail(email);
    if (existing) {
      return NextResponse.json(
        { error: 'A user with this email address already exists' },
        { status: 409 }
      );
    }

    const newUser: UserAccount = {
      id: `user-${Date.now()}`,
      name: name.trim(),
      email: email.trim().toLowerCase(),
      passwordHash: password.trim(),
      role: (role as UserRole) || 'engineering',
      designation: designation?.trim() || 'Team Member',
      department: department?.trim() || 'Engineering',
      joiningDate: new Date().toISOString().split('T')[0],
      skills: Array.isArray(skills) ? skills : (skills ? skills.split(',').map((s: string) => s.trim()) : ['General']),
      activeProjects: [],
      activeTasks: 0,
      completedTasks: 0,
      overdueTasks: 0,
    };

    serverDb.addUser(newUser);

    const { passwordHash, ...safeUser } = newUser;
    return NextResponse.json({ success: true, user: safeUser }, { status: 201 });
  } catch (error) {
    console.error('Failed to create user:', error);
    return NextResponse.json({ error: 'Failed to create user' }, { status: 500 });
  }
}
