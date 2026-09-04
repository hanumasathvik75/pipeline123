import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';
import type { UserRole, UserStatus } from '@/lib/types';

export async function GET() {
  try {
    const users = await serverDb.getAllUsers();
    return NextResponse.json({ users });
  } catch (error) {
    console.error('Failed to get users:', error);
    return NextResponse.json({ error: 'Failed to retrieve users' }, { status: 500 });
  }
}

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { name, username, password, designation, role, department, skills } = body;

    if (!name || !username || !password) {
      return NextResponse.json(
        { error: 'Name, username, and password are required' },
        { status: 400 }
      );
    }

    const isAvailable = await serverDb.checkUsernameAvailable(username);
    if (!isAvailable) {
      return NextResponse.json(
        { error: 'A user with this username already exists' },
        { status: 409 }
      );
    }

    const passwordHash = await serverDb.hashPassword(password);

    const newUser = await serverDb.addUser({
      id: `user-${Date.now()}`,
      name: name.trim(),
      username: username.trim(),
      passwordHash,
      role: (role as UserRole) || 'intern',
      status: 'active' as UserStatus,
      designation: designation?.trim() || 'Team Member',
      department: department?.trim() || 'General',
      joiningDate: new Date().toISOString().split('T')[0],
      skills: Array.isArray(skills) ? skills : (skills ? skills.split(',').map((s: string) => s.trim()) : ['General']),
    });

    return NextResponse.json({ success: true, user: newUser }, { status: 201 });
  } catch (error) {
    console.error('Failed to create user:', error);
    return NextResponse.json({ error: 'Failed to create user' }, { status: 500 });
  }
}

export async function PUT(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get('id');
    if (!id) return NextResponse.json({ error: 'ID required' }, { status: 400 });

    const updates = await request.json();
    
    // Hash password if updating
    if (updates.password) {
      updates.passwordHash = await serverDb.hashPassword(updates.password);
      delete updates.password;
    }

    const updated = await serverDb.updateUser(id, updates);
    if (!updated) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }
    
    return NextResponse.json({ success: true, user: updated });
  } catch (error) {
    console.error('Failed to update user:', error);
    return NextResponse.json({ error: 'Failed to update user' }, { status: 500 });
  }
}

export async function DELETE(request: Request) {
  try {
    const { searchParams } = new URL(request.url);
    const id = searchParams.get('id');
    if (!id) return NextResponse.json({ error: 'ID required' }, { status: 400 });

    const ok = await serverDb.deleteUser(id);
    if (!ok) {
       return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    return NextResponse.json({ success: true });
  } catch (error) {
     console.error('Failed to delete user:', error);
    return NextResponse.json({ error: 'Failed to delete user' }, { status: 500 });
  }
}
