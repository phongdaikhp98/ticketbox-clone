"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import SeatMapEditor from "@/components/SeatMapEditor";
import { seatMapService } from "@/lib/seat-map-service";
import { eventService } from "@/lib/event-service";
import { SeatMapResponse, SectionConfig } from "@/types/seat-map";
import { Event, TicketTypeInfo } from "@/types/event";

export default function SeatMapPage() {
  return (
    <ProtectedRoute roles={["ORGANIZER", "ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <Header />
        <SeatMapContent />
      </div>
    </ProtectedRoute>
  );
}

function SeatMapContent() {
  const params = useParams();
  const router = useRouter();
  const eventId = Number(params.id);

  const [event, setEvent] = useState<Event | null>(null);
  const [seatMap, setSeatMap] = useState<SeatMapResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);

  // Form state for creating seat map
  const [mapName, setMapName] = useState("Sơ đồ chỗ ngồi");
  const [sections, setSections] = useState<SectionConfig[]>([
    { name: "Standard", color: "#22c55e", ticketTypeId: 0, rowLabels: ["A", "B", "C"], seatsPerRow: 10 },
  ]);

  useEffect(() => {
    Promise.all([
      eventService.getEventForManage(eventId),
      seatMapService.getSeatMapByEvent(eventId).catch(() => null),
    ]).then(([ev, sm]) => {
      setEvent(ev);
      setSeatMap(sm);
      // Initialize section ticketTypeIds from event
      if (ev.ticketTypes.length > 0 && sections[0].ticketTypeId === 0) {
        setSections((prev) =>
          prev.map((s, i) => ({
            ...s,
            ticketTypeId: ev.ticketTypes[i % ev.ticketTypes.length]?.id || ev.ticketTypes[0].id,
          }))
        );
      }
    }).catch(() => setError("Không thể tải thông tin"))
      .finally(() => setLoading(false));
  }, [eventId]);

  const addSection = () => {
    setSections((prev) => [
      ...prev,
      {
        name: `Section ${prev.length + 1}`,
        color: "#6366f1",
        ticketTypeId: event?.ticketTypes[0]?.id || 0,
        rowLabels: ["A"],
        seatsPerRow: 10,
      },
    ]);
  };

  const removeSection = (idx: number) => {
    setSections((prev) => prev.filter((_, i) => i !== idx));
  };

  const updateSection = (idx: number, field: keyof SectionConfig, value: unknown) => {
    setSections((prev) =>
      prev.map((s, i) => (i === idx ? { ...s, [field]: value } : s))
    );
  };

  const handleCreate = async () => {
    if (!event) return;
    setCreating(true);
    setError("");
    try {
      const created = await seatMapService.createSeatMap({
        eventId,
        name: mapName,
        sections,
      });
      setSeatMap(created);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || "Tạo seat map thất bại");
    } finally {
      setCreating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-400">
        Đang tải...
      </div>
    );
  }

  return (
    <main className="max-w-4xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white">Seat Map</h1>
          {event && <p className="text-gray-400 text-sm mt-1">{event.title}</p>}
        </div>
        <button
          onClick={() => router.back()}
          className="text-gray-400 hover:text-white text-sm transition"
        >
          ← Quay lại
        </button>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {seatMap ? (
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-white font-semibold">{seatMap.name}</h2>
            <span className="text-gray-400 text-sm">
              {seatMap.sections.reduce((acc, s) => acc + s.seats.length, 0)} ghế tổng cộng
            </span>
          </div>
          <SeatMapEditor seatMap={seatMap} onUpdate={setSeatMap} />
        </div>
      ) : (
        <div className="bg-zinc-800 rounded-lg p-6 space-y-4">
          <h2 className="text-white font-semibold">Tạo sơ đồ chỗ ngồi</h2>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Tên sơ đồ</label>
            <input
              type="text"
              value={mapName}
              onChange={(e) => setMapName(e.target.value)}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-gray-300 text-sm font-medium">Các khu vực</h3>
              <button
                type="button"
                onClick={addSection}
                className="px-3 py-1 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 text-sm"
              >
                + Thêm khu vực
              </button>
            </div>

            {sections.map((section, idx) => (
              <div key={idx} className="border border-zinc-700 rounded-lg p-4 space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-gray-300 text-sm font-medium">Khu vực {idx + 1}</span>
                  {sections.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeSection(idx)}
                      className="text-red-400 hover:text-red-300 text-sm"
                    >
                      Xóa
                    </button>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-gray-500 text-xs mb-1">Tên khu vực</label>
                    <input
                      type="text"
                      value={section.name}
                      onChange={(e) => updateSection(idx, "name", e.target.value)}
                      className="w-full px-3 py-1.5 bg-zinc-700 border border-zinc-600 rounded text-white text-sm focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-gray-500 text-xs mb-1">Màu</label>
                    <input
                      type="color"
                      value={section.color}
                      onChange={(e) => updateSection(idx, "color", e.target.value)}
                      className="w-full h-8 rounded bg-zinc-700 border border-zinc-600 cursor-pointer"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-gray-500 text-xs mb-1">Loại vé</label>
                    <select
                      value={section.ticketTypeId}
                      onChange={(e) => updateSection(idx, "ticketTypeId", Number(e.target.value))}
                      className="w-full px-3 py-1.5 bg-zinc-700 border border-zinc-600 rounded text-white text-sm focus:outline-none"
                    >
                      {event?.ticketTypes.map((tt: TicketTypeInfo) => (
                        <option key={tt.id} value={tt.id}>
                          {tt.name} ({tt.price.toLocaleString("vi-VN")}đ)
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-gray-500 text-xs mb-1">Ghế mỗi hàng</label>
                    <input
                      type="number"
                      value={section.seatsPerRow}
                      onChange={(e) => updateSection(idx, "seatsPerRow", Number(e.target.value))}
                      min="1"
                      max="30"
                      className="w-full px-3 py-1.5 bg-zinc-700 border border-zinc-600 rounded text-white text-sm focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-gray-500 text-xs mb-1">
                    Nhãn hàng (cách nhau bằng dấu phẩy, vd: A,B,C,D)
                  </label>
                  <input
                    type="text"
                    value={section.rowLabels.join(",")}
                    onChange={(e) =>
                      updateSection(
                        idx,
                        "rowLabels",
                        e.target.value.split(",").map((s) => s.trim()).filter(Boolean)
                      )
                    }
                    placeholder="A,B,C,D"
                    className="w-full px-3 py-1.5 bg-zinc-700 border border-zinc-600 rounded text-white text-sm focus:outline-none"
                  />
                </div>
              </div>
            ))}
          </div>

          <button
            onClick={handleCreate}
            disabled={creating}
            className="w-full py-2.5 bg-primary text-white rounded-lg hover:bg-green-600 transition disabled:opacity-50 font-medium"
          >
            {creating ? "Đang tạo..." : "Tạo sơ đồ chỗ ngồi"}
          </button>
        </div>
      )}
    </main>
  );
}
