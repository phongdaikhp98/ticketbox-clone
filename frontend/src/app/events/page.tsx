"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import EventCard from "@/components/EventCard";
import EventFilter from "@/components/EventFilter";
import Pagination from "@/components/Pagination";
import { eventService } from "@/lib/event-service";
import { Event, EventFilterParams, PageResponse } from "@/types/event";

export default function EventsPage() {
  const [events, setEvents] = useState<PageResponse<Event> | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<EventFilterParams>({});

  const fetchEvents = async (params: EventFilterParams = {}) => {
    setLoading(true);
    try {
      const data = await eventService.getEvents(params);
      setEvents(data);
    } catch {
      setEvents(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents(filter);
  }, []);

  const handleFilter = (params: EventFilterParams) => {
    setFilter(params);
    fetchEvents(params);
  };

  const handlePageChange = (page: number) => {
    const newFilter = { ...filter, page };
    setFilter(newFilter);
    fetchEvents(newFilter);
  };

  return (
    <div className="min-h-screen bg-secondary">
      <Header />
      <main className="max-w-7xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-white mb-6">Events</h1>

        <EventFilter onFilter={handleFilter} />

        {loading ? (
          <div className="text-center text-gray-400 py-12">Loading events...</div>
        ) : !events || events.content.length === 0 ? (
          <div className="text-center text-gray-400 py-12">No events found</div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-6">
              {events.content.map((event) => (
                <EventCard key={event.id} event={event} />
              ))}
            </div>

            <Pagination
              currentPage={events.number}
              totalPages={events.totalPages}
              onPageChange={handlePageChange}
            />
          </>
        )}
      </main>
    </div>
  );
}
