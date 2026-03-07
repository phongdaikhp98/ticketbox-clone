"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { adminService } from "@/lib/admin-service";
import { AdminEvent } from "@/types/admin";
import { PageResponse } from "@/types/event";

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

const EVENT_STATUS_LABELS: Record<string, string> = {
  DRAFT: "Nháp",
  PUBLISHED: "Đã đăng",
  CANCELLED: "Đã hủy",
};

const EVENT_STATUS_COLORS: Record<string, string> = {
  PUBLISHED: "bg-green-900/30 text-green-400",
  DRAFT: "bg-yellow-900/30 text-yellow-400",
  CANCELLED: "bg-red-900/30 text-red-400",
};

const CATEGORY_LABELS: Record<string, string> = {
  MUSIC: "Âm nhạc",
  SPORTS: "Thể thao",
  CONFERENCE: "Hội thảo",
  THEATER: "Kịch",
  FILM: "Phim",
  WORKSHOP: "Workshop",
  OTHER: "Khác",
};

export default function AdminEventsPage() {
  const [data, setData] = useState<PageResponse<AdminEvent> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      if (statusFilter) params.status = statusFilter;
      if (categoryFilter) params.category = categoryFilter;
      const result = await adminService.getEvents(params);
      setData(result);
    } catch {
      setError("Không thể tải danh sách sự kiện");
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter, categoryFilter]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  const handleSearch = () => {
    setSearch(searchInput);
    setPage(0);
  };

  const handleToggleFeatured = async (eventId: number) => {
    setActionLoading(eventId);
    try {
      const updated = await adminService.toggleFeatured(eventId);
      setData((prev) =>
        prev
          ? { ...prev, content: prev.content.map((e) => (e.id === eventId ? updated : e)) }
          : prev
      );
    } catch {
      alert("Không thể thay đổi trạng thái nổi bật");
    } finally {
      setActionLoading(null);
    }
  };

  const handleChangeStatus = async (eventId: number, newStatus: string) => {
    if (!newStatus) return;
    setActionLoading(eventId);
    try {
      const updated = await adminService.changeEventStatus(eventId, newStatus);
      setData((prev) =>
        prev
          ? { ...prev, content: prev.content.map((e) => (e.id === eventId ? updated : e)) }
          : prev
      );
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : null;
      alert(msg || "Không thể thay đổi trạng thái sự kiện");
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-7xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-white">Quản lý sự kiện</h1>
              <p className="text-gray-400 mt-1">
                {data ? `${data.totalElements.toLocaleString("vi-VN")} sự kiện` : ""}
              </p>
            </div>
            <Link
              href="/"
              className="text-gray-400 hover:text-white text-sm transition flex items-center gap-2"
            >
              ← Quay lại trang chủ
            </Link>
          </div>

          {/* Filters */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-4 mb-6 flex flex-wrap gap-3">
            <div className="flex gap-2 flex-1 min-w-64">
              <input
                type="text"
                placeholder="Tìm kiếm theo tên sự kiện..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                className="flex-1 bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-400 focus:outline-none focus:border-primary"
              />
              <button
                onClick={handleSearch}
                className="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-green-600 transition"
              >
                Tìm
              </button>
            </div>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              className="bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-primary"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="DRAFT">Nháp</option>
              <option value="PUBLISHED">Đã đăng</option>
              <option value="CANCELLED">Đã hủy</option>
            </select>
            <select
              value={categoryFilter}
              onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
              className="bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-primary"
            >
              <option value="">Tất cả danh mục</option>
              {Object.entries(CATEGORY_LABELS).map(([val, label]) => (
                <option key={val} value={val}>{label}</option>
              ))}
            </select>
          </div>

          {/* Error */}
          {error && (
            <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-4 mb-4">
              {error}
            </div>
          )}

          {/* Table */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-zinc-700">
                  <tr>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">ID</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Tên sự kiện</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Người tổ chức</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Danh mục</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Trạng thái</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Nổi bật</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Vé</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Ngày diễn</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={9} className="text-center py-12">
                        <div className="flex justify-center">
                          <div className="w-6 h-6 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                        </div>
                      </td>
                    </tr>
                  ) : data?.content.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="text-center text-gray-400 py-12">
                        Không có sự kiện nào
                      </td>
                    </tr>
                  ) : (
                    data?.content.map((event) => (
                      <tr
                        key={event.id}
                        className="border-b border-zinc-700 last:border-0 hover:bg-zinc-750 transition"
                      >
                        <td className="px-4 py-3 text-gray-400 text-sm">{event.id}</td>
                        <td className="px-4 py-3 max-w-48">
                          <p className="text-white text-sm font-medium truncate">{event.title}</p>
                        </td>
                        <td className="px-4 py-3 text-gray-300 text-sm max-w-36 truncate">
                          {event.organizerName}
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-sm">
                          {CATEGORY_LABELS[event.category] || event.category}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`text-xs px-2 py-1 rounded-full font-medium ${
                              EVENT_STATUS_COLORS[event.status] || "bg-zinc-700 text-gray-300"
                            }`}
                          >
                            {EVENT_STATUS_LABELS[event.status] || event.status}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <button
                            onClick={() => handleToggleFeatured(event.id)}
                            disabled={actionLoading === event.id}
                            title={event.isFeatured ? "Bỏ nổi bật" : "Đặt nổi bật"}
                            className={`text-xl transition disabled:opacity-50 ${
                              event.isFeatured ? "text-yellow-400" : "text-zinc-600 hover:text-yellow-400"
                            }`}
                          >
                            ★
                          </button>
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-sm whitespace-nowrap">
                          {event.totalSold}/{event.totalCapacity}
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-sm whitespace-nowrap">
                          {formatDate(event.eventDate)}
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            {event.status !== "CANCELLED" && (
                              <select
                                defaultValue=""
                                onChange={(e) => {
                                  if (e.target.value) handleChangeStatus(event.id, e.target.value);
                                  e.target.value = "";
                                }}
                                disabled={actionLoading === event.id}
                                className="bg-zinc-700 border border-zinc-600 rounded px-2 py-1 text-white text-xs focus:outline-none focus:border-primary disabled:opacity-50"
                              >
                                <option value="" disabled>Đổi trạng thái</option>
                                {event.status === "DRAFT" && (
                                  <option value="PUBLISHED">→ Đã đăng</option>
                                )}
                                <option value="CANCELLED">→ Đã hủy</option>
                              </select>
                            )}
                            <Link
                              href={`/organizer/dashboard/events/${event.id}`}
                              className="text-xs text-primary hover:underline whitespace-nowrap"
                            >
                              Thống kê
                            </Link>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="mt-6">
              <Pagination
                currentPage={page}
                totalPages={data.totalPages}
                onPageChange={setPage}
              />
            </div>
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
}
