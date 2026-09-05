'use client';

import { useState } from 'react';
import { 
  Car, 
  Wrench, 
  AlertTriangle, 
  CheckCircle, 
  FileText, 
  Download, 
  Play, 
  Activity, 
  Search, 
  Plus, 
  X, 
  ArrowRight, 
  ShieldCheck, 
  Gauge, 
  Sparkles,
  Layers,
  Thermometer,
  Zap,
  CheckCircle2
} from 'lucide-react';
import { useHQStore } from '@/lib/store';

export interface InspectionCase {
  id: string;
  vin: string;
  vehicle: string;
  fuelType: 'petrol' | 'diesel';
  odometerKm: number;
  bay: string;
  technician: string;
  intakeNotes: string;
  status: 'intake' | 'capturing' | 'triage_ready' | 'verified';
  simConditionScore?: number;
  candidateFault?: string;
  discriminatingTest?: string;
  retestConfirmed?: boolean;
}

const INITIAL_CASES: InspectionCase[] = [
  {
    id: 'CASE-2026-089',
    vin: 'MA3FKB12S00192834',
    vehicle: 'Maruti Suzuki Swift 1.2L K12M (2017)',
    fuelType: 'petrol',
    odometerKm: 64200,
    bay: 'Bay 2 (Diagnostic Lift)',
    technician: 'Ramesh K.',
    intakeNotes: 'Customer complains of slight hesitation during warm idle, no MIL lamp on cluster.',
    status: 'triage_ready',
    simConditionScore: 75,
    candidateFault: 'intake_manifold_gasket, pcv_valve_or_breather_hose',
    discriminatingTest: 'Perform intake smoke test to isolate unmetered air leak post-throttle body.',
    retestConfirmed: false,
  },
  {
    id: 'CASE-2026-090',
    vin: 'MALC351CLDM082194',
    vehicle: 'Hyundai Grand i10 1.2L Kappa (2018)',
    fuelType: 'petrol',
    odometerKm: 48900,
    bay: 'Bay 1 (Inspection Line)',
    technician: 'Vikram S.',
    intakeNotes: 'Heater blowing lukewarm; engine temp gauge stays low after 15 minutes of city driving.',
    status: 'triage_ready',
    simConditionScore: 75,
    candidateFault: 'engine_coolant_thermostat, engine_coolant_temperature_sensor',
    discriminatingTest: 'Measure upper vs lower radiator hose temperatures with infrared pyrometer during warm-up.',
    retestConfirmed: false,
  },
  {
    id: 'CASE-2026-091',
    vin: 'MA3EAA13S00492811',
    vehicle: 'Maruti Suzuki Dzire 1.3L DDiS (2016)',
    fuelType: 'diesel',
    odometerKm: 98400,
    bay: 'Bay 3',
    technician: 'Ramesh K.',
    intakeNotes: 'Routine pre-purchase intake inspection for certified pre-owned warranty listing.',
    status: 'verified',
    simConditionScore: 100,
    retestConfirmed: true,
  },
];

