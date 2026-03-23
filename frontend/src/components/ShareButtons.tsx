"use client";

import { useState } from "react";

interface ShareButtonsProps {
  url: string;
  title: string;
}

export default function ShareButtons({ url, title }: ShareButtonsProps) {
  const [copied, setCopied] = useState(false);

  const encodedUrl = encodeURIComponent(url);
  const encodedTitle = encodeURIComponent(title);

  const openPopup = (shareUrl: string) => {
    window.open(shareUrl, "_blank", "width=600,height=500,noopener,noreferrer");
  };

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
      const el = document.createElement("textarea");
      el.value = url;
      document.body.appendChild(el);
      el.select();
      document.execCommand("copy");
      document.body.removeChild(el);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="flex items-center gap-2">
      <span className="text-gray-400 text-sm">Chia sẻ:</span>

      {/* Facebook */}
      <button
        onClick={() =>
          openPopup(
            `https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}&quote=${encodedTitle}`
          )
        }
        title="Chia sẻ lên Facebook"
        className="flex items-center justify-center w-8 h-8 rounded-full bg-[#1877F2] hover:opacity-80 transition"
      >
        <svg className="w-4 h-4 text-white fill-current" viewBox="0 0 24 24">
          <path d="M24 12.073C24 5.405 18.627 0 12 0S0 5.405 0 12.073C0 18.1 4.388 23.094 10.125 24v-8.437H7.078v-3.49h3.047V9.41c0-3.025 1.792-4.697 4.533-4.697 1.312 0 2.686.236 2.686.236v2.97h-1.513c-1.491 0-1.956.93-1.956 1.884v2.25h3.328l-.532 3.49h-2.796V24C19.612 23.094 24 18.1 24 12.073z" />
        </svg>
      </button>

      {/* Zalo */}
      <button
        onClick={() =>
          openPopup(
            `https://zalo.me/share?url=${encodedUrl}&title=${encodedTitle}`
          )
        }
        title="Chia sẻ qua Zalo"
        className="flex items-center justify-center w-8 h-8 rounded-full bg-[#0068FF] hover:opacity-80 transition"
      >
        <span className="text-white text-xs font-extrabold leading-none">Z</span>
      </button>

      {/* X (Twitter) */}
      <button
        onClick={() =>
          openPopup(
            `https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedTitle}`
          )
        }
        title="Chia sẻ lên X"
        className="flex items-center justify-center w-8 h-8 rounded-full bg-black border border-zinc-600 hover:opacity-80 transition"
      >
        <svg className="w-3.5 h-3.5 text-white fill-current" viewBox="0 0 24 24">
          <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-4.714-6.231-5.401 6.231H2.748l7.73-8.835L1.254 2.25H8.08l4.259 5.631L18.244 2.25zm-1.161 17.52h1.833L7.084 4.126H5.117L17.083 19.77z" />
        </svg>
      </button>

      {/* Copy link */}
      <button
        onClick={handleCopy}
        title="Sao chép liên kết"
        className={`flex items-center gap-1.5 px-3 h-8 rounded-full text-xs font-medium transition ${
          copied
            ? "bg-green-500/20 text-green-400 border border-green-500/40"
            : "bg-zinc-700 text-gray-300 hover:bg-zinc-600 border border-transparent"
        }`}
      >
        {copied ? (
          <>
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
            Đã sao chép
          </>
        ) : (
          <>
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
            </svg>
            Sao chép link
          </>
        )}
      </button>
    </div>
  );
}
