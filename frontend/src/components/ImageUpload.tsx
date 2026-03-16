"use client";

import { useRef, useState } from "react";
import api from "@/lib/api";

interface ImageUploadProps {
  currentUrl?: string;
  onUpload: (url: string) => void;
  folder?: string;
  aspectRatio?: "video" | "square";
  label?: string;
}

export default function ImageUpload({
  currentUrl,
  onUpload,
  folder = "general",
  aspectRatio = "video",
  label = "Ảnh",
}: ImageUploadProps) {
  const [preview, setPreview] = useState<string | undefined>(currentUrl);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const containerClass =
    aspectRatio === "square"
      ? "w-32 h-32 rounded-full"
      : "w-full h-48 rounded-lg";

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Client-side validation
    if (file.size > 5 * 1024 * 1024) {
      setError("File không được vượt quá 5MB");
      return;
    }
    const allowedTypes = ["image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"];
    if (!allowedTypes.includes(file.type)) {
      setError("Chỉ chấp nhận file ảnh (JPEG, PNG, WebP, GIF)");
      return;
    }

    setError(null);
    setUploading(true);

    // Show local preview immediately
    const localUrl = URL.createObjectURL(file);
    setPreview(localUrl);

    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("folder", folder);

      const res = await api.post<{ data: { url: string } }>("/v1/upload/image", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      const uploadedUrl = res.data.data.url;
      setPreview(uploadedUrl);
      onUpload(uploadedUrl);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Upload thất bại, vui lòng thử lại";
      setError(message);
      setPreview(currentUrl); // revert to original
    } finally {
      setUploading(false);
      // Reset input so same file can be re-selected
      if (inputRef.current) inputRef.current.value = "";
    }
  };

  return (
    <div className="flex flex-col gap-2">
      {label && (
        <label className="text-sm font-medium text-zinc-300">{label}</label>
      )}

      <div
        className={`relative ${containerClass} bg-zinc-700 border-2 border-dashed border-zinc-500 overflow-hidden cursor-pointer hover:border-green-500 transition-colors`}
        onClick={() => !uploading && inputRef.current?.click()}
      >
        {/* Image preview */}
        {preview ? (
          <img
            src={preview}
            alt="Preview"
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex flex-col items-center justify-center text-zinc-400 gap-2">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="w-10 h-10"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
              />
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
            <span className="text-sm">Chọn ảnh</span>
          </div>
        )}

        {/* Loading overlay */}
        {uploading && (
          <div className="absolute inset-0 bg-black/60 flex flex-col items-center justify-center gap-2">
            <div className="w-8 h-8 border-2 border-green-500 border-t-transparent rounded-full animate-spin" />
            <span className="text-white text-sm">Đang tải lên...</span>
          </div>
        )}

        {/* Change button overlay (shown on hover when image exists) */}
        {preview && !uploading && (
          <div className="absolute inset-0 bg-black/0 hover:bg-black/40 flex items-center justify-center opacity-0 hover:opacity-100 transition-all">
            <span className="text-white text-sm font-medium bg-black/50 px-3 py-1 rounded-full">
              Đổi ảnh
            </span>
          </div>
        )}
      </div>

      {/* Error message */}
      {error && <p className="text-red-400 text-sm">{error}</p>}

      {/* Hidden file input */}
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleFileChange}
      />
    </div>
  );
}
