import React, { useState } from 'react';
import { Comparison, Evidence } from '../types';
import { ReactCompareSlider, ReactCompareSliderImage } from 'react-compare-slider';
import { Sliders, AlertTriangle, Layers, CheckCircle2 } from 'lucide-react';

interface ComparisonViewProps {
  comparisons: Comparison[];
  evidenceList: Evidence[];
  investigationId: string;
}

export const ComparisonView: React.FC<ComparisonViewProps> = ({
  comparisons,
  evidenceList,
  investigationId,
}) => {
  const [showDiffOverlay, setShowDiffOverlay] = useState<boolean>(false);
  const [selectedCompIndex] = useState<number>(0);

  const activeComp = comparisons[selectedCompIndex];

  // Look up submitted and candidate evidence
  const submittedEv = evidenceList.find((e) => e.kind === 'SUBMITTED_IMAGE');
  const candidateEv = activeComp
    ? evidenceList.find((e) => e.id === activeComp.candidateEvidenceId)
    : null;

  if (!activeComp || activeComp.alignmentStatus === 'NOT_COMPARABLE') {
    return (
      <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-8 text-center space-y-4">
        <AlertTriangle className="h-10 w-10 text-amber-400 mx-auto" />
        <div className="space-y-1">
          <h3 className="text-sm font-semibold text-slate-100 font-mono">
            STATUS: NO_COMPARABLE_SOURCE
          </h3>
          <p className="text-xs text-slate-400 max-w-lg mx-auto">
            Autonomous feature matching did not discover sufficient geometric inliers to project a valid
            perspective alignment. VORZAK never fabricates original images or synthetic ancestor pairs.
          </p>
        </div>
      </div>
    );
  }

  // Parse paramsJson for diff image base64 if available
  let diffImageBase64: string | null = null;
  if (activeComp.paramsJson) {
    try {
      const parsed = JSON.parse(activeComp.paramsJson);
      diffImageBase64 = parsed.diffImageBase64;
    } catch {}
  }

  const subUrl = submittedEv
    ? `/api/investigations/${investigationId}/evidence/${submittedEv.id}`
    : '';
  const candUrl = candidateEv
    ? `/api/investigations/${investigationId}/evidence/${candidateEv.id}`
    : '';

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2 text-xs font-semibold text-slate-300 font-mono uppercase tracking-wider">
          <Sliders className="h-4 w-4 text-emerald-400" />
          <span>Computer Vision Alignment & Difference Mapping</span>
        </div>

        <div className="flex items-center space-x-3">
          {diffImageBase64 && (
            <button
              onClick={() => setShowDiffOverlay(!showDiffOverlay)}
              className={`px-3 py-1.5 rounded-lg text-xs font-mono font-medium flex items-center space-x-1.5 border transition ${
                showDiffOverlay
                  ? 'bg-rose-500/20 text-rose-300 border-rose-500/40'
                  : 'bg-slate-900 text-slate-300 border-slate-800 hover:border-slate-700'
              }`}
            >
              <Layers className="h-3.5 w-3.5" />
              <span>{showDiffOverlay ? 'Hide Difference Mask' : 'Show Unexplained Diff Regions'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
          <span className="text-[10px] text-slate-500 font-mono uppercase block mb-1">Geometric Alignment</span>
          <div className="flex items-center space-x-1.5 text-emerald-400 font-mono font-bold text-sm">
            <CheckCircle2 className="h-4 w-4" />
            <span>{activeComp.alignmentStatus}</span>
          </div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
          <span className="text-[10px] text-slate-500 font-mono uppercase block mb-1">RANSAC Homography Inliers</span>
          <span className="text-slate-100 font-mono font-bold text-sm">
            {activeComp.inlierCount ?? 0} keypoints
          </span>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
          <span className="text-[10px] text-slate-500 font-mono uppercase block mb-1">Normalized SSIM Score</span>
          <span className="text-slate-100 font-mono font-bold text-sm">
            {activeComp.ssimNormalized != null ? activeComp.ssimNormalized.toFixed(4) : 'N/A'}
          </span>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4">
          <span className="text-[10px] text-slate-500 font-mono uppercase block mb-1">Unexplained Diff Regions</span>
          <span
            className={`font-mono font-bold text-sm ${
              (activeComp.diffRegionCount ?? 0) > 0 ? 'text-rose-400' : 'text-emerald-400'
            }`}
          >
            {activeComp.diffRegionCount ?? 0} regions
          </span>
        </div>
      </div>

      {/* Visual Compare Container */}
      <div className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden shadow-2xl p-4">
        {showDiffOverlay && diffImageBase64 ? (
          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs font-mono text-slate-400 mb-1">
              <span>Unexplained Difference Mask (Highlighted in Neon Red):</span>
              <span className="text-rose-400 font-semibold">{activeComp.diffRegionCount} detected regions</span>
            </div>
            <img
              src={`data:image/png;base64,${diffImageBase64}`}
              alt="Difference Map"
              className="max-h-[500px] mx-auto rounded-lg border border-rose-500/30"
            />
          </div>
        ) : (
          <div className="space-y-2">
            <div className="flex items-center justify-between text-xs font-mono text-slate-400 mb-2">
              <span>← Submitted Image</span>
              <span>Earlier Discovered Candidate →</span>
            </div>
            <div className="max-w-2xl mx-auto rounded-lg overflow-hidden border border-slate-800">
              <ReactCompareSlider
                itemOne={<ReactCompareSliderImage src={subUrl} alt="Submitted Image" />}
                itemTwo={<ReactCompareSliderImage src={candUrl} alt="Candidate Image" />}
                className="max-h-[460px] mx-auto"
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
