import React, { useState } from 'react';
import { Evidence } from '../types';
import { Database, ExternalLink, Copy, Check, ShieldCheck, Eye, RefreshCw } from 'lucide-react';

interface EvidenceLedgerProps {
  evidenceList: Evidence[];
}

export const EvidenceLedger: React.FC<EvidenceLedgerProps> = ({ evidenceList }) => {
  const [selectedEvidence, setSelectedEvidence] = useState<Evidence | null>(null);
  const [copiedHash, setCopiedHash] = useState<string | null>(null);
  const [verifyingId, setVerifyingId] = useState<string | null>(null);
  const [verifyStatus, setVerifyStatus] = useState<Record<string, string>>({});
  const [filterTier, setFilterTier] = useState<string>('ALL');

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedHash(text);
    setTimeout(() => setCopiedHash(null), 2000);
  };

  const handleVerify = async (eid: string) => {
    setVerifyingId(eid);
    try {
      const res = await fetch(`/api/evidence/${eid}/verify`, { method: 'POST' });
      const data = await res.json();
      setVerifyStatus((prev) => ({ ...prev, [eid]: data.verificationStatus || 'MATCH' }));
    } catch {
      setVerifyStatus((prev) => ({ ...prev, [eid]: 'UNREACHABLE' }));
    } finally {
      setVerifyingId(null);
    }
  };

  const filtered = evidenceList.filter((ev) => {
    if (filterTier === 'ALL') return true;
    return ev.sourceTier === filterTier;
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center space-x-2 text-xs font-semibold text-slate-300 font-mono uppercase tracking-wider">
          <Database className="h-4 w-4 text-emerald-400" />
          <span>Immutable Evidence Ledger ({evidenceList.length} items recorded)</span>
        </div>

        {/* Tier Filter */}
        <div className="flex items-center space-x-2 text-xs font-mono">
          <span className="text-slate-500">Tier Filter:</span>
          {['ALL', 'FACT_CHECKER', 'WIRE', 'MAJOR_OUTLET', 'SOCIAL', 'OTHER'].map((tier) => (
            <button
              key={tier}
              onClick={() => setFilterTier(tier)}
              className={`px-2 py-0.5 rounded text-[11px] transition ${
                filterTier === tier
                  ? 'bg-slate-700 text-slate-100 font-bold'
                  : 'bg-slate-900 text-slate-400 hover:text-slate-200'
              }`}
            >
              {tier}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-slate-400 font-mono uppercase text-[10px]">
              <tr>
                <th className="py-3 px-4">Kind</th>
                <th className="py-3 px-4">Source Tier</th>
                <th className="py-3 px-4">Publication Date</th>
                <th className="py-3 px-4">Source URL / Target</th>
                <th className="py-3 px-4">SHA-256 (Integrity)</th>
                <th className="py-3 px-4">Mode</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {filtered.map((ev) => (
                <tr key={ev.id} className="hover:bg-slate-800/30 transition">
                  <td className="py-3 px-4 font-mono font-medium text-slate-200">
                    <span className="bg-slate-800 text-slate-300 px-2 py-0.5 rounded text-[10px]">
                      {ev.kind}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-mono font-semibold ${
                        ev.sourceTier === 'FACT_CHECKER'
                          ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/30'
                          : ev.sourceTier === 'WIRE'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                          : ev.sourceTier === 'MAJOR_OUTLET'
                          ? 'bg-blue-500/10 text-blue-400 border border-blue-500/30'
                          : ev.sourceTier === 'SOCIAL'
                          ? 'bg-purple-500/10 text-purple-400 border border-purple-500/30'
                          : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      {ev.sourceTier || 'UNKNOWN'}
                    </span>
                  </td>
                  <td className="py-3 px-4 font-mono text-slate-300">
                    {ev.dateValue ? (
                      <div>
                        <span>{new Date(ev.dateValue).toLocaleDateString()}</span>
                        <span className="block text-[10px] text-slate-500">source: {ev.dateSource}</span>
                      </div>
                    ) : (
                      <span className="text-slate-600 font-mono text-[11px]">UNKNOWN (Not Guessed)</span>
                    )}
                  </td>
                  <td className="py-3 px-4 max-w-xs truncate">
                    {ev.sourceUrl ? (
                      <a
                        href={ev.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-emerald-400 hover:underline flex items-center space-x-1"
                      >
                        <span className="truncate">{ev.sourceUrl}</span>
                        <ExternalLink className="h-3 w-3 shrink-0" />
                      </a>
                    ) : (
                      <span className="text-slate-500 font-mono text-[11px]">Local Submission</span>
                    )}
                  </td>
                  <td className="py-3 px-4 font-mono text-slate-400">
                    <div className="flex items-center space-x-1.5">
                      <span>{ev.contentSha256.substring(0, 10)}...</span>
                      <button
                        onClick={() => copyToClipboard(ev.contentSha256)}
                        title="Copy full SHA-256"
                        className="text-slate-500 hover:text-slate-300"
                      >
                        {copiedHash === ev.contentSha256 ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                      </button>
                    </div>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${ev.mode === 'REPLAY' ? 'text-amber-400 bg-amber-500/10' : 'text-emerald-400 bg-emerald-500/10'}`}>
                      {ev.mode}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <div className="flex items-center justify-end space-x-2">
                      <button
                        onClick={() => handleVerify(ev.id)}
                        disabled={verifyingId === ev.id}
                        className="p-1 text-slate-400 hover:text-emerald-400 transition"
                        title="Verify source SHA-256 integrity"
                      >
                        <RefreshCw className={`h-3.5 w-3.5 ${verifyingId === ev.id ? 'animate-spin text-cyan-400' : ''}`} />
                      </button>
                      {verifyStatus[ev.id] && (
                        <span className="text-[10px] font-mono text-emerald-400">{verifyStatus[ev.id]}</span>
                      )}
                      <button
                        onClick={() => setSelectedEvidence(ev)}
                        className="p-1 text-slate-400 hover:text-slate-200 transition"
                        title="Inspect Evidence Metadata"
                      >
                        <Eye className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Evidence Detail Modal */}
      {selectedEvidence && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-xl max-w-2xl w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div className="flex items-center space-x-2 font-mono text-sm font-semibold text-slate-100">
                <ShieldCheck className="h-4 w-4 text-emerald-400" />
                <span>Evidence Record: {selectedEvidence.kind}</span>
              </div>
              <button
                onClick={() => setSelectedEvidence(null)}
                className="text-slate-400 hover:text-slate-200 text-xs font-mono"
              >
                [CLOSE]
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <span className="text-slate-500 block font-mono">Evidence ID:</span>
                <span className="font-mono text-slate-200">{selectedEvidence.id}</span>
              </div>
              <div>
                <span className="text-slate-500 block font-mono">Full SHA-256 Checksum:</span>
                <span className="font-mono text-slate-300 break-all bg-slate-950 p-2 rounded block">
                  {selectedEvidence.contentSha256}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <span className="text-slate-500 block font-mono">Mode:</span>
                  <span className="font-mono text-slate-200">{selectedEvidence.mode}</span>
                </div>
                <div>
                  <span className="text-slate-500 block font-mono">Source Tier:</span>
                  <span className="font-mono text-slate-200">{selectedEvidence.sourceTier || 'UNKNOWN'}</span>
                </div>
              </div>

              {selectedEvidence.rawJson && (
                <div>
                  <span className="text-slate-500 block font-mono mb-1">Raw Ingestion Payload (JSON):</span>
                  <pre className="bg-slate-950 border border-slate-800 p-3 rounded-lg overflow-x-auto text-[11px] font-mono text-slate-300 max-h-48">
                    {selectedEvidence.rawJson}
                  </pre>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
