import { ShieldCheck, History, FileDown } from 'lucide-react';
import { Investigation } from '../types';

interface NavbarProps {
  investigation: Investigation | null;
  onLoadReplay: (caseId: string) => void;
  onNewInvestigation: () => void;
  backendOnline: boolean;
}

export const Navbar: React.FC<NavbarProps> = ({
  investigation,
  onLoadReplay,
  onNewInvestigation,
  backendOnline,
}) => {
  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-50 px-6 py-3.5 flex items-center justify-between">
      <div className="flex items-center space-x-4">
        <button
          onClick={onNewInvestigation}
          className="flex items-center space-x-3 text-left focus:outline-none group"
        >
          <div className="h-9 w-9 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 group-hover:border-emerald-500/50 transition">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold tracking-wider text-lg text-slate-100 font-mono">VORZAK</span>
              <span className="text-[10px] bg-slate-800 text-slate-300 border border-slate-700 px-1.5 py-0.5 rounded font-mono">v0.1.0</span>
            </div>
            <p className="text-xs text-slate-400">Evidence-Backed Image Provenance Investigator</p>
          </div>
        </button>

        {/* Permanent Mode Badge */}
        {investigation && (
          <div className="hidden sm:flex items-center space-x-2 pl-4 border-l border-slate-800">
            {investigation.mode === 'REPLAY' ? (
              <span className="bg-amber-500/10 text-amber-400 border border-amber-500/30 px-2.5 py-1 rounded text-xs font-mono font-semibold flex items-center space-x-1.5">
                <History className="h-3.5 w-3.5" />
                <span>REPLAY MODE (Preserved Timestamps)</span>
              </span>
            ) : (
              <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 px-2.5 py-1 rounded text-xs font-mono font-semibold flex items-center space-x-1.5">
                <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
                <span>LIVE INVESTIGATION</span>
              </span>
            )}
          </div>
        )}
      </div>

      <div className="flex items-center space-x-3">
        {investigation ? (
          <>
            <a
              href={`/api/investigations/${investigation.id}/report`}
              target="_blank"
              rel="noreferrer"
              className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-medium flex items-center space-x-2 transition shadow-sm"
            >
              <FileDown className="h-3.5 w-3.5 text-emerald-400" />
              <span>PDF Dossier</span>
            </a>
            <button
              onClick={onNewInvestigation}
              className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-medium rounded-lg text-xs flex items-center space-x-1.5 transition shadow"
            >
              <span>+ New Search</span>
            </button>
          </>
        ) : (
          <button
            onClick={() => onLoadReplay('case-001')}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-amber-300 border border-amber-500/30 rounded-lg text-xs font-mono flex items-center space-x-1.5 transition"
          >
            <History className="h-3.5 w-3.5" />
            <span>Load Replay Demo (Case-001)</span>
          </button>
        )}

        <div className="flex items-center space-x-1.5 bg-slate-950 px-2.5 py-1 rounded border border-slate-800 text-[11px] font-mono text-slate-400">
          <span className={`h-2 w-2 rounded-full ${backendOnline ? 'bg-emerald-500' : 'bg-red-500'}`}></span>
          <span>{backendOnline ? 'API READY' : 'API OFFLINE'}</span>
        </div>
      </div>
    </header>
  );
};
