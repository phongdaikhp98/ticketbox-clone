"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { eventService } from "@/lib/event-service";
import { CreateEventRequest, TicketTypeRequest, EVENT_CATEGORIES } from "@/types/event";

export default function CreateEventPage() {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <CreateEventForm />
      </div>
    </ProtectedRoute>
  );
}

function CreateEventForm() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [form, setForm] = useState({
    title: "",
    description: "",
    eventDate: "",
    endDate: "",
    location: "",
    imageUrl: "",
    category: "MUSIC",
    isFeatured: false,
  });

  const [ticketTypes, setTicketTypes] = useState<TicketTypeRequest[]>([
    { name: "Standard", price: 0, capacity: 100 },
  ]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    setForm({
      ...form,
      [name]: type === "checkbox" ? (e.target as HTMLInputElement).checked : value,
    });
    setError("");
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
    setLoading(true);
    setError("");

    try {
      const request: CreateEventRequest = {
        title: form.title,
        description: form.description || undefined,
        eventDate: form.eventDate,
        endDate: form.endDate || undefined,
        location: form.location,
        imageUrl: form.imageUrl || undefined,
        category: form.category,
        isFeatured: form.isFeatured,
        ticketTypes: ticketTypes.map((tt) => ({
          name: tt.name,
          price: Number(tt.price),
          capacity: Number(tt.capacity),
        })),
      };

      const created = await eventService.createEvent(request);
      router.push(`/events/${created.id}`);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string; data?: Record<string, string> } } };
      if (error.response?.data?.data) {
        setError(Object.values(error.response.data.data).join(". "));
      } else {
        setError(error.response?.data?.message || "Failed to create event");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="max-w-3xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-white mb-6">Create Event</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        {error && (
          <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
            {error}
          </div>
        )}

        <div className="bg-zinc-800 rounded-lg p-6 space-y-4">
          <h2 className="text-white font-semibold">Event Details</h2>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Title *</label>
            <input
              type="text"
              name="title"
              value={form.title}
              onChange={handleChange}
              required
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
              <label className="block text-gray-400 text-sm mb-1">Event Date *</label>
              <input
                type="datetime-local"
                name="eventDate"
                value={form.eventDate}
                onChange={handleChange}
                required
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
            <label className="block text-gray-400 text-sm mb-1">Location *</label>
            <input
              type="text"
              name="location"
              value={form.location}
              onChange={handleChange}
              required
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-gray-400 text-sm mb-1">Category *</label>
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
                placeholder="https://example.com/image.jpg"
                className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
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
                  required
                  placeholder="VIP, Standard..."
                  className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
                />
              </div>
              <div className="w-32">
                <label className="block text-gray-500 text-xs mb-1">Price (VND)</label>
                <input
                  type="number"
                  value={tt.price}
                  onChange={(e) => handleTicketChange(index, "price", e.target.value)}
                  required
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
                  required
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

        <button
          type="submit"
          disabled={loading}
          className="w-full py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition disabled:opacity-50 font-medium"
        >
          {loading ? "Creating..." : "Create Event"}
        </button>
      </form>
    </main>
  );
}
