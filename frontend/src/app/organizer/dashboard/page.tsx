"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { dashboardService } from "@/lib/dashboard-service";
import { eventService } from "@/lib/event-service";
import { DashboardOverviewResponse } from "@/types/dashboard";
import { Event, PageResponse } from "@/types/event";

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

export default function DashboardPage() {
  return (
    <ProtectedRoute roles={["ORGANIZER", "ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <Header />
        <DashboardContent />
      </div>
    </ProtectedRoute>
  );
}

function DashboardContent() {
  const [overview, setOverview] = useState<DashboardOverviewResponse | null>(null);
  const [events, setEvents] = useState<PageResponse<Event> | null>(null);
  const [loading, setLoading] = useState(true);
  const [eventsPage, setEventsPage] = useState(0);

  useEffect(() => {
    const fetchOverview = async () => {
      try {
        const data = await dashboardService.getOverview();
        setOverview(data);
      } catch {
        // ignore
      }
    };
    fetchOverview();
  }, []);

  useEffect(() => {
    const fetchEvents = async () => {
      setLoading(true);
      try {
        const data = await eventService.getMyEvents(eventsPage, 6);
        setEvents(data);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    };
    fetchEvents();
  }, [eventsPage]);

  return (
    <main className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-white mb-6">Dashboard Ban Tổ chức</h1>

      {/* KPI Cards */}
      {overview && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <StatCard
            title="Tổng sự kiện"
            value={overview.totalEvents.toString()}
            icon={
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            }
            color="text-blue-400"
          />
          <StatCard
            title="Tổng doanh thu"
            value={formatCurrency(overview.totalRevenue)}
            icon={
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            }
            color="text-green-400"
          />
          <StatCard
            title="Vé đã bán"
            value={overview.totalTicketsSold.toString()}
            icon={
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
              </svg>
            }
            color="text-purple-400"
          />
          <StatCard
            title="Đã check-in"
            value={overview.totalCheckedIn.toString()}
            icon={
              <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            }
            color="text-yellow-400"
          />
        </div>
      )}

      {/* My Events */}
      <section className="mb-8">
        <h2 className="text-lg font-semibold text-white mb-4">Sự kiện của tôi</h2>
        {loading ? (
          <div className="text-center text-gray-400 py-8">Đang tải...</div>
        ) : !events || events.content.length === 0 ? (
          <div className="text-center text-gray-400 py-8">
            Chưa có sự kiện nào.{" "}
            <Link href="/events/create" className="text-primary hover:underline">
              Tạo sự kiện đầu tiên
            </Link>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {events.content.map((event) => (
                <Link
                  key={event.id}
                  href={`/organizer/dashboard/events/${event.id}`}
                  className="bg-zinc-800 rounded-lg border border-zinc-700 p-4 hover:border-primary transition group"
                >
                  <div className="flex gap-3">
                    {event.imageUrl && (
                      <img
                        src={event.imageUrl}
                        alt={event.title}
                        className="w-16 h-16 rounded object-cover flex-shrink-0"
                      />
                    )}
                    <div className="min-w-0">
                      <h3 className="text-white font-medium truncate group-hover:text-primary transition">
                        {event.title}
                      </h3>
                      <p className="text-gray-400 text-sm mt-1">{formatDate(event.eventDate)}</p>
                      <p className="text-gray-500 text-xs mt-0.5 truncate">{event.location}</p>
                    </div>
                  </div>
                  <div className="mt-3 flex items-center justify-between text-xs">
                    <span className={`px-2 py-0.5 rounded ${
                      event.status === "PUBLISHED" ? "bg-green-900/30 text-green-400" :
                      event.status === "DRAFT" ? "bg-yellow-900/30 text-yellow-400" :
                      "bg-red-900/30 text-red-400"
                    }`}>
                      {event.status === "PUBLISHED" ? "Đã đăng" : event.status === "DRAFT" ? "Nháp" : "Đã hủy"}
                    </span>
                    <span className="text-gray-400">
                      {event.ticketTypes.reduce((sum, t) => sum + t.soldCount, 0)}/
                      {event.ticketTypes.reduce((sum, t) => sum + t.capacity, 0)} vé
                    </span>
                  </div>
                </Link>
              ))}
            </div>
            <Pagination
              currentPage={events.number}
              totalPages={events.totalPages}
              onPageChange={setEventsPage}
            />
          </>
        )}
      </section>

      {/* Recent Orders */}
      {overview && overview.recentOrders.length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-white mb-4">Đơn hàng gần đây</h2>
          <div className="bg-zinc-800 rounded-lg border border-zinc-700 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-zinc-700">
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Mã đơn</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Khách hàng</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Sự kiện</th>
                    <th className="text-right text-gray-400 font-medium px-4 py-3">Tổng tiền</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Trạng thái</th>
                    <th className="text-left text-gray-400 font-medium px-4 py-3">Ngày đặt</th>
                  </tr>
                </thead>
                <tbody>
                  {overview.recentOrders.map((order) => (
                    <tr key={order.orderId} className="border-b border-zinc-700/50 hover:bg-zinc-700/30">
                      <td className="px-4 py-3 text-white font-mono">#{order.orderId}</td>
                      <td className="px-4 py-3">
                        <div className="text-white">{order.customerName}</div>
                        <div className="text-gray-500 text-xs">{order.customerEmail}</div>
                      </td>
                      <td className="px-4 py-3 text-gray-300">{order.eventTitle}</td>
                      <td className="px-4 py-3 text-right text-primary font-medium">
                        {formatCurrency(order.totalAmount)}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-0.5 rounded text-xs ${
                          order.status === "COMPLETED" ? "bg-green-900/30 text-green-400" :
                          order.status === "PENDING" ? "bg-yellow-900/30 text-yellow-400" :
                          "bg-red-900/30 text-red-400"
                        }`}>
                          {order.status === "COMPLETED" ? "Hoàn thành" :
                           order.status === "PENDING" ? "Chờ thanh toán" : "Đã hủy"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-gray-400">{formatDate(order.createdDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}
    </main>
  );
}

function StatCard({ title, value, icon, color }: {
  title: string;
  value: string;
  icon: React.ReactNode;
  color: string;
}) {
  return (
    <div className="bg-zinc-800 rounded-lg border border-zinc-700 p-6">
      <div className="flex items-center justify-between mb-4">
        <span className={color}>{icon}</span>
      </div>
      <div className="text-2xl font-bold text-white mb-1">{value}</div>
      <div className="text-gray-400 text-sm">{title}</div>
    </div>
  );
}
