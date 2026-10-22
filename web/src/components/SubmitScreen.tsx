import React, { useState, useRef } from 'react';
import { UploadCloud, Link as LinkIcon, AlertCircle, Info, Sparkles, CheckCircle2 } from 'lucide-react';

interface SubmitScreenProps {
  onSubmit: (file: File | null, imageUrl: string, claimText: string) => Promise<void>;
  onLoadReplay: (caseId: string) => void;
  submitting: boolean;
  error: string | null;
}

export const SubmitScreen: React.FC<SubmitScreenProps> = ({
  onSubmit,
  onLoadReplay,
  submitting,
  error,
}) => {
  const [activeTab, setActiveTab] = useState<'upload' | 'url'>('upload');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [imageUrl, setImageUrl] = useState<string>('');
  const [claimText, setClaimText] = useState<string>('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (activeTab === 'upload' && !selectedFile) return;
    if (activeTab === 'url' && !imageUrl.trim()) return;
    if (!claimText.trim()) return;

    await onSubmit(selectedFile, imageUrl, claimText);
  };

  const loadExample = (claim: string) => {
    setClaimText(claim);
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 space-y-8">
      {/* Product Mission & Scope Notice */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 backdrop-blur space-y-4">
        <div className="flex items-start space-x-3">
          <Info className="h-5 w-5 text-emerald-400 mt-0.5 shrink-0" />
          <div className="space-y-1">
            <h2 className="text-sm font-semibold text-slate-100">
              What VORZAK Concludes and Reports
            </h2>
            <p className="text-xs text-slate-300 leading-relaxed">
              VORZAK autonomously investigates image provenance across indexed credible records.
              It reports the <strong>earliest discovered appearance</strong> (never claiming original provenance)
              and calculates <strong>unexplained visual difference regions</strong> via deterministic computer vision.
              Verdicts derive strictly from rules over typed evidence—never arbitrary generative LLM scores.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs pt-2 border-t border-slate-800/80">
          <div className="flex items-center space-x-2 text-emerald-400/90 font-mono">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
            <span>Earliest Discovered Date (with source tier)</span>
          </div>
          <div className="flex items-center space-x-2 text-emerald-400/90 font-mono">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
            <span>Deterministic RANSAC & SSIM Alignment</span>
          </div>
          <div className="flex items-center space-x-2 text-slate-400 font-mono">
            <span className="text-amber-400 font-bold">!</span>
            <span>Does not declare subjective &ldquo;fake&rdquo; or &ldquo;real&rdquo;</span>
          </div>
          <div className="flex items-center space-x-2 text-slate-400 font-mono">
            <span className="text-amber-400 font-bold">!</span>
            <span>Missing timestamps remain explicitly UNKNOWN</span>
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 flex items-center space-x-3 text-xs text-red-300">
          <AlertCircle className="h-5 w-5 text-red-400 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Main Form */}
      <form onSubmit={handleSubmit} className="bg-slate-900/40 border border-slate-800 rounded-xl p-6 backdrop-blur space-y-6">
        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2 font-mono">
            1. Submit Suspicious Image
          </label>
          
          <div className="flex space-x-2 mb-4">
            <button
              type="button"
              onClick={() => setActiveTab('upload')}
              className={`px-4 py-2 rounded-lg text-xs font-medium flex items-center space-x-2 transition ${
                activeTab === 'upload'
                  ? 'bg-slate-800 text-slate-100 border border-slate-700'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <UploadCloud className="h-4 w-4" />
              <span>File Upload (JPEG, PNG, WebP)</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('url')}
              className={`px-4 py-2 rounded-lg text-xs font-medium flex items-center space-x-2 transition ${
                activeTab === 'url'
                  ? 'bg-slate-800 text-slate-100 border border-slate-700'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <LinkIcon className="h-4 w-4" />
              <span>Direct Image URL (SSRF Protected)</span>
            </button>
          </div>

          {activeTab === 'upload' ? (
            <div
              onDragOver={(e) => e.preventDefault()}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className="border-2 border-dashed border-slate-800 hover:border-slate-700 rounded-xl p-8 text-center cursor-pointer transition bg-slate-950/40"
            >
              <input
                ref={fileInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                onChange={handleFileChange}
                className="hidden"
              />
              {previewUrl ? (
                <div className="space-y-3">
                  <img src={previewUrl} alt="Preview" className="max-h-48 mx-auto rounded-lg border border-slate-800 shadow" />
                  <p className="text-xs text-slate-400 font-mono">{selectedFile?.name} ({(selectedFile!.size / 1024).toFixed(1)} KB)</p>
                  <p className="text-[11px] text-emerald-400">Click or drop a different file to replace</p>
                </div>
              ) : (
                <div className="space-y-2">
                  <UploadCloud className="h-10 w-10 text-slate-500 mx-auto" />
                  <p className="text-sm font-medium text-slate-200">Drag & drop image here or click to browse</p>
                  <p className="text-xs text-slate-500 font-mono">Max 10MB • Magic byte validated (JPEG, PNG, WebP)</p>
                </div>
              )}
            </div>
          ) : (
            <div>
              <input
                type="url"
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
                placeholder="https://example.com/breaking-image.jpg"
                className="w-full px-4 py-3 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/50 transition font-mono"
              />
              <p className="text-[11px] text-slate-500 mt-1 font-mono">
                Hardened downloader automatically blocks cloud metadata endpoints (169.254.x) and private IP ranges.
              </p>
            </div>
          )}
        </div>

        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider font-mono">
              2. Claim Text Under Investigation
            </label>
            <span className="text-[11px] text-slate-500">Event, date, or location claim to verify</span>
          </div>
          <textarea
            value={claimText}
            onChange={(e) => setClaimText(e.target.value)}
            rows={3}
            placeholder="e.g. 'Photo shows military mobilization in Eastern Europe on October 15, 2024 following border crisis'"
            className="w-full px-4 py-3 bg-slate-950 border border-slate-800 rounded-lg text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500/50 transition"
          />

          {/* Quick Benchmark Templates */}
          <div className="mt-3 flex items-center space-x-2 overflow-x-auto text-xs pb-1">
            <span className="text-slate-500 flex items-center space-x-1 shrink-0 font-mono">
              <Sparkles className="h-3.5 w-3.5 text-amber-400" />
              <span>Presets:</span>
            </span>
            <button
              type="button"
              onClick={() => loadExample("Military armored column active in 2024 border clash")}
              className="px-2.5 py-1 bg-slate-800/80 hover:bg-slate-700 text-slate-300 rounded border border-slate-700/60 whitespace-nowrap text-[11px] transition"
            >
              Case 1: Re-captioned Military Photo
            </button>
            <button
              type="button"
              onClick={() => loadExample("Protest crowd gathering with banners in capital square")}
              className="px-2.5 py-1 bg-slate-800/80 hover:bg-slate-700 text-slate-300 rounded border border-slate-700/60 whitespace-nowrap text-[11px] transition"
            >
              Case 2: Protest Banner Alteration
            </button>
            <button
              type="button"
              onClick={() => onLoadReplay('case-001')}
              className="px-2.5 py-1 bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 rounded border border-amber-500/30 whitespace-nowrap text-[11px] transition"
            >
              Load Replay Fixture (Instant)
            </button>
          </div>
        </div>

        <button
          type="submit"
          disabled={submitting || (!selectedFile && !imageUrl) || !claimText.trim()}
          className="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-slate-950 font-bold rounded-xl text-sm transition shadow-lg flex items-center justify-center space-x-2 font-mono"
        >
          <span>{submitting ? 'Initiating Pipeline...' : 'START AUTONOMOUS INVESTIGATION'}</span>
        </button>
      </form>
    </div>
  );
};
