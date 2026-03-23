"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { Event } from "@/types/event";
import { eventService } from "@/lib/event-service";

export default function FeaturedEventsCarousel() {
  const [events, setEvents] = useState<Event[]>([]);
  const [current, setCurrent] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    eventService
      .getFeaturedEvents()
      .then(setEvents)
      .catch(() => setEvents([]))
      .finally(() => setLoading(false));
  }, []);

  const prev = useCallback(() =>
    setCurrent((c) => (c - 1 + events.length) % events.length), [events.length]);

  const next = useCallback(() =>
    setCurrent((c) => (c + 1) % events.length), [events.length]);

  // Auto-advance every 5 seconds
  useEffect(() => {
    if (events.length <= 1) return;
    const timer = setInterval(next, 5000);
    return () => clearInterval(timer);
  }, [events.length, next]);

  if (loading) {
    return (
      <div className="w-full h-80 bg-zinc-800 rounded-xl animate-pulse" />
    );
  }

  if (events.length === 0) return null;

  const event = events[current];

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  const formatPrice = (price: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

  const minPrice =
    event.ticketTypes.length > 0
      ? Math.min(...event.ticketTypes.map((t) => t.price))
      : 0;

  return (
    <section className="mb-10">
      <div className="flex items-center gap-2 mb-4">
        <span className="text-yellow-400 text-lg">★</span>
        <h2 className="text-lg font-bold text-white">Sự kiện nổi bật</h2>
      </div>

      <div className="relative rounded-xl overflow-hidden bg-zinc-800 h-80 md:h-96 group">
        {/* Background image */}
        {event.imageUrl ? (
          <img
            src={event.imageUrl}
            alt={event.title}
            className="absolute inset-0 w-full h-full object-cover transition-opacity duration-500"
          />
        ) : (
          <div className="absolute inset-0 bg-gradient-to-br from-zinc-700 to-zinc-900" />
        )}

        {/* Gradient overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/40 to-transparent" />

        {/* Content */}
        <div className="absolute bottom-0 left-0 right-0 p-6 md:p-8">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              {event.category && (
                <span className="inline-block px-2 py-0.5 bg-primary/80 text-white text-xs rounded mb-2">
                  {event.category.icon && <span className="mr-1">{event.category.icon}</span>}
                  {event.category.name}
                </span>
              )}
              <h3 className="text-white text-xl md:text-2xl font-bold line-clamp-2 mb-1">
                {event.title}
              </h3>
              <p className="text-gray-300 text-sm mb-1">
                {formatDate(event.eventDate)}
              </p>
              <p className="text-gray-400 text-sm truncate mb-3">
                {event.location}
              </p>
              <div className="flex items-center gap-3">
                <span className="text-primary font-semibold">
                  {minPrice > 0 ? `Từ ${formatPrice(minPrice)}` : "Miễn phí"}
                </span>
                <Link
                  href={`/events/${event.id}`}
                  className="px-4 py-1.5 bg-primary hover:bg-primary/90 text-white text-sm rounded-lg transition font-medium"
                >
                  Xem ngay
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* Navigation arrows */}
        {events.length > 1 && (
          <>
            <button
              onClick={prev}
              className="absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 bg-black/50 hover:bg-black/70 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
              aria-label="Previous"
            >
              ‹
            </button>
            <button
              onClick={next}
              className="absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 bg-black/50 hover:bg-black/70 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
              aria-label="Next"
            >
              ›
            </button>
          </>
        )}

        {/* Dot indicators */}
        {events.length > 1 && (
          <div className="absolute bottom-3 right-6 flex gap-1.5">
            {events.map((_, i) => (
              <button
                key={i}
                onClick={() => setCurrent(i)}
                className={`w-2 h-2 rounded-full transition-all ${
                  i === current ? "bg-white w-4" : "bg-white/40 hover:bg-white/70"
                }`}
                aria-label={`Slide ${i + 1}`}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
