"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import { eventService } from "@/lib/event-service";
import { Event } from "@/types/event";

export default function EventDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id) return;
    eventService
      .getEventById(id)
      .then(setEvent)
      .catch(() => setError("Event not found"))
      .finally(() => setLoading(false));
  }, [id]);

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      weekday: "long",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  return (
    <div className="min-h-screen bg-secondary">
      <Header />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <Link href="/events" className="text-gray-400 hover:text-white text-sm transition mb-4 inline-block">
          &larr; Back to Events
        </Link>

        {loading ? (
          <div className="text-center text-gray-400 py-12">Loading...</div>
        ) : error ? (
          <div className="text-center text-red-400 py-12">{error}</div>
        ) : event ? (
          <div className="space-y-6">
            {event.imageUrl && (
              <div className="rounded-lg overflow-hidden h-80">
                <img
                  src={event.imageUrl}
                  alt={event.title}
                  className="w-full h-full object-cover"
                />
              </div>
            )}

            <div className="bg-zinc-800 rounded-lg p-6 space-y-4">
              <div className="flex items-center gap-3 flex-wrap">
                <span className="px-2 py-1 bg-purple-500/20 text-purple-400 text-xs rounded">
                  {event.category}
                </span>
                {event.isFeatured && (
                  <span className="px-2 py-1 bg-primary/20 text-primary text-xs rounded">
                    Featured
                  </span>
                )}
              </div>

              <h1 className="text-3xl font-bold text-white">{event.title}</h1>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-500">Date</span>
                  <p className="text-white">{formatDate(event.eventDate)}</p>
                  {event.endDate && (
                    <p className="text-gray-400">to {formatDate(event.endDate)}</p>
                  )}
                </div>
                <div>
                  <span className="text-gray-500">Location</span>
                  <p className="text-white">{event.location}</p>
                </div>
                <div>
                  <span className="text-gray-500">Organizer</span>
                  <p className="text-white">{event.organizer.fullName}</p>
                </div>
              </div>

              {event.description && (
                <div>
                  <h3 className="text-gray-400 text-sm mb-2">Description</h3>
                  <p className="text-gray-300 whitespace-pre-line">{event.description}</p>
                </div>
              )}
            </div>

            <div className="bg-zinc-800 rounded-lg p-6">
              <h3 className="text-white font-semibold mb-4">Tickets</h3>
              <div className="space-y-3">
                {event.ticketTypes.map((tt) => (
                  <div
                    key={tt.id}
                    className="flex items-center justify-between p-4 bg-zinc-700 rounded-lg"
                  >
                    <div>
                      <p className="text-white font-medium">{tt.name}</p>
                      <p className="text-gray-400 text-sm">
                        {tt.availableCount} / {tt.capacity} available
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-primary font-semibold">
                        {tt.price > 0 ? formatPrice(tt.price) : "Free"}
                      </p>
                      <button
                        className="mt-1 px-4 py-1.5 bg-primary text-white text-sm rounded hover:bg-green-600 transition disabled:opacity-50"
                        disabled={tt.availableCount === 0}
                      >
                        {tt.availableCount > 0 ? "Buy" : "Sold Out"}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
}
