import React, { useRef, useEffect } from 'react';
import { Task, SseEvent, InvestigationState } from '../types';
import { Terminal, MonitorPlay, Activity } from 'lucide-react';

interface LiveInvestigationScreenProps {
  state: InvestigationState;
  tasks: Task[];
  events: SseEvent[];
  streamingUrl: string | null;
}

const PIPELINE_STATES: InvestigationState[] = [
  'CREATED',
  'VALIDATING',
  'DISCOVERING',
  'ACQUIRING',
  'COMPARING',
  'ASSESSING',
  'REFUTING',
  'REPORTING',
  'DONE',
];

export const LiveInvestigationScreen: React.FC<LiveInvestigationScreenProps> = ({
  state,
  tasks,
  events,
  streamingUrl,
}) => {
  const eventEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    eventEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events]);

  const currentStateIndex = PIPELINE_STATES.indexOf(state);

  return (
    <div className="space-y-6">
      {/* State Machine Stepper */}
      <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-4 overflow-x-auto">
        <div className="flex items-center justify-between min-w-[700px]">
          {PIPELINE_STATES.map((step, idx) => {
            const isCompleted = currentStateIndex > idx || state === 'DONE';
            const isCurrent = currentStateIndex === idx && state !== 'DONE';

            return (
              <div key={step} className="flex items-center space-x-2">
                <div
                  className={`h-7 px-2.5 rounded flex items-center space-x-1.5 text-[11px] font-mono transition ${
                    isCurrent
                      ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 font-bold'
                      : isCompleted
                      ? 'bg-slate-800/80 text-slate-300'
                      : 'bg-slate-950 text-slate-600 border border-slate-900'
                  }`}
                >
                  {isCurrent && <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping"></span>}
                  <span>{step}</span>
                </div>
                {idx < PIPELINE_STATES.length - 1 && (
                  <div className={`h-[1px] w-4 ${isCompleted ? 'bg-emerald-500/50' : 'bg-slate-800'}`}></div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Main 3-Column / 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Task Status Chips */}
        <div className="lg:col-span-3 space-y-3">
          <div className="flex items-center space-x-2 text-xs font-semibold text-slate-300 font-mono uppercase tracking-wider">
            <Activity className="h-4 w-4 text-emerald-400" />
            <span>Autonomous Tasks</span>
          </div>

          <div className="space-y-2">
            {['SEARCH', 'FETCH', 'AGENT', 'DOWNLOAD', 'COMPARE', 'REFUTE'].map((channel) => {
              const task = tasks.find((t) => t.channel === channel);
              const status = task ? task.status : 'QUEUED';

              let badgeColor = 'bg-slate-950 text-slate-500 border-slate-900';
              if (status === 'RUNNING') badgeColor = 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30 animate-pulse';
              if (status === 'OK') badgeColor = 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
              if (status === 'EMPTY') badgeColor = 'bg-slate-800 text-slate-400 border-slate-700';
              if (status === 'ERROR' || status === 'TIMEOUT') badgeColor = 'bg-red-500/10 text-red-400 border-red-500/30';

              return (
                <div
                  key={channel}
                  className="bg-slate-900/60 border border-slate-800 rounded-lg p-3 flex items-center justify-between"
                >
                  <div>
                    <span className="text-xs font-mono font-semibold text-slate-200">{channel}</span>
                    <p className="text-[10px] text-slate-500 font-mono">
                      {channel === 'SEARCH' && 'Multi-query discovery'}
                      {channel === 'FETCH' && 'Metadata & media extraction'}
                      {channel === 'AGENT' && 'Browser exploration'}
                      {channel === 'DOWNLOAD' && 'SSRF-safe acquisition'}
                      {channel === 'COMPARE' && 'CV alignment & SSIM'}
                      {channel === 'REFUTE' && 'Counter-evidence pass'}
                    </p>
                  </div>
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${badgeColor}`}>
                    {status}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Center: Real-Time Event Log (SSE) */}
        <div className={streamingUrl ? 'lg:col-span-5 space-y-3' : 'lg:col-span-9 space-y-3'}>
          <div className="flex items-center justify-between text-xs font-semibold text-slate-300 font-mono uppercase tracking-wider">
            <div className="flex items-center space-x-2">
              <Terminal className="h-4 w-4 text-emerald-400" />
              <span>Real-Time Evidence Event Stream</span>
            </div>
            <span className="text-[10px] text-slate-500">Resumable SSE (Last-Event-ID)</span>
          </div>

          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 h-[440px] overflow-y-auto font-mono text-xs space-y-2.5">
            {events.length === 0 ? (
              <div className="text-slate-600 flex items-center justify-center h-full">
                <span>Awaiting event telemetry from backend...</span>
              </div>
            ) : (
              events.map((ev, index) => (
                <div key={ev.id || index} className="border-b border-slate-900/80 pb-2">
                  <div className="flex items-center justify-between text-[10px] text-slate-500 mb-0.5">
                    <span className="text-emerald-400 font-semibold">{ev.type}</span>
                    <span>{ev.ts ? new Date(ev.ts).toLocaleTimeString() : new Date().toLocaleTimeString()}</span>
                  </div>
                  <pre className="text-slate-300 text-[11px] whitespace-pre-wrap break-all bg-slate-900/50 p-1.5 rounded">
                    {typeof ev.payload === 'string' ? ev.payload : JSON.stringify(ev.payload, null, 2)}
                  </pre>
                </div>
              ))
            )}
            <div ref={eventEndRef} />
          </div>
        </div>

        {/* Right: Embedded Live Browser Iframe (Shown ONLY when STREAMING_URL exists) */}
        {streamingUrl && (
          <div className="lg:col-span-4 space-y-3">
            <div className="flex items-center space-x-2 text-xs font-semibold text-cyan-400 font-mono uppercase tracking-wider">
              <MonitorPlay className="h-4 w-4" />
              <span>TinyFish Live Browser</span>
            </div>
            <div className="border border-cyan-500/30 rounded-xl overflow-hidden h-[440px] bg-slate-950 shadow-2xl">
              <iframe
                src={streamingUrl}
                title="TinyFish Browser Stream"
                className="w-full h-full border-0"
                allow="autoplay"
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
