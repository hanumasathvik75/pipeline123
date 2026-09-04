import { NextResponse } from 'next/server';
import { serverDb } from '@/lib/serverDb';
import bcrypt from 'bcryptjs';

export async function POST(request: Request) {
  try {
    const { username, currentPassword, newPassword } = await request.json();

    if (!username || !currentPassword || !newPassword) {
      return NextResponse.json(
        { error: 'Missing required fields' },
        { status: 400 }
      );
    }

    const user = await serverDb.findUserByUsername(username);

    if (!user) {
      return NextResponse.json(
        { error: 'User not found' },
        { status: 404 }
      );
    }

    const isValid = await serverDb.verifyPassword(currentPassword, user.passwordHash);

    if (!isValid) {
      return NextResponse.json(
        { error: 'Incorrect current password' },
        { status: 401 }
      );
    }

    const newHash = await bcrypt.hash(newPassword, 10);
    const updated = await serverDb.updateUser(user.id, { passwordHash: newHash });

    if (!updated) {
       return NextResponse.json(
        { error: 'Failed to update password' },
        { status: 500 }
      );
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error('Password change error:', error);
    return NextResponse.json(
      { error: error.message || 'Internal server error' },
      { status: 500 }
    );
  }
}
