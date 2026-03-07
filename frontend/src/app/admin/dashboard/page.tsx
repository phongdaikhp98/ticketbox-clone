"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import ProtectedRoute from "@/components/ProtectedRoute";
import { adminService } from "@/lib/admin-service";
import { AdminOverview } from "@/types/admin";

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr);
  return d.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ thanh toán",
  PAID: "Đã thanh toán",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const ORDER_STATUS_COLORS: Record<string, string> = {
  COMPLETED: "bg-green-900/30 text-green-400",
  PAID: "bg-blue-900/30 text-blue-400",
  PENDING: "bg-yellow-900/30 text-yellow-400",
  CANCELLED: "bg-red-900/30 text-red-400",
};

export default function AdminDashboardPage() {
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    adminService
      .getOverview()
      .then(setOverview)
      .catch(() => setError("Không thể tải dữ liệu dashboard"))
      .finally(() => setLoading(false));
  }, []);

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-7xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="mb-8 flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-white">Quản trị hệ thống</h1>
              <p className="text-gray-400 mt-1">Thống kê tổng quan toàn hệ thống</p>
            </div>
            <Link
              href="/"
              className="text-gray-400 hover:text-white text-sm transition flex items-center gap-2"
            >
              ← Quay lại trang chủ
            </Link>
          </div>

          {loading && (
            <div className="flex justify-center py-20">
              <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {error && (
            <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-4">
              {error}
            </div>
          )}

          {overview && (
            <>
              {/* KPI Cards */}
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng người dùng</p>
                  <p className="text-3xl font-bold text-white mt-1">
                    {overview.totalUsers.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng sự kiện</p>
                  <p className="text-3xl font-bold text-white mt-1">
                    {overview.totalEvents.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng doanh thu</p>
                  <p className="text-2xl font-bold text-primary mt-1">
                    {formatCurrency(overview.totalRevenue)}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng đơn hàng</p>
                  <p className="text-3xl font-bold text-white mt-1">
                    {overview.totalOrders.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Vé đã bán</p>
                  <p className="text-3xl font-bold text-white mt-1">
                    {overview.totalTicketsSold.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Đã check-in</p>
                  <p className="text-3xl font-bold text-white mt-1">
                    {overview.totalCheckedIn.toLocaleString("vi-VN")}
                  </p>
                </div>
              </div>

              {/* Quick Links */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8">
                <Link
                  href="/admin/users"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Người dùng</p>
                  <p className="text-gray-400 text-xs mt-1">Quản lý tài khoản</p>
                </Link>
                <Link
                  href="/admin/orders"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Đơn hàng</p>
                  <p className="text-gray-400 text-xs mt-1">Xem tất cả đơn</p>
                </Link>
                <Link
                  href="/admin/events"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Sự kiện</p>
                  <p className="text-gray-400 text-xs mt-1">Quản lý sự kiện</p>
                </Link>
                <Link
                  href="/organizer/dashboard"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Organizer View</p>
                  <p className="text-gray-400 text-xs mt-1">Dashboard tổ chức</p>
                </Link>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Top Events by Revenue */}
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6">
                  <h2 className="text-lg font-semibold text-white mb-4">
                    Sự kiện doanh thu cao nhất
                  </h2>
                  {overview.topEventsByRevenue.length === 0 ? (
                    <p className="text-gray-400 text-sm">Chưa có dữ liệu</p>
                  ) : (
                    <div className="space-y-3">
                      {overview.topEventsByRevenue.map((item, idx) => (
                        <div
                          key={item.eventId}
                          className="flex items-center justify-between py-2 border-b border-zinc-700 last:border-0"
                        >
                          <div className="flex items-center gap-3">
                            <span className="w-6 h-6 bg-zinc-700 rounded-full flex items-center justify-center text-xs text-gray-400 font-medium">
                              {idx + 1}
                            </span>
                            <Link
                              href={`/organizer/dashboard/events/${item.eventId}`}
                              className="text-white hover:text-primary text-sm transition line-clamp-1"
                            >
                              {item.eventTitle}
                            </Link>
                          </div>
                          <span className="text-primary font-semibold text-sm whitespace-nowrap ml-4">
                            {formatCurrency(item.revenue)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Recent Orders */}
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6">
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-semibold text-white">Đơn hàng gần đây</h2>
                    <Link
                      href="/admin/orders"
                      className="text-primary text-sm hover:underline"
                    >
                      Xem tất cả
                    </Link>
                  </div>
                  {overview.recentOrders.length === 0 ? (
                    <p className="text-gray-400 text-sm">Chưa có đơn hàng</p>
                  ) : (
                    <div className="space-y-3">
                      {overview.recentOrders.map((order) => (
                        <div
                          key={order.orderId}
                          className="flex items-center justify-between py-2 border-b border-zinc-700 last:border-0"
                        >
                          <div className="min-w-0 flex-1">
                            <Link
                              href={`/admin/orders/${order.orderId}`}
                              className="text-white hover:text-primary text-sm font-medium transition"
                            >
                              #{order.orderId}
                            </Link>
                            <p className="text-gray-400 text-xs truncate">{order.customerName}</p>
                          </div>
                          <div className="text-right ml-4 flex-shrink-0">
                            <p className="text-primary text-sm font-semibold">
                              {formatCurrency(order.totalAmount)}
                            </p>
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full ${
                                ORDER_STATUS_COLORS[order.status] || "bg-zinc-700 text-gray-400"
                              }`}
                            >
                              {ORDER_STATUS_LABELS[order.status] || order.status}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
}
