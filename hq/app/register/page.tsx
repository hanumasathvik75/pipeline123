'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

import Link from 'next/link';

export default function RegisterPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    name: '',
    username: '',
    password: '',
    confirmPassword: '',
    designation: '',
    department: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [status, setStatus] = useState<'pending' | 'active' | 'rejected' | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    setLoading(true);
    setError('');

    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });
      const data = await res.json();

      if (res.ok) {
        setSubmitted(true);
        setStatus('pending');
      } else {
        setError(data.error || 'Registration failed');
      }
    } catch (err) {
      setError('An error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Poll for status if submitted
  useEffect(() => {
    if (!submitted) return;

    const checkStatus = async () => {
      try {
        const res = await fetch(`/api/auth/status?username=${formData.username}`);
        if (res.ok) {
          const data = await res.json();
          setStatus(data.status);
          if (data.status === 'active') {
            // Automatically redirect to login once approved
            setTimeout(() => {
              router.push('/login');
            }, 3000);
          }
        }
      } catch (err) {
        // ignore
      }
    };

    const interval = setInterval(checkStatus, 5000); // check every 5s
    return () => clearInterval(interval);
  }, [submitted, formData.username, router]);


  if (submitted) {
    return (
      <div style={{ minHeight: '100dvh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-base)', padding: '2rem' }}>
        <div className="card animate-fade-in" style={{ padding: '3rem 2rem', textAlign: 'center', maxWidth: 420, width: '100%' }}>
          {status === 'pending' && (
            <>
              <div style={{ display: 'inline-flex', padding: '1rem', background: 'rgba(0, 0, 0, 0.1)', borderRadius: '50%', marginBottom: '1.5rem' }}>
                <></>
              </div>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1rem', color: 'var(--text-primary)' }}>Request Submitted</h2>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '2rem' }}>
                Your account request for <strong>{formData.username}</strong> has been sent to the administrators for approval.
              </p>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', padding: '1rem', background: 'var(--bg-elevated)', borderRadius: 'var(--radius)' }}>
                This page will automatically redirect to the login screen once your account is approved.
              </div>
            </>
          )}
          
          {status === 'active' && (
            <>
              <div style={{ display: 'inline-flex', padding: '1rem', background: 'rgba(0, 0, 0, 0.1)', borderRadius: '50%', marginBottom: '1.5rem' }}>
                <></>
              </div>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1rem', color: 'var(--text-primary)' }}>Account Approved!</h2>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '2rem' }}>
                Redirecting to login...
              </p>
            </>
          )}

           {status === 'rejected' && (
            <>
              <div style={{ display: 'inline-flex', padding: '1rem', background: 'rgba(0, 0, 0, 0.1)', borderRadius: '50%', marginBottom: '1.5rem' }}>
                <></>
              </div>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1rem', color: 'var(--text-primary)' }}>Request Denied</h2>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '2rem' }}>
                Your account request was declined by an administrator.
              </p>
              <button className="btn btn-secondary" onClick={() => setSubmitted(false)}>Start Over</button>
            </>
          )}
        </div>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100dvh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg-base)', padding: '2rem',
    }}>
      <div style={{
        position: 'fixed', inset: 0, zIndex: 0,
        backgroundImage: 'linear-gradient(rgba(0,0,0,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(0,0,0,0.03) 1px, transparent 1px)',
        backgroundSize: '40px 40px',
      }} />

      <div style={{ position: 'relative', zIndex: 1, width: '100%', maxWidth: 480 }}>
        <div style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
           {/* eslint-disable-next-line @next/next/no-img-element */}
           <img src="/logo.png" alt="IgnitionAI Logo" style={{ width: 180, height: 'auto', objectFit: 'contain', margin: '0 auto 1rem' }} />
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>Request Access</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.5rem' }}>Join the HQ Operating System</p>
        </div>

        <div className="card" style={{ padding: '2rem' }}>
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Full Name *</label>
              <input
                className="input"
                type="text"
                value={formData.name}
                onChange={e => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g. John Doe"
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Designation *</label>
                <input
                  className="input"
                  type="text"
                  value={formData.designation}
                  onChange={e => setFormData({ ...formData, designation: e.target.value })}
                  placeholder="e.g. ML Engineer"
                  required
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Department</label>
                <input
                  className="input"
                  type="text"
                  value={formData.department}
                  onChange={e => setFormData({ ...formData, department: e.target.value })}
                  placeholder="e.g. Engineering"
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Desired Username *</label>
              <div style={{ position: 'relative' }}>
                <></>
                <input
                  className="input"
                  type="text"
                  value={formData.username}
                  onChange={e => setFormData({ ...formData, username: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '') })}
                  placeholder="e.g. jdoe (lowercase, no spaces)"
                  style={{ paddingLeft: '2.25rem' }}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Password *</label>
                <div style={{ position: 'relative' }}>
                  <></>
                  <input
                    className="input"
                    type="password"
                    value={formData.password}
                    onChange={e => setFormData({ ...formData, password: e.target.value })}
                    style={{ paddingLeft: '2.25rem' }}
                    required
                    minLength={6}
                  />
                </div>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.375rem' }}>Confirm *</label>
                <div style={{ position: 'relative' }}>
                  <></>
                  <input
                    className="input"
                    type="password"
                    value={formData.confirmPassword}
                    onChange={e => setFormData({ ...formData, confirmPassword: e.target.value })}
                    style={{ paddingLeft: '2.25rem' }}
                    required
                  />
                </div>
              </div>
            </div>

            {error && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.75rem', background: 'rgba(0, 0, 0, 0.1)', border: '1px solid rgba(0, 0, 0, 0.2)', borderRadius: 8 }}>
                <></>
                <span style={{ fontSize: '0.8rem', color: '#000000' }}>{error}</span>
              </div>
            )}

            <button type="submit" className="btn btn-primary" disabled={loading} style={{ width: '100%', padding: '0.75rem', marginTop: '0.5rem' }}>
              {loading ? <></> : 'Submit Request'}
            </button>
          </form>

          <div style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.8rem' }}>
            <span style={{ color: 'var(--text-muted)' }}>Already have an account? </span>
            <Link href="/login" style={{ color: 'var(--accent)', textDecoration: 'none', fontWeight: 600 }}>
              Sign In
            </Link>
          </div>
        </div>
      </div>
      
      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        .animate-spin { animation: spin 1s linear infinite; }
      `}</style>
    </div>
  );
}
