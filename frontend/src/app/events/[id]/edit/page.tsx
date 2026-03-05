"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { eventService } from "@/lib/event-service";
import { Event, UpdateEventRequest, TicketTypeRequest, EVENT_CATEGORIES } from "@/types/event";

export default function EditEventPage() {
  return (
    <ProtectedRoute roles={["ORGANIZER", "ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <Header />
        <EditEventForm />
      </div>
    </ProtectedRoute>
  );
}

function EditEventForm() {
  const params = useParams();
  const router = useRouter();
  const id = Number(params.id);

  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [form, setForm] = useState({
    title: "",
    description: "",
    eventDate: "",
    endDate: "",
    location: "",
    imageUrl: "",
    category: "MUSIC",
    status: "DRAFT",
    isFeatured: false,
  });

  const [ticketTypes, setTicketTypes] = useState<TicketTypeRequest[]>([]);

  useEffect(() => {
    if (!id) return;
    eventService
      .getEventById(id)
      .then((data) => {
        setEvent(data);
        setForm({
          title: data.title,
          description: data.description || "",
          eventDate: data.eventDate?.slice(0, 16) || "",
          endDate: data.endDate?.slice(0, 16) || "",
          location: data.location,
          imageUrl: data.imageUrl || "",
          category: data.category,
          status: data.status,
          isFeatured: data.isFeatured,
        });
        setTicketTypes(
          data.ticketTypes.map((tt) => ({
            name: tt.name,
            price: tt.price,
            capacity: tt.capacity,
          }))
        );
      })
      .catch(() => setError("Event not found"))
      .finally(() => setLoading(false));
  }, [id]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    setForm({
      ...form,
      [name]: type === "checkbox" ? (e.target as HTMLInputElement).checked : value,
    });
    setError("");
    setSuccess("");
  };

  const handleTicketChange = (index: number, field: string, value: string | number) => {
    const updated = [...ticketTypes];
    updated[index] = { ...updated[index], [field]: value };
    setTicketTypes(updated);
  };

  const addTicketType = () => {
    setTicketTypes([...ticketTypes, { name: "", price: 0, capacity: 100 }]);
  };

  const removeTicketType = (index: number) => {
    if (ticketTypes.length <= 1) return;
    setTicketTypes(ticketTypes.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");

    try {
      const request: UpdateEventRequest = {
        title: form.title,
        description: form.description || undefined,
        eventDate: form.eventDate || undefined,
        endDate: form.endDate || undefined,
        location: form.location,
        imageUrl: form.imageUrl || undefined,
        category: form.category,
        status: form.status,
        isFeatured: form.isFeatured,
        ticketTypes: ticketTypes.map((tt) => ({
          name: tt.name,
          price: Number(tt.price),
          capacity: Number(tt.capacity),
        })),
      };

      await eventService.updateEvent(id, request);
      setSuccess("Event updated successfully!");
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string; data?: Record<string, string> } } };
      if (error.response?.data?.data) {
        setError(Object.values(error.response.data.data).join(". "));
      } else {
        setError(error.response?.data?.message || "Failed to update event");
      }
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm("Are you sure you want to delete this event?")) return;
    try {
      await eventService.deleteEvent(id);
      router.push("/events/my-events");
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || "Failed to delete event");
    }
  };

  if (loading) {
    return (
      <main className="max-w-3xl mx-auto px-4 py-8">
        <div className="text-center text-gray-400">Loading...</div>
      </main>
    );
  }

  if (!event) {
    return (
      <main className="max-w-3xl mx-auto px-4 py-8">
        <div className="text-center text-red-400">{error || "Event not found"}</div>
      </main>
    );
  }

  const statusOptions = () => {
    if (event.status === "DRAFT") return ["DRAFT", "PUBLISHED"];
    if (event.status === "PUBLISHED") return ["PUBLISHED", "CANCELLED"];
    return [event.status];
  };

  return (
    <main className="max-w-3xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">Edit Event</h1>
        <button
          onClick={() => router.push("/events/my-events")}
          className="text-gray-400 hover:text-white text-sm transition"
        >
          Back to My Events
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {error && (
          <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
            {error}
          </div>
        )}
        {success && (
          <div className="p-3 bg-green-500/10 border border-green-500/20 rounded-lg text-green-400 text-sm">
            {success}
          </div>
        )}

        <div className="bg-zinc-800 rounded-lg p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-white font-semibold">Event Details</h2>
            <select
              name="status"
              value={form.status}
              onChange={handleChange}
              className="px-3 py-1.5 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
            >
              {statusOptions().map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Title</label>
            <input
              type="text"
              name="title"
              value={form.title}
              onChange={handleChange}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Description</label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              rows={4}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary resize-none"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-gray-400 text-sm mb-1">Event Date</label>
              <input
                type="datetime-local"
                name="eventDate"
                value={form.eventDate}
                onChange={handleChange}
                className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-gray-400 text-sm mb-1">End Date</label>
              <input
                type="datetime-local"
                name="endDate"
                value={form.endDate}
                onChange={handleChange}
                className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Location</label>
            <input
              type="text"
              name="location"
              value={form.location}
              onChange={handleChange}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-gray-400 text-sm mb-1">Category</label>
              <select
                name="category"
                value={form.category}
                onChange={handleChange}
                className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
              >
                {EVENT_CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-gray-400 text-sm mb-1">Image URL</label>
              <input
                type="text"
                name="imageUrl"
                value={form.imageUrl}
                onChange={handleChange}
                className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              name="isFeatured"
              checked={form.isFeatured}
              onChange={handleChange}
              className="rounded"
            />
            <label className="text-gray-400 text-sm">Featured Event</label>
          </div>
        </div>

        <div className="bg-zinc-800 rounded-lg p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-white font-semibold">Ticket Types</h2>
            <button
              type="button"
              onClick={addTicketType}
              className="px-3 py-1 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 transition text-sm"
            >
              + Add Type
            </button>
          </div>

          {ticketTypes.map((tt, index) => (
            <div key={index} className="flex gap-3 items-end">
              <div className="flex-1">
                <label className="block text-gray-500 text-xs mb-1">Name</label>
                <input
                  type="text"
                  value={tt.name}
                  onChange={(e) => handleTicketChange(index, "name", e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
                />
              </div>
              <div className="w-32">
                <label className="block text-gray-500 text-xs mb-1">Price (VND)</label>
                <input
                  type="number"
                  value={tt.price}
                  onChange={(e) => handleTicketChange(index, "price", e.target.value)}
                  min="0"
                  className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
                />
              </div>
              <div className="w-24">
                <label className="block text-gray-500 text-xs mb-1">Capacity</label>
                <input
                  type="number"
                  value={tt.capacity}
                  onChange={(e) => handleTicketChange(index, "capacity", e.target.value)}
                  min="1"
                  className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
                />
              </div>
              {ticketTypes.length > 1 && (
                <button
                  type="button"
                  onClick={() => removeTicketType(index)}
                  className="px-3 py-2 text-red-400 hover:text-red-300 text-sm"
                >
                  Remove
                </button>
              )}
            </div>
          ))}
        </div>

        <div className="flex gap-4">
          <button
            type="submit"
            disabled={saving}
            className="flex-1 py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition disabled:opacity-50 font-medium"
          >
            {saving ? "Saving..." : "Save Changes"}
          </button>

          {event.status === "DRAFT" && (
            <button
              type="button"
              onClick={handleDelete}
              className="py-3 px-6 bg-red-500/10 text-red-400 rounded-lg hover:bg-red-500/20 transition font-medium"
            >
              Delete
            </button>
          )}
        </div>
      </form>
    </main>
  );
}
