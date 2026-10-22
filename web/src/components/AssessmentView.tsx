import React from 'react';
import { Finding, Investigation } from '../types';
import { AlertOctagon, HelpCircle, FileSearch, CheckCircle2 } from 'lucide-react';

interface AssessmentViewProps {
  investigation: Investigation;
  findings: Finding[];
}

export const AssessmentView: React.FC<AssessmentViewProps> = ({ investigation, findings }) => {
  // Determine primary outcome
  const primaryFinding = findings[0];
  const outcome = primaryFinding ? primaryFinding.outcome : 'NO_COMPARABLE_SOURCE';
  const confidence = primaryFinding ? primaryFinding.confidence : 'LOW';

  let bannerBg = 'bg-slate-900 border-slate-800';
  let bannerTextColor = 'text-slate-100';
  let IconComponent = HelpCircle;

  if (outcome === 'CONTRADICTED') {
    bannerBg = 'bg-rose-500/10 border-rose-500/30';
    bannerTextColor = 'text-rose-400';
    IconComponent = AlertOctagon;
  } else if (outcome === 'SUPPORTED') {
    bannerBg = 'bg-emerald-500/10 border-emerald-500/30';
    bannerTextColor = 'text-emerald-400';
    IconComponent = CheckCircle2;
  } else if (outcome === 'INCONCLUSIVE') {
    bannerBg = 'bg-amber-500/10 border-amber-500/30';
    bannerTextColor = 'text-amber-400';
    IconComponent = HelpCircle;
  } else if (outcome === 'NO_COMPARABLE_SOURCE') {
    bannerBg = 'bg-slate-900/60 border-slate-800';
    bannerTextColor = 'text-slate-300';
    IconComponent = FileSearch;
  }

  return (
    <div className="space-y-6">
      {/* Primary Assessment Banner */}
      <div className={`border rounded-2xl p-6 ${bannerBg} shadow-2xl`}>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-4">
            <div className={`h-12 w-12 rounded-xl flex items-center justify-center shrink-0 border ${bannerBg}`}>
              <IconComponent className={`h-7 w-7 ${bannerTextColor}`} />
            </div>
            <div>
              <div className="flex items-center space-x-3">
                <span className={`text-xl font-bold font-mono tracking-wide ${bannerTextColor}`}>
                  {outcome}
                </span>
                <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-slate-300">
                  Confidence: {confidence}
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-1">
                Deterministic rule verdict evaluated over typed evidence ledger records.
              </p>
            </div>
          </div>

          <div className="text-right font-mono text-xs text-slate-400">
            <span className="block text-slate-500 text-[10px]">CLAIM UNDER INVESTIGATION:</span>
            <span className="text-slate-200 line-clamp-1 italic max-w-xs">{investigation.claimText}</span>
          </div>
        </div>
      </div>

      {/* Rules That Fired */}
      <div className="space-y-3">
        <h3 className="text-xs font-semibold text-slate-300 font-mono uppercase tracking-wider">
          Deterministic Rules Fired ({findings.length})
        </h3>

        <div className="space-y-3">
          {findings.map((f) => (
            <div key={f.id} className="bg-slate-900/50 border border-slate-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-mono font-bold text-emerald-400">{f.ruleId}</span>
                  <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-800 text-slate-300">
                    {f.outcome} • {f.confidence} CONFIDENCE
                  </span>
                </div>
                <span className="text-[10px] font-mono text-slate-500">
                  Linked Evidence: {f.evidenceIds?.length || 0} records
                </span>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed font-sans">{f.explanation}</p>

              {f.evidenceIds && f.evidenceIds.length > 0 && (
                <div className="pt-2 border-t border-slate-800/80 flex items-center space-x-2 text-[10px] font-mono text-slate-500 overflow-x-auto">
                  <span>Evidence IDs:</span>
                  {f.evidenceIds.map((eid) => (
                    <span key={eid} className="bg-slate-950 px-1.5 py-0.5 rounded text-slate-400 border border-slate-800">
                      {eid.substring(0, 8)}...
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Contradiction Matrix Table */}
      <div className="space-y-3">
        <h3 className="text-xs font-semibold text-slate-300 font-mono uppercase tracking-wider">
          Contradiction Matrix (Claim Aspect vs Evidence Record)
        </h3>

        <div className="bg-slate-900/40 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-slate-400 font-mono uppercase text-[10px]">
              <tr>
                <th className="py-2.5 px-4">Aspect</th>
                <th className="py-2.5 px-4">Claim Text Assertion</th>
                <th className="py-2.5 px-4">Discovered Record</th>
                <th className="py-2.5 px-4">Rule Finding</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono text-xs">
              <tr className="hover:bg-slate-800/20">
                <td className="py-3 px-4 text-slate-400">DATE & EVENT</td>
                <td className="py-3 px-4 text-slate-200">{investigation.claimText}</td>
                <td className="py-3 px-4 text-emerald-400">Discovered historical archival record</td>
                <td className="py-3 px-4">
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${outcome === 'CONTRADICTED' ? 'bg-rose-500/10 text-rose-400' : 'bg-slate-800 text-slate-300'}`}>
                    {outcome}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Limitations Notice */}
      <div className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 text-xs font-mono text-slate-400 space-y-2">
        <div className="text-slate-300 font-semibold flex items-center space-x-2">
          <span>VORZAK Investigation Limitations</span>
        </div>
        <ul className="list-disc list-inside space-y-1 text-slate-400 text-[11px] leading-relaxed">
          <li>Never reports "fake" or "real": findings describe earliest discovered appearances and context contradictions.</li>
          <li>Integrity-checked evidence log: not a legal certification and not proof of authenticity.</li>
          <li>Unexplained difference regions reflect visual pixel disparities, not conclusive proof of intentional tampering.</li>
        </ul>
      </div>
    </div>
  );
};
