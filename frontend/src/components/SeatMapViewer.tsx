"use client";

import { useState, useEffect, useCallback } from "react";
import { SeatMapResponse, SectionResponse, SeatResponse } from "@/types/seat-map";
import { seatMapService } from "@/lib/seat-map-service";

interface SeatMapViewerProps {
  eventId: number;
  onSeatSelected?: (seat: SeatResponse, sectionName: string, price: number) => void;
}

const RESERVATION_TTL = 600; // 10 minutes in seconds

export default function SeatMapViewer({ eventId, onSeatSelected }: SeatMapViewerProps) {
  const [seatMap, setSeatMap] = useState<SeatMapResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedSeat, setSelectedSeat] = useState<{ seat: SeatResponse; section: SectionResponse } | null>(null);
  const [reserving, setReserving] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);

  useEffect(() => {
    seatMapService.getSeatMapByEvent(eventId)
      .then(setSeatMap)
      .catch(() => setError("Không thể tải sơ đồ chỗ ngồi"))
      .finally(() => setLoading(false));
  }, [eventId]);

  // Countdown timer
  useEffect(() => {
    if (countdown === null) return;
    if (countdown <= 0) {
      setCountdown(null);
      setSelectedSeat(null);
      // Reload seat map to reflect expired reservation
      seatMapService.getSeatMapByEvent(eventId).then(setSeatMap).catch(() => {});
      return;
    }
    const timer = setTimeout(() => setCountdown((c) => (c !== null ? c - 1 : null)), 1000);
    return () => clearTimeout(timer);
  }, [countdown, eventId]);

  const handleSeatClick = useCallback(async (seat: SeatResponse, section: SectionResponse) => {
    if (seat.status !== "AVAILABLE" && !seat.reservedByMe) return;

    // If clicking own reserved seat, toggle it off
    if (seat.reservedByMe) {
      try {
        await seatMapService.releaseSeat(seat.id);
        setSelectedSeat(null);
        setCountdown(null);
        setSeatMap((prev) =>
          prev
            ? {
                ...prev,
                sections: prev.sections.map((s) =>
                  s.id === section.id
                    ? {
                        ...s,
                        seats: s.seats.map((seat2) =>
                          seat2.id === seat.id
                            ? { ...seat2, status: "AVAILABLE" as const, reservedByMe: false }
                            : seat2
                        ),
                      }
                    : s
                ),
              }
            : prev
        );
      } catch {
        // ignore
      }
      return;
    }

    // Release previous selection if any
    if (selectedSeat) {
      try {
        await seatMapService.releaseSeat(selectedSeat.seat.id);
      } catch {
        // ignore
      }
      setSeatMap((prev) =>
        prev
          ? {
              ...prev,
              sections: prev.sections.map((s) =>
                s.id === selectedSeat.section.id
                  ? {
                      ...s,
                      seats: s.seats.map((seat2) =>
                        seat2.id === selectedSeat.seat.id
                          ? { ...seat2, status: "AVAILABLE" as const, reservedByMe: false }
                          : seat2
                      ),
                    }
                  : s
              ),
            }
          : prev
      );
    }

    setReserving(true);
    try {
      await seatMapService.reserveSeat(seat.id);
      setSeatMap((prev) =>
        prev
          ? {
              ...prev,
              sections: prev.sections.map((s) =>
                s.id === section.id
                  ? {
                      ...s,
                      seats: s.seats.map((seat2) =>
                        seat2.id === seat.id
                          ? { ...seat2, status: "RESERVED" as const, reservedByMe: true }
                          : seat2
                      ),
                    }
                  : s
              ),
            }
          : prev
      );
      setSelectedSeat({ seat: { ...seat, status: "RESERVED", reservedByMe: true }, section });
      setCountdown(RESERVATION_TTL);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || "Ghế đã được chọn");
      setTimeout(() => setError(null), 3000);
    } finally {
      setReserving(false);
    }
  }, [selectedSeat]);

  const handleAddToCart = async () => {
    if (!selectedSeat) return;
    try {
      await seatMapService.addSeatToCart(selectedSeat.seat.id);
      onSeatSelected?.(selectedSeat.seat, selectedSeat.section.name, selectedSeat.section.price);
      setSelectedSeat(null);
      setCountdown(null);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || "Không thể thêm vào giỏ");
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-40 text-gray-400">
        Đang tải sơ đồ chỗ ngồi...
      </div>
    );
  }

  if (error && !seatMap) {
    return <div className="text-red-400 text-sm">{error}</div>;
  }

  if (!seatMap) return null;

  return (
    <div className="space-y-4">
      {error && (
        <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
          {error}
        </div>
      )}

      {/* Legend */}
      <div className="flex flex-wrap gap-4 text-sm text-gray-400">
        <span className="flex items-center gap-2">
          <span className="w-4 h-4 rounded bg-green-500 inline-block" /> Trống
        </span>
        <span className="flex items-center gap-2">
          <span className="w-4 h-4 rounded bg-blue-500 inline-block" /> Đang chọn
        </span>
        <span className="flex items-center gap-2">
          <span className="w-4 h-4 rounded bg-yellow-500 inline-block" /> Đang giữ
        </span>
        <span className="flex items-center gap-2">
          <span className="w-4 h-4 rounded bg-gray-600 inline-block" /> Đã bán
        </span>
      </div>

      {/* Stage */}
      <div className="text-center py-2 bg-zinc-700 rounded text-gray-400 text-sm font-medium tracking-widest">
        ← SÂN KHẤU →
      </div>

      {/* Sections */}
      {seatMap.sections.map((section) => {
        const rows: Record<string, SeatResponse[]> = {};
        for (const seat of section.seats) {
          if (!rows[seat.rowLabel]) rows[seat.rowLabel] = [];
          rows[seat.rowLabel].push(seat);
        }
        const rowLabels = Object.keys(rows).sort();
        const maxSeats = Math.max(...section.seats.map((s) => s.seatNumber), 1);

        return (
          <div key={section.id} className="bg-zinc-800 rounded-lg p-4">
            <div className="flex items-center gap-3 mb-3">
              <span
                className="w-3 h-3 rounded"
                style={{ backgroundColor: section.color || "#6366f1" }}
              />
              <span className="text-white font-medium text-sm">{section.name}</span>
              <span className="text-gray-400 text-xs">
                {section.ticketTypeName} — {section.price.toLocaleString("vi-VN")}đ
              </span>
            </div>

            <div className="overflow-x-auto">
              <div className="inline-block min-w-full">
                {rowLabels.map((row) => (
                  <div key={row} className="flex items-center gap-1 mb-1">
                    <span className="w-6 text-center text-gray-500 text-xs flex-shrink-0">
                      {row}
                    </span>
                    {rows[row]
                      .sort((a, b) => a.seatNumber - b.seatNumber)
                      .map((seat) => {
                        let bgColor = "";
                        let title = "";
                        let clickable = false;

                        if (seat.reservedByMe) {
                          bgColor = "bg-blue-500 hover:bg-blue-400";
                          title = `${seat.seatCode} — đang chọn (click để bỏ chọn)`;
                          clickable = true;
                        } else if (seat.status === "AVAILABLE") {
                          bgColor = "hover:opacity-80 cursor-pointer";
                          title = `${seat.seatCode} — ${section.price.toLocaleString("vi-VN")}đ`;
                          clickable = true;
                        } else if (seat.status === "RESERVED") {
                          bgColor = "bg-yellow-500 cursor-not-allowed opacity-60";
                          title = `${seat.seatCode} — đang giữ`;
                        } else if (seat.status === "SOLD") {
                          bgColor = "bg-gray-600 cursor-not-allowed opacity-50";
                          title = `${seat.seatCode} — đã bán`;
                        } else if (seat.status === "BLOCKED") {
                          bgColor = "bg-gray-700 cursor-not-allowed opacity-30";
                          title = `${seat.seatCode} — đã khóa`;
                        }

                        return (
                          <button
                            key={seat.id}
                            title={title}
                            disabled={!clickable || reserving}
                            onClick={() => handleSeatClick(seat, section)}
                            className={`w-7 h-7 rounded text-xs font-medium text-white transition-all flex-shrink-0 ${bgColor}`}
                            style={
                              seat.status === "AVAILABLE" && !seat.reservedByMe
                                ? { backgroundColor: section.color || "#22c55e" }
                                : {}
                            }
                          >
                            {seat.seatNumber}
                          </button>
                        );
                      })}
                  </div>
                ))}
              </div>
            </div>
          </div>
        );
      })}

      {/* Selected seat action bar */}
      {selectedSeat && (
        <div className="fixed bottom-0 left-0 right-0 bg-zinc-900 border-t border-zinc-700 p-4 z-50">
          <div className="max-w-3xl mx-auto flex items-center justify-between gap-4">
            <div>
              <p className="text-white font-medium">
                Ghế {selectedSeat.seat.seatCode} — {selectedSeat.section.name}
              </p>
              <p className="text-green-400 font-bold">
                {selectedSeat.section.price.toLocaleString("vi-VN")}đ
              </p>
              {countdown !== null && (
                <p className="text-yellow-400 text-sm">
                  Giữ chỗ trong:{" "}
                  {Math.floor(countdown / 60)}:{String(countdown % 60).padStart(2, "0")}
                </p>
              )}
            </div>
            <button
              onClick={handleAddToCart}
              className="px-6 py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition font-medium"
            >
              Thêm vào giỏ hàng
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
