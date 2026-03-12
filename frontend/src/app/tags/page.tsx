"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { tagService } from "@/lib/tag-service";
import { TagInfo } from "@/types/event";

export default function TagsPage() {
  const [tags, setTags] = useState<TagInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    tagService.getAllTags()
      .then(setTags)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const maxUsage = tags.length > 0 ? Math.max(...tags.map((t) => t.usageCount)) : 1;

  const getFontSize = (usageCount: number) => {
    const ratio = maxUsage > 0 ? usageCount / maxUsage : 0;
    if (ratio > 0.7) return "text-2xl font-bold";
    if (ratio > 0.4) return "text-lg font-semibold";
    if (ratio > 0.2) return "text-base font-medium";
    return "text-sm";
  };

  const getOpacity = (usageCount: number) => {
    const ratio = maxUsage > 0 ? usageCount / maxUsage : 0;
    if (ratio > 0.5) return "text-white";
    if (ratio > 0.2) return "text-zinc-300";
    return "text-zinc-400";
  };

  return (
    <div className="min-h-screen bg-secondary flex flex-col">
      <Header />
      <main className="flex-1 max-w-4xl mx-auto px-4 py-8 w-full">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">Khám phá Tags</h1>
          <p className="text-gray-400">
            {loading ? "Đang tải..." : `${tags.length} tags · Click để xem sự kiện theo tag`}
          </p>
        </div>

        {loading ? (
          <div className="text-center text-gray-400 py-12">Đang tải...</div>
        ) : tags.length === 0 ? (
          <div className="text-center text-gray-500 py-12">Chưa có tags nào.</div>
        ) : (
          <div className="bg-zinc-800 rounded-xl p-8">
            <div className="flex flex-wrap gap-4 justify-center">
              {tags.map((tag) => (
                <Link
                  key={tag.id}
                  href={`/events?tag=${encodeURIComponent(tag.name)}`}
                  className={`group inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-zinc-700 hover:bg-primary/20 hover:text-primary transition-all duration-150 ${getFontSize(tag.usageCount)} ${getOpacity(tag.usageCount)}`}
                >
                  <span>#{tag.name}</span>
                  {tag.usageCount > 0 && (
                    <span className="text-xs text-zinc-500 group-hover:text-primary/70">
                      {tag.usageCount}
                    </span>
                  )}
                </Link>
              ))}
            </div>
          </div>
        )}

        {!loading && tags.length > 0 && (
          <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-zinc-800 rounded-lg p-4 text-center">
              <p className="text-3xl font-bold text-primary">{tags.length}</p>
              <p className="text-gray-400 text-sm mt-1">Tổng tags</p>
            </div>
            <div className="bg-zinc-800 rounded-lg p-4 text-center">
              <p className="text-3xl font-bold text-primary">
                {tags.reduce((sum, t) => sum + t.usageCount, 0)}
              </p>
              <p className="text-gray-400 text-sm mt-1">Lượt sử dụng</p>
            </div>
            <div className="bg-zinc-800 rounded-lg p-4 text-center">
              <p className="text-xl font-bold text-primary truncate">
                #{tags[0]?.name}
              </p>
              <p className="text-gray-400 text-sm mt-1">Tag phổ biến nhất</p>
            </div>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
