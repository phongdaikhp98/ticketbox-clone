"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import FeaturedEventsCarousel from "@/components/FeaturedEventsCarousel";
import EventCard from "@/components/EventCard";
import { useAuth } from "@/contexts/AuthContext";
import { eventService } from "@/lib/event-service";
import { categoryService } from "@/lib/category-service";
import { Event, CategoryInfo, PageResponse } from "@/types/event";

export default function Home() {
  return (
    <div className="min-h-screen bg-secondary flex flex-col">
      <Header />
      <main className="flex-1">
        <HeroSection />
        <div className="max-w-7xl mx-auto px-4 py-10 space-y-14">
          <FeaturedEventsCarousel />
          <UpcomingEventsSection />
          <OrganizerCTA />
        </div>
      </main>
      <Footer />
    </div>
  );
}

/* ─── Hero ─────────────────────────────────────────────────────────────── */

function HeroSection() {
  const { user } = useAuth();

  return (
    <section className="relative overflow-hidden bg-zinc-900">
      {/* background gradient blobs */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute -top-32 -left-32 w-[500px] h-[500px] bg-primary/20 rounded-full blur-3xl" />
        <div className="absolute -bottom-32 -right-32 w-[500px] h-[500px] bg-indigo-500/15 rounded-full blur-3xl" />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 py-20 md:py-28 flex flex-col items-center text-center gap-6">
        <span className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary/10 border border-primary/30 text-primary text-sm font-medium">
          🎉 Nền tảng mua vé sự kiện hàng đầu
        </span>

        <h1 className="text-4xl sm:text-5xl md:text-6xl font-extrabold text-white leading-tight max-w-3xl">
          Khám phá sự kiện{" "}
          <span className="text-primary">tuyệt vời</span>{" "}
          gần bạn
        </h1>

        <p className="text-gray-400 text-lg max-w-xl">
          Hàng trăm sự kiện âm nhạc, thể thao, hội thảo và nhiều hơn nữa — đặt vé chỉ trong vài giây.
        </p>

        <div className="flex flex-col sm:flex-row gap-3 mt-2">
          <Link
            href="/events"
            className="px-8 py-3 bg-primary hover:bg-green-600 text-white font-semibold rounded-xl transition text-base"
          >
            Khám phá ngay
          </Link>
          {!user && (
            <Link
              href="/register"
              className="px-8 py-3 bg-zinc-700 hover:bg-zinc-600 text-white font-semibold rounded-xl transition text-base"
            >
              Đăng ký miễn phí
            </Link>
          )}
        </div>

        {/* Stats row */}
        <div className="flex flex-wrap justify-center gap-8 mt-8 pt-8 border-t border-zinc-700/50 w-full max-w-lg">
          {[
            { label: "Sự kiện", value: "500+" },
            { label: "Người dùng", value: "10K+" },
            { label: "Vé đã bán", value: "50K+" },
          ].map((s) => (
            <div key={s.label} className="text-center">
              <p className="text-2xl font-bold text-white">{s.value}</p>
              <p className="text-gray-500 text-sm">{s.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ─── Upcoming Events + Category filter ───────────────────────────────── */

function UpcomingEventsSection() {
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [selectedCat, setSelectedCat] = useState<number | null>(null);
  const [data, setData] = useState<PageResponse<Event> | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    categoryService.getCategories().then(setCategories).catch(() => {});
  }, []);

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const result = await eventService.getEvents({
        page: 0,
        size: 8,
        ...(selectedCat ? { categoryId: selectedCat } : {}),
      });
      setData(result);
    } catch {
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [selectedCat]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  return (
    <section>
      {/* Section title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
        <h2 className="text-xl font-bold text-white">Sự kiện sắp diễn ra</h2>
        <Link href="/events" className="text-primary hover:text-green-400 text-sm transition">
          Xem tất cả →
        </Link>
      </div>

      {/* Category filter */}
      {categories.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-2 mb-6 scrollbar-hide">
          <button
            onClick={() => setSelectedCat(null)}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition shrink-0 ${
              selectedCat === null
                ? "bg-primary text-white"
                : "bg-zinc-800 text-gray-300 hover:bg-zinc-700 border border-zinc-700"
            }`}
          >
            Tất cả
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setSelectedCat(cat.id)}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition shrink-0 ${
                selectedCat === cat.id
                  ? "bg-primary text-white"
                  : "bg-zinc-800 text-gray-300 hover:bg-zinc-700 border border-zinc-700"
              }`}
            >
              {cat.icon && <span>{cat.icon}</span>}
              {cat.name}
            </button>
          ))}
        </div>
      )}

      {/* Events grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="h-72 bg-zinc-800 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="text-center text-gray-500 py-16">
          Không có sự kiện nào trong danh mục này
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {data.content.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </section>
  );
}

/* ─── Organizer CTA ────────────────────────────────────────────────────── */

function OrganizerCTA() {
  const { user } = useAuth();

  if (user?.role === "ORGANIZER" || user?.role === "ADMIN") return null;

  return (
    <section className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-primary/20 via-zinc-800 to-indigo-500/20 border border-zinc-700 p-8 md:p-12 text-center">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-0 left-1/4 w-64 h-64 bg-primary/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 right-1/4 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl" />
      </div>
      <div className="relative space-y-4">
        <p className="text-4xl">🎭</p>
        <h2 className="text-2xl md:text-3xl font-bold text-white">
          Bạn muốn tổ chức sự kiện?
        </h2>
        <p className="text-gray-400 max-w-lg mx-auto">
          Trở thành Organizer trên Ticketbox — tạo sự kiện, bán vé và quản lý người tham dự dễ dàng.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
          {user ? (
            <Link
              href="/organizer-application"
              className="px-8 py-3 bg-primary hover:bg-green-600 text-white font-semibold rounded-xl transition"
            >
              Đăng ký làm Organizer
            </Link>
          ) : (
            <>
              <Link
                href="/register"
                className="px-8 py-3 bg-primary hover:bg-green-600 text-white font-semibold rounded-xl transition"
              >
                Bắt đầu ngay
              </Link>
              <Link
                href="/login"
                className="px-8 py-3 bg-zinc-700 hover:bg-zinc-600 text-white font-semibold rounded-xl transition"
              >
                Đăng nhập
              </Link>
            </>
          )}
        </div>
      </div>
    </section>
  );
}
