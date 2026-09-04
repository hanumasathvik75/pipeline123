'use client';

import { useState } from 'react';
import { useHQStore } from '@/lib/store';
import type { ProgressCategory, ProgressSubComponent, DailyLog } from '@/lib/types';
import {
  ClipboardList, Plus, Settings, Edit2, Trash2, X, Check, Save, User
} from 'lucide-react';

export default function ProgressPage() {
  const { currentUser, progressCategories, progressLogs, team, addProgressCategory, updateProgressCategory, deleteProgressCategory, addProgressLog } = useHQStore();
  
  const isAdmin = currentUser?.role === 'c_level';
  const [activeTab, setActiveTab] = useState<'log' | 'admin'>('log');
  
  // Date selection
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);

  // Log Form State
  const [logEntries, setLogEntries] = useState<Record<string, { hours: number, notes: string }>>({});

  // Admin Config State
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<ProgressCategory | null>(null);
  const [catName, setCatName] = useState('');
  const [subComponents, setSubComponents] = useState<ProgressSubComponent[]>([{ id: `sub-${Date.now()}`, name: '' }]);

  // Admin View Logs State
  const [viewUserLogs, setViewUserLogs] = useState<string>('all');

  // --- Handlers for User Logging ---
  const handleEntryChange = (subId: string, catId: string, field: 'hours' | 'notes', value: any) => {
      setLogEntries(prev => ({
          ...prev,
          [subId]: {
              ...prev[subId],
              categoryId: catId,
              hours: field === 'hours' ? value : (prev[subId]?.hours || 0),
              notes: field === 'notes' ? value : (prev[subId]?.notes || '')
          }
      }));
  };

  const submitLog = () => {
      if(!currentUser) return;
      const entries = Object.entries(logEntries).map(([subId, data]) => ({
          subComponentId: subId,
          categoryId: (data as any).categoryId,
          hours: data.hours,
          notes: data.notes
      })).filter(e => e.hours > 0 || e.notes.trim() !== '');

      if(entries.length === 0) {
          alert('Please enter at least some hours or notes to submit.');
          return;
      }

      addProgressLog({
          userId: currentUser.id,
          date: selectedDate,
          entries
      });
      alert('Daily log submitted successfully!');
      setLogEntries({});
  };

  // --- Handlers for Admin Config ---
  const openCategoryModal = (cat: ProgressCategory | null = null) => {
      if(cat) {
          setEditingCategory(cat);
          setCatName(cat.name);
          setSubComponents(cat.subComponents.length > 0 ? [...cat.subComponents] : [{ id: `sub-${Date.now()}`, name: '' }]);
      } else {
          setEditingCategory(null);
          setCatName('');
          setSubComponents([{ id: `sub-${Date.now()}`, name: '' }]);
      }
      setIsCategoryModalOpen(true);
  };

  const saveCategory = (e: React.FormEvent) => {
      e.preventDefault();
      if(!catName.trim()) return;

      const validSubComponents = subComponents.filter(s => s.name.trim() !== '');

      if(editingCategory) {
          updateProgressCategory(editingCategory.id, {
              name: catName,
              subComponents: validSubComponents
          });
      } else {
          addProgressCategory({
              name: catName,
              subComponents: validSubComponents
          });
      }
      setIsCategoryModalOpen(false);
  };

  const deleteCategory = (id: string) => {
      if(window.confirm('Delete this category?')) {
          deleteProgressCategory(id);
      }
  };

  // Pre-load user's log for the selected date
  const myLogForDate = progressLogs.find(l => l.userId === currentUser?.id && l.date === selectedDate);
  const myTotalHours = myLogForDate ? myLogForDate.entries.reduce((sum, e) => sum + e.hours, 0) : 0;

  return (
    <div className="page animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Daily Progress Logger</h1>
          <p className="page-subtitle">Track daily focus areas, engineering hours, and operational blockers.</p>
        </div>
        {isAdmin && (
             <div style={{ display: 'flex', gap: '0.5rem' }}>
                 <button 
                     className={`btn ${activeTab === 'log' ? 'btn-primary' : 'btn-secondary'}`} 
                     onClick={() => setActiveTab('log')}
                 >
                     My Logs
                 </button>
                 <button 
                     className={`btn ${activeTab === 'admin' ? 'btn-primary' : 'btn-secondary'}`} 
                     onClick={() => setActiveTab('admin')}
                 >
                     <Settings size={15} /> Admin View
                 </button>
             </div>
        )}
      </div>

      {activeTab === 'log' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div className="card" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                      <label style={{ fontWeight: 600 }}>Select Date:</label>
                      <input 
                          type="date" 
                          className="input" 
                          value={selectedDate}
                          onChange={(e) => setSelectedDate(e.target.value)}
                      />
                  </div>
                  {myLogForDate && (
                      <div style={{ color: '#10b981', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <Check size={16} /> Log submitted ({myTotalHours}h total)
                      </div>
                  )}
              </div>

              {progressCategories.length === 0 ? (
                  <div className="card" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                      <ClipboardList size={48} style={{ opacity: 0.2, margin: '0 auto 1rem' }} />
                      <h3>No Categories Defined</h3>
                      <p>An admin needs to configure progress categories before you can log time.</p>
                  </div>
              ) : (
                  <div className="grid-auto">
                      {progressCategories.map(cat => (
                          <div key={cat.id} className="card" style={{ padding: '1.5rem' }}>
                              <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>
                                  {cat.name}
                              </h3>
                              
                              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                  {cat.subComponents.map(sub => {
                                      // If logged already today, show existing data
                                      const existingEntry = myLogForDate?.entries.find(e => e.subComponentId === sub.id);
                                      const hours = existingEntry?.hours || logEntries[sub.id]?.hours || '';
                                      const notes = existingEntry?.notes || logEntries[sub.id]?.notes || '';
                                      
                                      return (
                                          <div key={sub.id} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                  <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>{sub.name}</span>
                                                  <input 
                                                      type="number" 
                                                      className="input" 
                                                      placeholder="Hours" 
                                                      style={{ width: '80px', padding: '0.25rem 0.5rem', height: '30px' }}
                                                      min="0"
                                                      step="0.5"
                                                      value={hours}
                                                      onChange={e => handleEntryChange(sub.id, cat.id, 'hours', Number(e.target.value))}
                                                      disabled={!!myLogForDate}
                                                  />
                                              </div>
                                              <input 
                                                  className="input"
                                                  placeholder="Notes / What was done..."
                                                  style={{ fontSize: '0.75rem', height: '32px' }}
                                                  value={notes}
                                                  onChange={e => handleEntryChange(sub.id, cat.id, 'notes', e.target.value)}
                                                  disabled={!!myLogForDate}
                                              />
                                          </div>
                                      );
                                  })}
                                  {cat.subComponents.length === 0 && (
                                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>No subcomponents defined.</div>
                                  )}
                              </div>
                          </div>
                      ))}
                  </div>
              )}

              {!myLogForDate && progressCategories.length > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1rem' }}>
                      <button className="btn btn-primary" onClick={submitLog}>
                          <Save size={16} /> Submit Daily Log
                      </button>
                  </div>
              )}
          </div>
      )}

      {/* ADMIN VIEW */}
      {isAdmin && activeTab === 'admin' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
              
              {/* Categories Config */}
              <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                      <h2 style={{ fontSize: '1.2rem', fontWeight: 700 }}>Log Categories Config</h2>
                      <button className="btn btn-sm btn-primary" onClick={() => openCategoryModal(null)}>
                          <Plus size={14} /> Add Category
                      </button>
                  </div>
                  
                  <div className="grid-auto">
                      {progressCategories.map(cat => (
                          <div key={cat.id} className="card" style={{ padding: '1rem' }}>
                              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                  <h3 style={{ fontWeight: 700 }}>{cat.name}</h3>
                                  <div style={{ display: 'flex', gap: '0.25rem' }}>
                                      <button className="btn btn-icon btn-ghost btn-sm" onClick={() => openCategoryModal(cat)}><Edit2 size={14}/></button>
                                      <button className="btn btn-icon btn-ghost btn-sm" onClick={() => deleteCategory(cat.id)} style={{ color: '#ef4444' }}><Trash2 size={14}/></button>
                                  </div>
                              </div>
                              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.25rem' }}>
                                  {cat.subComponents.map(sub => (
                                      <span key={sub.id} className="badge" style={{ background: 'var(--bg-elevated)' }}>{sub.name}</span>
                                  ))}
                              </div>
                          </div>
                      ))}
                  </div>
              </div>

              {/* Team Logs View */}
              <div style={{ borderTop: '1px solid var(--border)', paddingTop: '2rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                      <h2 style={{ fontSize: '1.2rem', fontWeight: 700 }}>Team Logs</h2>
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                           <input type="date" className="input" value={selectedDate} onChange={(e) => setSelectedDate(e.target.value)} />
                           <select className="input" value={viewUserLogs} onChange={e => setViewUserLogs(e.target.value)}>
                               <option value="all">All Members</option>
                               {team.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                           </select>
                      </div>
                  </div>

                  <div className="card" style={{ overflow: 'hidden' }}>
                      <table className="data-table">
                          <thead>
                              <tr>
                                  <th>Employee</th>
                                  <th>Category / SubComponent</th>
                                  <th>Hours</th>
                                  <th>Notes</th>
                              </tr>
                          </thead>
                          <tbody>
                              {progressLogs
                                  .filter(l => l.date === selectedDate && (viewUserLogs === 'all' || l.userId === viewUserLogs))
                                  .flatMap(log => {
                                      const member = team.find(m => m.id === log.userId);
                                      return log.entries.map((e, idx) => {
                                          const cat = progressCategories.find(c => c.id === e.categoryId);
                                          const sub = cat?.subComponents.find(s => s.id === e.subComponentId);
                                          return (
                                              <tr key={`${log.id}-${idx}`}>
                                                  {idx === 0 ? (
                                                      <td rowSpan={log.entries.length} style={{ verticalAlign: 'top', borderRight: '1px solid var(--border)' }}>
                                                          <div style={{ fontWeight: 600 }}>{member?.name || 'Unknown'}</div>
                                                      </td>
                                                  ) : null}
                                                  <td>
                                                      <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>{cat?.name}</div>
                                                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{sub?.name}</div>
                                                  </td>
                                                  <td>{e.hours}h</td>
                                                  <td style={{ fontSize: '0.8rem' }}>{e.notes || '—'}</td>
                                              </tr>
                                          );
                                      });
                                  })
                              }
                              {progressLogs.filter(l => l.date === selectedDate && (viewUserLogs === 'all' || l.userId === viewUserLogs)).length === 0 && (
                                  <tr>
                                      <td colSpan={4} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                                          No logs found for this date.
                                      </td>
                                  </tr>
                              )}
                          </tbody>
                      </table>
                  </div>
              </div>

          </div>
      )}

      {/* Admin Category Modal */}
      {isCategoryModalOpen && (
          <div style={{
            position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
            padding: '1rem'
          }}>
            <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 500, padding: '1.75rem', background: 'var(--bg-surface)' }}>
               <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1rem' }}>
                  {editingCategory ? 'Edit Category' : 'New Category'}
               </h3>
               
               <form onSubmit={saveCategory} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                   <div>
                       <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>Category Name</label>
                       <input className="input" value={catName} onChange={e => setCatName(e.target.value)} placeholder="e.g. Technical" required />
                   </div>

                   <div>
                       <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.5rem' }}>Sub-Components</label>
                       {subComponents.map((sub, idx) => (
                           <div key={sub.id} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
                               <input 
                                   className="input" 
                                   value={sub.name} 
                                   onChange={e => {
                                       const newSubs = [...subComponents];
                                       newSubs[idx].name = e.target.value;
                                       setSubComponents(newSubs);
                                   }} 
                                   placeholder="e.g. Architecture, Code Review..." 
                               />
                               <button type="button" className="btn btn-icon btn-ghost" onClick={() => {
                                   if(subComponents.length > 1) {
                                       setSubComponents(subComponents.filter((_, i) => i !== idx));
                                   }
                               }}><X size={14}/></button>
                           </div>
                       ))}
                       <button type="button" className="btn btn-sm btn-secondary" onClick={() => setSubComponents([...subComponents, { id: `sub-${Date.now()}`, name: '' }])}>
                           <Plus size={14} /> Add Sub-Component
                       </button>
                   </div>

                   <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
                       <button type="button" className="btn btn-secondary" onClick={() => setIsCategoryModalOpen(false)}>Cancel</button>
                       <button type="submit" className="btn btn-primary">Save Category</button>
                   </div>
               </form>
            </div>
          </div>
      )}

    </div>
  );
}
