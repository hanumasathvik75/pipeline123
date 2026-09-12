import { demoSupabaseAdmin } from '@/lib/demoSupabase';
import { Target, Mail, Calendar, User, Building } from 'lucide-react';
import { format } from 'date-fns';

export const revalidate = 0; // Disable caching to always fetch fresh data

export default async function RegistrationsPage() {
  // Check if client is initialized
  if (!demoSupabaseAdmin.from) {
    return (
      <div className="page animate-fade-in">
        <div className="card" style={{ padding: '2rem', textAlign: 'center', color: '#ef4444', border: '1px solid #ef4444' }}>
          <h3 style={{ marginBottom: '1rem', fontWeight: 600 }}>Configuration Missing</h3>
          <p>The external Supabase client is not configured.</p>
          <p style={{ marginTop: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Please ensure you have set NEXT_PUBLIC_DEMO_SUPABASE_URL and DEMO_SUPABASE_SERVICE_KEY in .env.local and <strong>restarted your development server</strong>.
          </p>
        </div>
      </div>
    );
  }

  // Fetch data from the external Supabase project
  let requests = null;
  let error = null;
  try {
    const res = await demoSupabaseAdmin
      .from('demo_requests')
      .select('*')
      .order('created_at', { ascending: false });
    requests = res.data;
    error = res.error;
  } catch (err: any) {
    error = err;
  }

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Website Registrations (Demo Requests)</h1>
          <p className="page-subtitle">
            Form submissions imported from the external Supabase project.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.65rem', alignItems: 'center' }}>
          <div className="badge" style={{ padding: '0.5rem 1rem', fontSize: '0.9rem', background: 'var(--accent)', color: '#fff' }}>
            {requests?.length || 0} Total Requests
          </div>
        </div>
      </div>

      {error ? (
        <div className="card" style={{ padding: '2rem', textAlign: 'center', color: '#ef4444', border: '1px solid #ef4444' }}>
          <h3 style={{ marginBottom: '1rem', fontWeight: 600 }}>Error Loading Data</h3>
          <p>{error.message}</p>
          <p style={{ marginTop: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Please ensure you have set the correct NEXT_PUBLIC_DEMO_SUPABASE_URL and DEMO_SUPABASE_SERVICE_KEY in .env.local
          </p>
        </div>
      ) : (
        <div className="card" style={{ overflow: 'hidden' }}>
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Name</th>
                  <th>Contact</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {requests && requests.length > 0 ? (
                  requests.map((req: any) => (
                    <tr key={req.id || Math.random()} className="hover-row">
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-secondary)' }}>
                          <Calendar size={14} />
                          {req.created_at ? format(new Date(req.created_at), 'MMM dd, yyyy HH:mm') : 'Unknown Date'}
                        </div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                          <User size={14} style={{ color: 'var(--accent)' }} />
                          {req.name || req.full_name || req.first_name || 'N/A'}
                        </div>
                        {(req.company || req.company_name) && (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                            <Building size={12} />
                            {req.company || req.company_name}
                          </div>
                        )}
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-secondary)' }}>
                          <Mail size={14} />
                          <a href={`mailto:${req.email}`} style={{ color: 'var(--accent)' }}>{req.email || 'No email provided'}</a>
                        </div>
                        {req.phone && (
                          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                            {req.phone}
                          </div>
                        )}
                      </td>
                      <td>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', maxWidth: '300px' }}>
                          {req.message || req.details || req.notes || (
                            <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>No additional details</span>
                          )}
                        </div>
                        {/* Displaying extra custom fields dynamically if they exist */}
                        {Object.keys(req).filter(k => !['id', 'created_at', 'name', 'full_name', 'first_name', 'last_name', 'email', 'phone', 'company', 'company_name', 'message', 'details', 'notes'].includes(k)).length > 0 && (
                           <div style={{ marginTop: '0.5rem', display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                             {Object.keys(req).filter(k => !['id', 'created_at', 'name', 'full_name', 'first_name', 'last_name', 'email', 'phone', 'company', 'company_name', 'message', 'details', 'notes'].includes(k)).map(key => (
                               <span key={key} className="badge" style={{ fontSize: '0.7rem', background: 'var(--bg-elevated)', border: '1px solid var(--border)' }}>
                                 {key}: {String(req[key])}
                               </span>
                             ))}
                           </div>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>
                      No demo requests found in the database.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
