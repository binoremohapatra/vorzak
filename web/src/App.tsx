import { useState, useEffect } from 'react';
import { Investigation, Task, Evidence, Comparison, Finding, SseEvent } from './types';
import { Navbar } from './components/Navbar';
import { SubmitScreen } from './components/SubmitScreen';
import { LiveInvestigationScreen } from './components/LiveInvestigationScreen';
import { EvidenceLedger } from './components/EvidenceLedger';
import { ComparisonView } from './components/ComparisonView';
import { ProvenanceTimeline } from './components/ProvenanceTimeline';
import { AssessmentView } from './components/AssessmentView';
import { Activity, ShieldCheck, Sliders, Database, Clock } from 'lucide-react';

export default function App() {
  const [investigation, setInvestigation] = useState<Investigation | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [evidenceList, setEvidenceList] = useState<Evidence[]>([]);
  const [comparisons, setComparisons] = useState<Comparison[]>([]);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [events, setEvents] = useState<SseEvent[]>([]);
  const [streamingUrl, setStreamingUrl] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<'live' | 'assessment' | 'comparison' | 'ledger' | 'timeline'>('live');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [backendOnline, setBackendOnline] = useState<boolean>(false);

  // Health check polling
  useEffect(() => {
    const checkHealth = () => {
      fetch('/api/health')
        .then((res) => {
          if (res.ok) setBackendOnline(true);
          else setBackendOnline(false);
        })
        .catch(() => setBackendOnline(false));
    };

    checkHealth();
    const interval = setInterval(checkHealth, 10000);
    return () => clearInterval(interval);
  }, []);

  // Poll / refresh investigation details
  const refreshInvestigationData = async (invId: string) => {
    try {
      const [invRes, evRes, compRes] = await Promise.all([
        fetch(`/api/investigations/${invId}`),
        fetch(`/api/investigations/${invId}/evidence`),
        fetch(`/api/investigations/${invId}/comparisons`),
      ]);

      if (invRes.ok) {
        const data = await invRes.json();
        setInvestigation(data.investigation);
        setTasks(data.tasks || []);
        setFindings(data.findings || []);
      }
      if (evRes.ok) {
        const evData = await evRes.json();
        setEvidenceList(evData || []);
      }
      if (compRes.ok) {
        const compData = await compRes.json();
        setComparisons(compData || []);
      }
    } catch (err) {
      console.warn('Error refreshing investigation details:', err);
    }
  };

  // Connect to SSE stream
  useEffect(() => {
    if (!investigation) return;

    const eventSource = new EventSource(`/api/investigations/${investigation.id}/events`);

    eventSource.onopen = () => {
      console.log('SSE connected for investigation:', investigation.id);
    };

    eventSource.addEventListener('STATE_CHANGE', (e: MessageEvent) => {
      const data = JSON.parse(e.data);
      setInvestigation((prev) => (prev ? { ...prev, state: data.state } : null));
      setEvents((prev) => [...prev, { id: e.lastEventId, type: 'STATE_CHANGE', ts: new Date().toISOString(), payload: data }]);
      refreshInvestigationData(investigation.id);
    });

    eventSource.addEventListener('TASK_UPDATE', (e: MessageEvent) => {
      const data = JSON.parse(e.data);
      setEvents((prev) => [...prev, { id: e.lastEventId, type: 'TASK_UPDATE', ts: new Date().toISOString(), payload: data }]);
      refreshInvestigationData(investigation.id);
    });

    eventSource.addEventListener('STREAMING_URL', (e: MessageEvent) => {
      const data = JSON.parse(e.data);
      setStreamingUrl(data.url);
      setEvents((prev) => [...prev, { id: e.lastEventId, type: 'STREAMING_URL', ts: new Date().toISOString(), payload: data }]);
    });

    eventSource.addEventListener('INVESTIGATION_COMPLETE', (e: MessageEvent) => {
      const data = JSON.parse(e.data);
      setEvents((prev) => [...prev, { id: e.lastEventId, type: 'INVESTIGATION_COMPLETE', ts: new Date().toISOString(), payload: data }]);
      refreshInvestigationData(investigation.id);
      setActiveTab('assessment');
    });

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [investigation?.id]);

  const handleSubmit = async (file: File | null, imageUrl: string, claimText: string) => {
    setSubmitting(true);
    setSubmitError(null);

    try {
      const formData = new FormData();
      if (file) formData.append('file', file);
      if (imageUrl) formData.append('imageUrl', imageUrl);
      formData.append('claimText', claimText);

      const res = await fetch('/api/investigations', {
        method: 'POST',
        headers: {
          'Idempotency-Key': 'key_' + Date.now(),
        },
        body: formData,
      });

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error || `HTTP ${res.status}`);
      }

      const data = await res.json();
      await refreshInvestigationData(data.id);
      setActiveTab('live');
    } catch (err: any) {
      setSubmitError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleLoadReplay = async (caseId: string) => {
    setSubmitting(true);
    setSubmitError(null);

    try {
      const res = await fetch(`/api/investigations/replay/${caseId}`, { method: 'POST' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      await refreshInvestigationData(data.id);
      setActiveTab('assessment');
    } catch (err: any) {
      setSubmitError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    setInvestigation(null);
    setTasks([]);
    setEvidenceList([]);
    setComparisons([]);
    setFindings([]);
    setEvents([]);
    setStreamingUrl(null);
    setActiveTab('live');
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between selection:bg-cyan-500/30 selection:text-cyan-200">
      <Navbar
        investigation={investigation}
        onLoadReplay={handleLoadReplay}
        onNewInvestigation={handleReset}
        backendOnline={backendOnline}
      />

      <main className="max-w-7xl mx-auto w-full px-4 sm:px-6 py-6 flex-1">
        {!investigation ? (
          <SubmitScreen
            onSubmit={handleSubmit}
            onLoadReplay={handleLoadReplay}
            submitting={submitting}
            error={submitError}
          />
        ) : (
          <div className="space-y-6">
            {/* Investigation Top Banner */}
            <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-mono text-slate-500">CASE:</span>
                  <span className="text-xs font-mono text-slate-200">{investigation.id}</span>
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded ${investigation.mode === 'REPLAY' ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'}`}>
                    {investigation.mode}
                  </span>
                </div>
                <p className="text-sm font-medium text-slate-200 italic">&ldquo;{investigation.claimText}&rdquo;</p>
              </div>

              <div className="flex items-center space-x-3 text-xs font-mono">
                <div className="text-right">
                  <span className="text-slate-500 block text-[10px]">PIPELINE STATUS:</span>
                  <span className="text-emerald-400 font-bold">{investigation.state}</span>
                </div>
              </div>
            </div>

            {/* Navigation Tabs */}
            <div className="flex items-center space-x-2 border-b border-slate-800 pb-2 overflow-x-auto text-xs font-mono">
              <button
                onClick={() => setActiveTab('live')}
                className={`px-3 py-1.5 rounded-lg flex items-center space-x-2 transition ${
                  activeTab === 'live'
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-bold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Activity className="h-4 w-4" />
                <span>Telemetry & Stream</span>
              </button>

              <button
                onClick={() => setActiveTab('assessment')}
                className={`px-3 py-1.5 rounded-lg flex items-center space-x-2 transition ${
                  activeTab === 'assessment'
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-bold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <ShieldCheck className="h-4 w-4" />
                <span>Assessment & Verdicts ({findings.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('comparison')}
                className={`px-3 py-1.5 rounded-lg flex items-center space-x-2 transition ${
                  activeTab === 'comparison'
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-bold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Sliders className="h-4 w-4" />
                <span>Visual Comparison</span>
              </button>

              <button
                onClick={() => setActiveTab('ledger')}
                className={`px-3 py-1.5 rounded-lg flex items-center space-x-2 transition ${
                  activeTab === 'ledger'
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-bold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Database className="h-4 w-4" />
                <span>Evidence Ledger ({evidenceList.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('timeline')}
                className={`px-3 py-1.5 rounded-lg flex items-center space-x-2 transition ${
                  activeTab === 'timeline'
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 font-bold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <Clock className="h-4 w-4" />
                <span>Timeline</span>
              </button>
            </div>

            {/* Tab Contents */}
            {activeTab === 'live' && (
              <LiveInvestigationScreen
                state={investigation.state}
                tasks={tasks}
                events={events}
                streamingUrl={streamingUrl}
              />
            )}

            {activeTab === 'assessment' && (
              <AssessmentView
                investigation={investigation}
                findings={findings}
              />
            )}

            {activeTab === 'comparison' && (
              <ComparisonView
                comparisons={comparisons}
                evidenceList={evidenceList}
                investigationId={investigation.id}
              />
            )}

            {activeTab === 'ledger' && (
              <EvidenceLedger
                evidenceList={evidenceList}
              />
            )}

            {activeTab === 'timeline' && (
              <ProvenanceTimeline
                evidenceList={evidenceList}
              />
            )}
          </div>
        )}
      </main>

      {/* Mandatory Non-Negotiable Rule 5 Integrity Disclaimer */}
      <footer className="border-t border-slate-900 bg-slate-950/80 px-6 py-4 text-center text-xs font-mono text-slate-500">
        <p>Integrity-checked evidence log. Not a legal certification and not proof of authenticity.</p>
      </footer>
    </div>
  );
}
