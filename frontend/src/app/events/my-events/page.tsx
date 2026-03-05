"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import EventCard from "@/components/EventCard";
import Pagination from "@/components/Pagination";
import { eventService } from "@/lib/event-service";
import { Event, PageResponse } from "@/types/event";

export default function MyEventsPage() {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <MyEventsList />
      </div>
    </ProtectedRoute>
  );
}

function MyEventsList() {
  const router = useRouter();
  const [events, setEvents] = useState<PageResponse<Event> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  const fetchEvents = async (p: number) => {
    setLoading(true);
    try {
      const data = await eventService.getMyEvents(p, 9);
      setEvents(data);
    } catch {
      setEvents(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents(page);
  }, [page]);

  const handleEdit = (id: number) => {
    router.push(`/events/${id}/edit`);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Are you sure you want to delete this event?")) return;
    try {
      await eventService.deleteEvent(id);
      fetchEvents(page);
    } catch {
      alert("Failed to delete event");
    }
  };

  return (
    <main className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">My Events</h1>
        <Link
          href="/events/create"
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition text-sm"
        >
          Create Event
        </Link>
      </div>

      {loading ? (
        <div className="text-center text-gray-400 py-12">Loading...</div>
      ) : !events || events.content.length === 0 ? (
        <div className="text-center text-gray-400 py-12">
          <p>No events yet</p>
          <Link
            href="/events/create"
            className="text-primary hover:underline mt-2 inline-block"
          >
            Create your first event
          </Link>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {events.content.map((event) => (
              <EventCard
                key={event.id}
                event={event}
                showStatus
                onEdit={handleEdit}
                onDelete={handleDelete}
              />
            ))}
          </div>

          <Pagination
            currentPage={events.number}
            totalPages={events.totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </main>
  );
}
