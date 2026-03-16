"use client";

import { useState } from "react";
import { SeatMapResponse, SectionResponse, SeatResponse } from "@/types/seat-map";
import { seatMapService } from "@/lib/seat-map-service";

interface SeatMapEditorProps {
  seatMap: SeatMapResponse;
  onUpdate?: (updated: SeatMapResponse) => void;
}

const STATUS_COLORS: Record<string, string> = {
  AVAILABLE: "fill-green-500 hover:fill-green-400 cursor-pointer",
  SOLD: "fill-gray-600 cursor-not-allowed",
  BLOCKED: "fill-red-700 hover:fill-red-600 cursor-pointer",
  RESERVED: "fill-yellow-500 cursor-not-allowed",
};

export default function SeatMapEditor({ seatMap, onUpdate }: SeatMapEditorProps) {
  const [sections, setSections] = useState<SectionResponse[]>(seatMap.sections);
  const [saving, setSaving] = useState<number | null>(null);

  const toggleSeat = async (section: SectionResponse, seat: SeatResponse) => {
    if (seat.status === "SOLD" || seat.status === "RESERVED") return;

    const newStatus = seat.status === "BLOCKED" ? "AVAILABLE" : "BLOCKED";
    setSaving(seat.id);

    try {
      await seatMapService.updateSeatStatus(seatMap.id, seat.id, newStatus);
      setSections((prev) =>
        prev.map((s) =>
          s.id === section.id
            ? {
                ...s,
                seats: s.seats.map((seat2) =>
                  seat2.id === seat.id ? { ...seat2, status: newStatus } : seat2
                ),
              }
            : s
        )
      );
      onUpdate?.({
        ...seatMap,
        sections: sections.map((s) =>
          s.id === section.id
            ? {
                ...s,
                seats: s.seats.map((seat2) =>
                  seat2.id === seat.id ? { ...seat2, status: newStatus } : seat2
                ),
              }
            : s
        ),
      });
    } catch {
      // ignore
    } finally {
      setSaving(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Legend */}
      <div className="flex flex-wrap gap-4 text-sm">
        <span className="flex items-center gap-2">
          <span className="w-4 h-4 rounded bg-green-500 inline-block" /> Trống
        </span>
        <span className="flex items-center gap-2">
          <span className="w-4 h-4 rounded bg-red-700 inline-block" /> Đã khóa
        </span>
        <span className="flex items-center gap-2">
          <span className="w-4 h-4 rounded bg-gray-600 inline-block" /> Đã bán
        </span>
        <span className="flex items-center gap-2 text-gray-400">
          Click ghế để khóa/mở khóa
        </span>
      </div>

      {sections.map((section) => {
        // Group seats by row
        const rows: Record<string, SeatResponse[]> = {};
        for (const seat of section.seats) {
          if (!rows[seat.rowLabel]) rows[seat.rowLabel] = [];
          rows[seat.rowLabel].push(seat);
        }
        const rowLabels = Object.keys(rows).sort();

        return (
          <div key={section.id} className="bg-zinc-800 rounded-lg p-4">
            <div className="flex items-center gap-3 mb-4">
              <span
                className="w-4 h-4 rounded"
                style={{ backgroundColor: section.color || "#6366f1" }}
              />
              <h3 className="text-white font-medium">{section.name}</h3>
              <span className="text-gray-400 text-sm">
                — {section.ticketTypeName} ({section.price.toLocaleString("vi-VN")}đ)
              </span>
            </div>

            <div className="overflow-x-auto">
              <svg
                viewBox={`0 0 ${(Math.max(...section.seats.map(s => s.seatNumber)) || 1) * 36 + 60} ${rowLabels.length * 36 + 20}`}
                className="w-full max-w-3xl"
              >
                {/* Stage indicator */}
                {rowLabels.map((row, rowIdx) => (
                  <g key={row}>
                    {/* Row label */}
                    <text
                      x="20"
                      y={rowIdx * 36 + 22}
                      className="fill-gray-400"
                      fontSize="12"
                      textAnchor="middle"
                      style={{ fill: "#9ca3af" }}
                    >
                      {row}
                    </text>

                    {rows[row]
                      .sort((a, b) => a.seatNumber - b.seatNumber)
                      .map((seat) => {
                        const x = 40 + (seat.seatNumber - 1) * 36;
                        const y = rowIdx * 36;
                        const colorClass = STATUS_COLORS[seat.status] || STATUS_COLORS.AVAILABLE;
                        const isBusy = saving === seat.id;

                        return (
                          <g key={seat.id} onClick={() => toggleSeat(section, seat)}>
                            <rect
                              x={x + 2}
                              y={y + 2}
                              width="28"
                              height="28"
                              rx="4"
                              className={colorClass}
                              style={{
                                fill:
                                  seat.status === "AVAILABLE"
                                    ? section.color || "#22c55e"
                                    : seat.status === "BLOCKED"
                                    ? "#b91c1c"
                                    : seat.status === "SOLD"
                                    ? "#4b5563"
                                    : "#eab308",
                                opacity: isBusy ? 0.5 : 1,
                                cursor: seat.status === "SOLD" || seat.status === "RESERVED" ? "not-allowed" : "pointer",
                              }}
                            />
                            <text
                              x={x + 16}
                              y={y + 20}
                              textAnchor="middle"
                              fontSize="9"
                              style={{ fill: "white", pointerEvents: "none" }}
                            >
                              {seat.seatNumber}
                            </text>
                          </g>
                        );
                      })}
                  </g>
                ))}
              </svg>
            </div>
          </div>
        );
      })}
    </div>
  );
}
