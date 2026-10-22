import React from 'react';
import { Evidence } from '../types';
import { Clock, Calendar, ExternalLink, ShieldCheck } from 'lucide-react';

interface ProvenanceTimelineProps {
  evidenceList: Evidence[];
}

export const ProvenanceTimeline: React.FC<ProvenanceTimelineProps> = ({ evidenceList }) => {
  // Filter for candidate pages and images with dates
  const datedEvidence = evidenceList
    .filter((e) => e.dateValue || e.sourceUrl)
    .sort((a, b) => {
      const tA = a.dateValue ? new Date(a.dateValue).getTime() : new Date(a.retrievedAt).getTime();
      const tB = b.dateValue ? new Date(b.dateValue).getTime() : new Date(b.retrievedAt).getTime();
      return tA - tB;
    });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2 text-xs font-semibold text-slate-300 font-mono uppercase tracking-wider">
          <Clock className="h-4 w-4 text-emerald-400" />
          <span>Chronological Provenance Timeline</span>
        </div>
        <span className="text-[11px] font-mono text-slate-500">
          *Ordered by publication/indexing timestamp
        </span>
      </div>

      <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-6">
        <div className="relative border-l border-slate-800 ml-4 pl-6 space-y-8">
          {datedEvidence.map((item, idx) => {
            const isEarliest = idx === 0;

            let edgeClass = 'OBSERVED';
            let edgeBadgeColor = 'bg-slate-800 text-slate-300 border-slate-700';

            if (item.sourceTier === 'FACT_CHECKER' || item.sourceTier === 'WIRE') {
              edgeClass = 'STRONGLY_SUPPORTED';
              edgeBadgeColor = 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
            } else if (item.sourceTier === 'SOCIAL') {
              edgeClass = 'WEAK_HYPOTHESIS';
              edgeBadgeColor = 'bg-purple-500/10 text-purple-400 border-purple-500/30';
            } else if (!item.dateValue) {
              edgeClass = 'UNKNOWN';
              edgeBadgeColor = 'bg-slate-900 text-slate-500 border-slate-800';
            }

            return (
              <div key={item.id} className="relative group">
                {/* Timeline Node Dot */}
                <span
                  className={`absolute -left-[31px] top-1.5 h-3.5 w-3.5 rounded-full border-2 ${
                    isEarliest
                      ? 'bg-emerald-400 border-emerald-300 ring-4 ring-emerald-500/20'
                      : 'bg-slate-800 border-slate-700'
                  }`}
                />

                <div className="space-y-2">
                  <div className="flex items-center space-x-2">
                    {isEarliest && (
                      <span className="px-2 py-0.5 bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 rounded text-[10px] font-mono font-bold flex items-center space-x-1">
                        <ShieldCheck className="h-3 w-3" />
                        <span>EARLIEST DISCOVERED APPEARANCE</span>
                      </span>
                    )}
                    <span className={`text-[10px] font-mono px-2 py-0.5 rounded border ${edgeBadgeColor}`}>
                      {edgeClass}
                    </span>
                    <span className="text-[11px] font-mono text-slate-500">
                      {item.sourceTier || 'UNKNOWN TIER'}
                    </span>
                  </div>

                  <div className="bg-slate-950/70 border border-slate-800/80 rounded-lg p-3 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-1.5 text-xs font-mono text-slate-200">
                        <Calendar className="h-3.5 w-3.5 text-slate-400" />
                        <span>
                          {item.dateValue ? new Date(item.dateValue).toLocaleDateString() : 'Date UNKNOWN'}
                        </span>
                        <span className="text-[10px] text-slate-500">({item.dateSource})</span>
                      </div>
                      <span className="text-[10px] font-mono text-slate-500">
                        SHA-256: {item.contentSha256.substring(0, 10)}...
                      </span>
                    </div>

                    {item.sourceUrl ? (
                      <a
                        href={item.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-emerald-400 hover:underline flex items-center space-x-1 truncate"
                      >
                        <span className="truncate">{item.sourceUrl}</span>
                        <ExternalLink className="h-3 w-3 shrink-0" />
                      </a>
                    ) : (
                      <p className="text-xs text-slate-400 font-mono">Local Submitted File</p>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Coverage Note */}
        <div className="mt-8 pt-4 border-t border-slate-800 text-[11px] font-mono text-slate-500 leading-relaxed">
          <strong>Coverage Note:</strong> VORZAK establishes timeline boundaries strictly from public indexed
          sources retrieved via TinyFish and search APIs. Offline archives, unindexed private messaging platforms,
          and deleted pages outside the crawl cache are not certified.
        </div>
      </div>
    </div>
  );
};
