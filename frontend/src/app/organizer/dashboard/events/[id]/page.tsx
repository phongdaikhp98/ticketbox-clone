"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { dashboardService } from "@/lib/dashboard-service";
import { EventStatsResponse, AttendeeResponse } from "@/types/dashboard";
import { PageResponse } from "@/types/event";

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(amount);
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

const TICKET_STATUS_LABELS: Record<string, string> = {
  ISSUED: "Chưa sử dụng",
  USED: "Đã sử dụng",
  CANCELLED: "Đã hủy",
};

export default function EventStatsPage() {
  return (
    <ProtectedRoute roles={["ORGANIZER", "ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <Header />
        <EventStatsContent />
      </div>
    </ProtectedRoute>
  );
}

function EventStatsContent() {
  const params = useParams();
  const eventId = Number(params.id);

  const [stats, setStats] = useState<EventStatsResponse | null>(null);
  const [attendees, setAttendees] = useState<PageResponse<AttendeeResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [attendeesLoading, setAttendeesLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState("");
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const data = await dashboardService.getEventStats(eventId);
        setStats(data);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, [eventId]);

  const fetchAttendees = useCallback(async () => {
    setAttendeesLoading(true);
    try {
      const data = await dashboardService.getAttendees(eventId, {
        page,
        size: 20,
        status: statusFilter || undefined,
        search: search || undefined,
      });
      setAttendees(data);
    } catch {
      // ignore
    } finally {
      setAttendeesLoading(false);
    }
  }, [eventId, page, statusFilter, search]);

  useEffect(() => {
    fetchAttendees();
  }, [fetchAttendees]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setSearch(searchInput);
  };

  if (loading) {
    return <div className="text-center text-gray-400 py-12">Đang tải...</div>;
  }

  if (!stats) {
    return <div className="text-center text-gray-400 py-12">Không tìm thấy sự kiện</div>;
  }

  const checkInRate = stats.totalTicketsSold > 0
    ? Math.round((stats.totalCheckedIn / stats.totalTicketsSold) * 100)
    : 0;

  return (
    <main className="max-w-7xl mx-auto px-4 py-8">
      {/* Back link */}
      <Link href="/organizer/dashboard" className="text-gray-400 hover:text-white text-sm mb-4 inline-block">
        &larr; Quay lại Dashboard
      </Link>

      {/* Event Header */}
      <div className="bg-zinc-800 rounded-lg border border-zinc-700 p-6 mb-6">
        <div className="flex gap-4">
          {stats.event.imageUrl && (
            <img
              src={stats.event.imageUrl}
              alt={stats.event.title}
              className="w-24 h-24 rounded-lg object-cover flex-shrink-0"
            />
          )}
          <div>
            <h1 className="text-2xl font-bold text-white">{stats.event.title}</h1>
            <p className="text-gray-400 mt-1">{formatDate(stats.event.eventDate)}</p>
            <p className="text-gray-500 text-sm">{stats.event.location}</p>
            <span className={`inline-block mt-2 px-2 py-0.5 rounded text-xs ${
              stats.event.status === "PUBLISHED" ? "bg-green-900/30 text-green-400" :
              stats.event.status === "DRAFT" ? "bg-yellow-900/30 text-yellow-400" :
              "bg-red-900/30 text-red-400"
            }`}>
              {stats.event.status === "PUBLISHED" ? "Đã đăng" : stats.event.status === "DRAFT" ? "Nháp" : "Đã hủy"}
            </span>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-zinc-800 rounded-lg border border-zinc-700 p-6">
          <div className="text-green-400 text-sm mb-1">Doanh thu</div>
          <div className="text-2xl font-bold text-white">{formatCurrency(stats.totalRevenue)}</div>
        </div>
        <div className="bg-zinc-800 rounded-lg border border-zinc-700 p-6">
          <div className="text-purple-400 text-sm mb-1">Vé đã bán / Sức chứa</div>
          <div className="text-2xl font-bold text-white">
            {stats.totalTicketsSold} / {stats.totalCapacity}
          </div>
          <div className="mt-2 bg-zinc-700 rounded-full h-2 overflow-hidden">
            <div
              className="bg-purple-500 h-full rounded-full transition-all"
              style={{ width: `${stats.totalCapacity > 0 ? (stats.totalTicketsSold / stats.totalCapacity) * 100 : 0}%` }}
            />
          </div>
        </div>
        <div className="bg-zinc-800 rounded-lg border border-zinc-700 p-6">
          <div className="text-yellow-400 text-sm mb-1">Tỉ lệ check-in</div>
          <div className="text-2xl font-bold text-white">
            {stats.totalCheckedIn} / {stats.totalTicketsSold}
            <span className="text-lg text-gray-400 ml-2">({checkInRate}%)</span>
          </div>
          <div className="mt-2 bg-zinc-700 rounded-full h-2 overflow-hidden">
            <div
              className="bg-yellow-500 h-full rounded-full transition-all"
              style={{ width: `${checkInRate}%` }}
            />
          </div>
        </div>
      </div>

      {/* Ticket Type Breakdown */}
      <section className="mb-8">
        <h2 className="text-lg font-semibold text-white mb-4">Thống kê theo loại vé</h2>
        <div className="bg-zinc-800 rounded-lg border border-zinc-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-zinc-700">
                  <th className="text-left text-gray-400 font-medium px-4 py-3">Loại vé</th>
                  <th className="text-right text-gray-400 font-medium px-4 py-3">Giá</th>
                  <th className="text-center text-gray-400 font-medium px-4 py-3">Đã bán / Sức chứa</th>
                  <th className="text-right text-gray-400 font-medium px-4 py-3">Doanh thu</th>
                  <th className="text-center text-gray-400 font-medium px-4 py-3">Check-in</th>
                </tr>
              </thead>
              <tbody>
                {stats.ticketTypeStats.map((tt) => (
                  <tr key={tt.ticketTypeId} className="border-b border-zinc-700/50">
                    <td className="px-4 py-3 text-white font-medium">{tt.name}</td>
                    <td className="px-4 py-3 text-right text-gray-300">{formatCurrency(tt.price)}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 bg-zinc-700 rounded-full h-2 overflow-hidden">
                          <div
                            className="bg-primary h-full rounded-full"
                            style={{ width: `${tt.capacity > 0 ? (tt.soldCount / tt.capacity) * 100 : 0}%` }}
                          />
                        </div>
                        <span className="text-gray-300 text-xs whitespace-nowrap">
                          {tt.soldCount}/{tt.capacity}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right text-primary font-medium">{formatCurrency(tt.revenue)}</td>
                    <td className="px-4 py-3 text-center text-gray-300">{tt.checkedInCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      {/* Attendee List */}
      <section>
        <h2 className="text-lg font-semibold text-white mb-4">Danh sách người tham dự</h2>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-3 mb-4">
          <form onSubmit={handleSearch} className="flex-1">
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Tìm theo tên hoặc mã vé..."
              className="w-full bg-zinc-800 text-white px-4 py-2 rounded-lg border border-zinc-700 focus:outline-none focus:border-primary text-sm"
            />
          </form>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="bg-zinc-800 text-white px-4 py-2 rounded-lg border border-zinc-700 focus:outline-none focus:border-primary text-sm"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ISSUED">Chưa sử dụng</option>
            <option value="USED">Đã sử dụng</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>

        {/* Table */}
        <div className="bg-zinc-800 rounded-lg border border-zinc-700 overflow-hidden">
          {attendeesLoading ? (
            <div className="text-center text-gray-400 py-8">Đang tải...</div>
          ) : !attendees || attendees.content.length === 0 ? (
            <div className="text-center text-gray-400 py-8">Không có dữ liệu</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-zinc-700">
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Họ tên</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Email</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Loại vé</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Mã vé</th>
                    <th className="text-center text-gray-400 font-medium px-4 py-3">Trạng thái</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Check-in</th>
                  </tr>
                </thead>
                <tbody>
                  {attendees.content.map((a) => (
                    <tr key={a.ticketId} className="border-b border-zinc-700/50 hover:bg-zinc-700/30">
                      <td className="px-4 py-3 text-white">{a.attendeeName}</td>
                      <td className="px-4 py-3 text-gray-400">{a.attendeeEmail}</td>
                      <td className="px-4 py-3 text-gray-300">{a.ticketTypeName}</td>
                      <td className="px-4 py-3 font-mono text-gray-300 text-xs">{a.ticketCode}</td>
                      <td className="px-4 py-3 text-center">
                        <span className={`px-2 py-0.5 rounded text-xs ${
                          a.status === "USED" ? "bg-green-900/30 text-green-400" :
                          a.status === "ISSUED" ? "bg-blue-900/30 text-blue-400" :
                          "bg-red-900/30 text-red-400"
                        }`}>
                          {TICKET_STATUS_LABELS[a.status] || a.status}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-gray-400 text-xs">
                        {a.usedAt ? formatDate(a.usedAt) : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {attendees && (
          <Pagination
            currentPage={attendees.number}
            totalPages={attendees.totalPages}
            onPageChange={setPage}
          />
        )}
      </section>
    </main>
  );
}