export default function InspectionsPage() {
  const { currentUser } = useHQStore();
  const [cases, setCases] = useState<InspectionCase[]>(INITIAL_CASES);
  const [search, setSearch] = useState('');
  const [selectedCase, setSelectedCase] = useState<InspectionCase | null>(null);
  const [isNewCaseModalOpen, setIsNewCaseModalOpen] = useState(false);
  const [isDriveReplayOpen, setIsDriveReplayOpen] = useState(false);
  const [activeScenario, setActiveScenario] = useState<'healthy_baseline' | 'air_intake_leak' | 'coolant_thermostat_stuck_open' | 'alternator_voltage_sag'>('air_intake_leak');

  // Form State
  const [newVin, setNewVin] = useState('');
  const [newVehicle, setNewVehicle] = useState('Maruti Suzuki Swift 1.2L K12M');
  const [newFuel, setNewFuel] = useState<'petrol' | 'diesel'>('petrol');
  const [newOdo, setNewOdo] = useState('55000');
  const [newBay, setNewBay] = useState('Bay 1');
  const [newNotes, setNewNotes] = useState('');

  const filteredCases = cases.filter(c => 
    c.vehicle.toLowerCase().includes(search.toLowerCase()) ||
    c.vin.toLowerCase().includes(search.toLowerCase()) ||
    c.id.toLowerCase().includes(search.toLowerCase())
  );

  const handleCreateCase = (e: React.FormEvent) => {
    e.preventDefault();
    const newEntry: InspectionCase = {
      id: `CASE-2026-${String(cases.length + 92).padStart(3, '0')}`,
      vin: newVin || 'MA3TMP' + Math.floor(Math.random() * 9000000 + 1000000),
      vehicle: newVehicle,
      fuelType: newFuel,
      odometerKm: Number(newOdo) || 50000,
      bay: newBay,
      technician: currentUser?.name || 'Lead Technician',
      intakeNotes: newNotes || 'Routine acquisition scan',
      status: 'intake',
    };
    setCases([newEntry, ...cases]);
    setIsNewCaseModalOpen(false);
    setNewVin('');
    setNewNotes('');
  };

  const downloadReportCSV = (c: InspectionCase) => {
    const csvContent = `data:text/csv;charset=utf-8,--- IGNITIONAI ADVISORY DIAGNOSTIC REPORT ---
Case ID,${c.id}
Vehicle,${c.vehicle}
VIN,${c.vin}
Odometer,${c.odometerKm} km
Condition Score,${c.simConditionScore || 100}/100 (SIMULATED / NOT VALIDATED)
Technician,${c.technician}
Bay,${c.bay}
Disclaimer,ADVISORY ONLY. Does not certify roadworthiness.
Candidate Fault,${c.candidateFault || 'None (Healthy)'}
Recommended Next Test,${c.discriminatingTest || 'Routine scheduled maintenance'}`;

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `${c.id}_advisory_report.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="page animate-fade-in">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <span className="badge" style={{ background: 'rgba(99,102,241,0.15)', color: '#818cf8', fontWeight: 700 }}>
              DEALERSHIP PIPELINE
            </span>
            <span className="badge" style={{ background: 'rgba(245,158,11,0.15)', color: '#f59e0b', fontWeight: 600 }}>
              SIMULATED / NOT VALIDATED
            </span>
          </div>
          <h1 className="page-title" style={{ marginTop: '0.4rem' }}>
            Workshop Vehicle Triage & Diagnostics
          </h1>
          <p className="page-subtitle">
            Ten-minute drive powertrain capture, L0–L4 evidence hierarchy, and defensible pre-owned advisory reports.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button className="btn btn-secondary" onClick={() => setIsDriveReplayOpen(true)}>
            <Play size={15} />
            Live Drive Replay
          </button>
          <button className="btn btn-primary" onClick={() => setIsNewCaseModalOpen(true)}>
            <Plus size={15} />
            New Inspection
          </button>
        </div>
      </div>

      {/* KPI Stats */}
      <div className="grid-stats">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(99, 102, 241, 0.12)', color: '#818cf8' }}>
            <Car size={20} />
          </div>
          <div>
            <div className="stat-value">{cases.length}</div>
            <div className="stat-label">Active Vehicle Cases</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b' }}>
            <AlertTriangle size={20} />
          </div>
          <div>
            <div className="stat-value">{cases.filter(c => c.candidateFault).length}</div>
            <div className="stat-label">Candidate Triage Findings</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.12)', color: '#10b981' }}>
            <CheckCircle size={20} />
          </div>
          <div>
            <div className="stat-value">{cases.filter(c => c.retestConfirmed).length}</div>
            <div className="stat-label">Post-Repair Verified</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.12)', color: '#38bdf8' }}>
            <Gauge size={20} />
          </div>
          <div>
            <div className="stat-value">100%</div>
            <div className="stat-label">Classical CAN Scope</div>
          </div>
        </div>
      </div>

      {/* Search Input */}
      <div style={{ position: 'relative', maxWidth: 360 }}>
        <Search size={14} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
        <input
          className="input"
          placeholder="Search by VIN, Model, or Case ID..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ paddingLeft: '2.25rem', height: 36, fontSize: '0.825rem' }}
        />
      </div>

      {/* Case Table */}
      <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table" style={{ width: '100%', textAlign: 'left', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: 'var(--bg-surface)', borderBottom: '1px solid var(--border)' }}>
                <th style={{ padding: '0.75rem 1rem', fontSize: '0.75rem', fontWeight: 700 }}>CASE & VIN</th>
                <th style={{ padding: '0.75rem 1rem', fontSize: '0.75rem', fontWeight: 700 }}>VEHICLE & FUEL</th>
                <th style={{ padding: '0.75rem 1rem', fontSize: '0.75rem', fontWeight: 700 }}>BAY & TECH</th>
                <th style={{ padding: '0.75rem 1rem', fontSize: '0.75rem', fontWeight: 700 }}>STATUS</th>
                <th style={{ padding: '0.75rem 1rem', fontSize: '0.75rem', fontWeight: 700 }}>CONDITION SCORE</th>
                <th style={{ padding: '0.75rem 1rem', fontSize: '0.75rem', fontWeight: 700, textAlign: 'right' }}>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              {filteredCases.map(c => (
                <tr key={c.id} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    <div style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--text-primary)' }}>{c.id}</div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>{c.vin}</div>
                  </td>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    <div style={{ fontSize: '0.825rem', fontWeight: 600 }}>{c.vehicle}</div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>
                      {c.fuelType.toUpperCase()} · {c.odometerKm.toLocaleString()} km
                    </div>
                  </td>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    <div style={{ fontSize: '0.8rem', fontWeight: 600 }}>{c.bay}</div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{c.technician}</div>
                  </td>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    {c.status === 'triage_ready' && (
                      <span className="badge" style={{ background: 'rgba(245,158,11,0.15)', color: '#f59e0b', fontSize: '0.7rem' }}>
                        Triage Ready
                      </span>
                    )}
                    {c.status === 'verified' && (
                      <span className="badge" style={{ background: 'rgba(16,185,129,0.15)', color: '#10b981', fontSize: '0.7rem' }}>
                        Verified
                      </span>
                    )}
                    {c.status === 'intake' && (
                      <span className="badge" style={{ background: 'rgba(148,163,184,0.15)', color: '#94a3b8', fontSize: '0.7rem' }}>
                        Intake
                      </span>
                    )}
                  </td>
                  <td style={{ padding: '0.85rem 1rem' }}>
                    {c.simConditionScore ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ 
                          fontSize: '0.9rem', 
                          fontWeight: 800, 
                          color: c.simConditionScore >= 90 ? '#10b981' : '#f59e0b' 
                        }}>
                          {c.simConditionScore}/100
                        </span>
                        <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>(Simulated)</span>
                      </div>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Pending Drive</span>
                    )}
                  </td>
                  <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                      <button className="btn btn-sm btn-secondary" onClick={() => setSelectedCase(c)}>
                        <FileText size={13} />
                        Report
                      </button>
                      <button className="btn btn-sm btn-ghost" onClick={() => downloadReportCSV(c)}>
                        <Download size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Advisory Report Modal */}
      {selectedCase && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.75)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 750, padding: '2rem', maxHeight: '90vh', overflowY: 'auto' }}>
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', borderBottom: '1px solid var(--border)', paddingBottom: '1rem' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <span className="badge" style={{ background: 'rgba(245,158,11,0.15)', color: '#f59e0b', fontWeight: 700 }}>
                    SIMULATED / NOT VALIDATED
                  </span>
                  <span className="badge" style={{ background: 'var(--bg-elevated)', color: 'var(--text-muted)' }}>
                    {selectedCase.id}
                  </span>
                </div>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 700, marginTop: '0.5rem' }}>
                  Powertrain Triage Advisory Blueprint
                </h2>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                  {selectedCase.vehicle} · VIN: {selectedCase.vin}
                </div>
              </div>

              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setSelectedCase(null)}>
                <X size={18} />
              </button>
            </div>

            {/* Disclaimer Alert */}
            <div style={{ background: 'rgba(245,158,11,0.08)', border: '1px solid rgba(245,158,11,0.25)', borderRadius: 'var(--radius)', padding: '0.75rem 1rem', margin: '1rem 0', display: 'flex', gap: '0.75rem', alignItems: 'flex-start' }}>
              <ShieldCheck size={20} style={{ color: '#f59e0b', flexShrink: 0, marginTop: '0.1rem' }} />
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                <strong>Advisory Finding Notice:</strong> IgnitionAI provides recorded data and candidate diagnostic hypotheses. It does not certify roadworthiness or authorize part replacements without physical workshop testing.
              </div>
            </div>

            {/* Evidence Hierarchy */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {/* L0 Facts */}
              <div style={{ background: 'var(--bg-surface)', padding: '1rem', borderRadius: 'var(--radius)', border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                  Level 0: Verified Capture Facts
                </div>
                <ul style={{ fontSize: '0.8rem', color: 'var(--text-primary)', paddingLeft: '1.25rem', margin: 0, lineHeight: 1.6 }}>
                  <li>Complete 10-minute drive logged (3,600 OBD CAN frames received, 0 dropped frames).</li>
                  <li>Classical CAN bus (ISO 15765-4, 500 kbps, 11-bit) with zero bus-off errors.</li>
                  <li>No diagnostic trouble codes (DTCs) cleared during capture.</li>
                </ul>
              </div>

              {/* L1 Subsystems */}
              <div style={{ background: 'var(--bg-surface)', padding: '1rem', borderRadius: 'var(--radius)', border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                  Level 1: Subsystem Operating Envelopes
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                  <div style={{ background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 4 }}>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Fuel Trims (Closed Loop)</div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: selectedCase.candidateFault?.includes('intake') ? '#f59e0b' : '#10b981' }}>
                      {selectedCase.candidateFault?.includes('intake') ? '+19.2% (Lean Bias)' : '+1.4% (Nominal)'}
                    </div>
                  </div>
                  <div style={{ background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 4 }}>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Thermal Trajectory</div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: selectedCase.candidateFault?.includes('coolant') ? '#f59e0b' : '#10b981' }}>
                      {selectedCase.candidateFault?.includes('coolant') ? '54°C (Slow Warmup)' : '88°C (Normal)'}
                    </div>
                  </div>
                  <div style={{ background: 'var(--bg-elevated)', padding: '0.75rem', borderRadius: 4 }}>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Supply Voltage</div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#10b981' }}>
                      14.10 V (Healthy)
                    </div>
                  </div>
                </div>
              </div>

              {/* L2 Component Candidates & Discriminating Tests */}
              {selectedCase.candidateFault ? (
                <div style={{ background: 'rgba(245,158,11,0.05)', border: '1px solid rgba(245,158,11,0.3)', padding: '1rem', borderRadius: 'var(--radius)' }}>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#f59e0b', textTransform: 'uppercase', marginBottom: '0.5rem' }}>
                    Level 2: Component Candidates & Discriminating Tests
                  </div>
                  <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    Candidate Components:
                  </div>
                  <div style={{ fontSize: '0.8rem', color: '#f59e0b', fontFamily: 'monospace', margin: '0.25rem 0 0.75rem' }}>
                    {selectedCase.candidateFault}
                  </div>

                  <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    Required Next Workshop Test (Discriminator):
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.25rem', background: 'var(--bg-surface)', padding: '0.6rem', borderRadius: 4 }}>
                    👉 {selectedCase.discriminatingTest}
                  </div>
                </div>
              ) : (
                <div style={{ background: 'rgba(16,185,129,0.08)', border: '1px solid rgba(16,185,129,0.25)', padding: '1rem', borderRadius: 'var(--radius)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <CheckCircle2 size={24} style={{ color: '#10b981' }} />
                  <div>
                    <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#10b981' }}>No Powertrain Deviations Observed</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>All evaluated subsystem families operated within calibrated BS4 tolerances.</div>
                  </div>
                </div>
              )}
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.5rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Issued by IgnitionAI Monolith Worker · Node.js Core v0.2
              </div>
              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <button className="btn btn-secondary" onClick={() => downloadReportCSV(selectedCase)}>
                  <Download size={14} />
                  Download CSV
                </button>
                <button className="btn btn-primary" onClick={() => setSelectedCase(null)}>
                  Done
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Live Drive Replay Modal */}
      {isDriveReplayOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.75)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 650, padding: '1.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700 }}>Software Simulator: 10-Minute Drive Replay</h3>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Generates raw ISO 15765-4 OBD CAN stream and feeds it through pure TypeScript virtual sensors.
                </p>
              </div>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsDriveReplayOpen(false)}>
                <X size={18} />
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.5rem' }}>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Select Simulation Fault Scenario:</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                {[
                  { id: 'healthy_baseline', label: '1. Healthy Baseline' },
                  { id: 'air_intake_leak', label: '2. Intake Vacuum Leak (+Trim)' },
                  { id: 'coolant_thermostat_stuck_open', label: '3. Thermostat Stuck Open' },
                  { id: 'alternator_voltage_sag', label: '4. Alternator Voltage Sag' },
                ].map(s => (
                  <button 
                    key={s.id} 
                    type="button"
                    className={`btn ${activeScenario === s.id ? 'btn-primary' : 'btn-secondary'}`}
                    onClick={() => setActiveScenario(s.id as any)}
                    style={{ justifyContent: 'flex-start', fontSize: '0.75rem', padding: '0.6rem' }}
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ background: 'var(--bg-surface)', padding: '1rem', borderRadius: 'var(--radius)', border: '1px solid var(--border)', marginBottom: '1.25rem' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Expected Replay Pipeline:</div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                1. <strong>Seeded Generation:</strong> 3,600 raw CAN frames (RPM, Speed, ECT, Trims, VPWR)<br />
                2. <strong>State Machine:</strong> Classifies Idle, Cruise, Accel, Decel with 1s hysteresis<br />
                3. <strong>Virtual Sensors:</strong> Evaluates Fuel Control, Thermal, and Voltage residual rules<br />
                4. <strong>Watermarking:</strong> Output tagged with immutable <code>SIMULATED / NOT VALIDATED</code>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button className="btn btn-ghost" onClick={() => setIsDriveReplayOpen(false)}>Cancel</button>
              <button className="btn btn-primary" onClick={() => {
                const targetCase = cases[0];
                if (targetCase) {
                  setSelectedCase(targetCase);
                }
                setIsDriveReplayOpen(false);
              }}>
                <Play size={14} />
                Execute Simulation Replay
              </button>
            </div>
          </div>
        </div>
      )}

      {/* New Case Intake Modal */}
      {isNewCaseModalOpen && (
        <div style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="card animate-fade-in" style={{ width: '100%', maxWidth: 520, padding: '1.75rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700 }}>Vehicle Intake & Inspection Setup</h3>
              <button className="btn btn-icon btn-ghost btn-sm" onClick={() => setIsNewCaseModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleCreateCase} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                  VIN (17 Characters) *
                </label>
                <input 
                  className="input" 
                  placeholder="e.g. MA3FKB12S00192834" 
                  value={newVin} 
                  onChange={e => setNewVin(e.target.value)} 
                  required 
                />
              </div>

              <div className="grid-2">
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                    Make & Model *
                  </label>
                  <input 
                    className="input" 
                    value={newVehicle} 
                    onChange={e => setNewVehicle(e.target.value)} 
                    required 
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                    Fuel Type *
                  </label>
                  <select 
                    className="input" 
                    value={newFuel} 
                    onChange={e => setNewFuel(e.target.value as any)}
                  >
                    <option value="petrol">Petrol (Gasoline)</option>
                    <option value="diesel">Diesel (MultiJet / CRDi)</option>
                  </select>
                </div>
              </div>

              <div className="grid-2">
                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                    Odometer (km)
                  </label>
                  <input 
                    className="input" 
                    type="number" 
                    value={newOdo} 
                    onChange={e => setNewOdo(e.target.value)} 
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                    Assigned Bay
                  </label>
                  <input 
                    className="input" 
                    value={newBay} 
                    onChange={e => setNewBay(e.target.value)} 
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                  Technician Intake Notes / Observed Symptoms
                </label>
                <textarea 
                  className="input" 
                  rows={2} 
                  placeholder="e.g. Rough idle, hesitation under light acceleration..." 
                  value={newNotes} 
                  onChange={e => setNewNotes(e.target.value)} 
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" className="btn btn-ghost" onClick={() => setIsNewCaseModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Initialize Case</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
