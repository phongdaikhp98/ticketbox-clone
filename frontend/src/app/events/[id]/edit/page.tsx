"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import ImageUpload from "@/components/ImageUpload";
import { eventService } from "@/lib/event-service";
import { categoryService } from "@/lib/category-service";
import { Event, UpdateEventRequest, TicketTypeRequest, CategoryInfo } from "@/types/event";

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
  const [categories, setCategories] = useState<CategoryInfo[]>([]);

  const [form, setForm] = useState({
    title: "",
    description: "",
    eventDate: "",
    endDate: "",
    location: "",
    imageUrl: "",
    categoryId: "",
    status: "DRAFT",
    isFeatured: false,
  });

  const [ticketTypes, setTicketTypes] = useState<TicketTypeRequest[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [tags, setTags] = useState<string[]>([]);

  useEffect(() => {
    categoryService.getCategories().then(setCategories).catch(() => {});
  }, []);

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
          categoryId: data.category ? String(data.category.id) : "",
          status: data.status,
          isFeatured: data.isFeatured,
        });
        setTags(data.tags?.map((t) => t.name) ?? []);
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

  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      const normalized = tagInput.trim().toLowerCase();
      if (normalized && !tags.includes(normalized) && tags.length < 10) {
        setTags([...tags, normalized]);
      }
      setTagInput("");
    }
  };

  const removeTag = (tag: string) => {
    setTags(tags.filter((t) => t !== tag));
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
        categoryId: form.categoryId ? Number(form.categoryId) : undefined,
        tags: tags,
        status: form.status,
        isFeatured: form.isFeatured,
        ticketTypes: ticketTypes.map((tt) => ({
          name: tt.name,
          price: Number(tt.price),
          capacity: Number(tt.capacity),
        })),
      };

      await eventService.updateEvent(id, request);
      setSuccess("Cập nhật sự kiện thành công!");
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
    if (!confirm("Bạn có chắc muốn xóa sự kiện này?")) return;
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
        <div className="text-center text-gray-400">Đang tải...</div>
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
        <h1 className="text-2xl font-bold text-white">Chỉnh sửa sự kiện</h1>
        <button
          onClick={() => router.push("/events/my-events")}
          className="text-gray-400 hover:text-white text-sm transition"
        >
          ← Sự kiện của tôi
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
            <h2 className="text-white font-semibold">Thông tin sự kiện</h2>
            <select
              name="status"
              value={form.status}
              onChange={handleChange}
              className="px-3 py-1.5 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
            >
              {statusOptions().map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Tiêu đề</label>
            <input
              type="text"
              name="title"
              value={form.title}
              onChange={handleChange}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Mô tả</label>
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
              <label className="block text-gray-400 text-sm mb-1">Ngày bắt đầu</label>
              <input
                type="datetime-local"
                name="eventDate"
                value={form.eventDate}
                onChange={handleChange}
                className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
              />
            </div>
            <div>
              <label className="block text-gray-400 text-sm mb-1">Ngày kết thúc</label>
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
            <label className="block text-gray-400 text-sm mb-1">Địa điểm</label>
            <input
              type="text"
              name="location"
              value={form.location}
              onChange={handleChange}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Thể loại</label>
            <select
              name="categoryId"
              value={form.categoryId}
              onChange={handleChange}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            >
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.icon} {cat.name}
                </option>
              ))}
            </select>
          </div>

          <ImageUpload
            label="Ảnh banner sự kiện"
            folder="events"
            aspectRatio="video"
            currentUrl={form.imageUrl || undefined}
            onUpload={(url) => setForm((prev) => ({ ...prev, imageUrl: url }))}
          />

          <div>
            <label className="block text-gray-400 text-sm mb-1">Tags (Enter hoặc dấu phẩy để thêm)</label>
            <div className="flex flex-wrap gap-2 p-2 bg-zinc-700 border border-zinc-600 rounded-lg min-h-[2.5rem]">
              {tags.map((tag) => (
                <span
                  key={tag}
                  className="flex items-center gap-1 px-2 py-0.5 bg-zinc-600 text-zinc-200 text-sm rounded"
                >
                  #{tag}
                  <button
                    type="button"
                    onClick={() => removeTag(tag)}
                    className="text-zinc-400 hover:text-white ml-1"
                  >
                    ×
                  </button>
                </span>
              ))}
              <input
                type="text"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={handleTagKeyDown}
                placeholder={tags.length === 0 ? "Nhập tag..." : ""}
                className="flex-1 min-w-[120px] bg-transparent text-white text-sm outline-none placeholder-gray-500"
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
            <label className="text-gray-400 text-sm">Sự kiện nổi bật</label>
          </div>
        </div>

        <div className="bg-zinc-800 rounded-lg p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-white font-semibold">Loại vé</h2>
            <button
              type="button"
              onClick={addTicketType}
              className="px-3 py-1 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 transition text-sm"
            >
              + Thêm loại vé
            </button>
          </div>

          {ticketTypes.map((tt, index) => (
            <div key={index} className="flex gap-3 items-end">
              <div className="flex-1">
                <label className="block text-gray-500 text-xs mb-1">Tên</label>
                <input
                  type="text"
                  value={tt.name}
                  onChange={(e) => handleTicketChange(index, "name", e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
                />
              </div>
              <div className="w-32">
                <label className="block text-gray-500 text-xs mb-1">Giá (VND)</label>
                <input
                  type="number"
                  value={tt.price}
                  onChange={(e) => handleTicketChange(index, "price", e.target.value)}
                  min="0"
                  className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white text-sm focus:outline-none focus:border-primary"
                />
              </div>
              <div className="w-24">
                <label className="block text-gray-500 text-xs mb-1">Số lượng</label>
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
                  Xóa
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
            {saving ? "Đang lưu..." : "Lưu thay đổi"}
          </button>

          {event.status === "DRAFT" && (
            <button
              type="button"
              onClick={handleDelete}
              className="py-3 px-6 bg-red-500/10 text-red-400 rounded-lg hover:bg-red-500/20 transition font-medium"
            >
              Xóa
            </button>
          )}
        </div>
      </form>
    </main>
  );
}
