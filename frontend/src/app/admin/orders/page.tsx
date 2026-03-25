"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { adminService } from "@/lib/admin-service";
import { AdminOrder } from "@/types/admin";
import { PageResponse } from "@/types/event";

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

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

const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chưa thanh toán",
  SUCCESS: "Đã thanh toán",
  FAILED: "Thất bại",
};

export default function AdminOrdersPage() {
  const [data, setData] = useState<PageResponse<AdminOrder> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [exportLoading, setExportLoading] = useState(false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      if (statusFilter) params.status = statusFilter;
      const result = await adminService.getOrders(params);
      setData(result);
    } catch {
      setError("Không thể tải danh sách đơn hàng");
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleSearch = () => {
    setSearch(searchInput);
    setPage(0);
  };

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-7xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-white">Quản lý đơn hàng</h1>
              <p className="text-gray-400 mt-1">
                {data ? `${data.totalElements.toLocaleString("vi-VN")} đơn hàng` : ""}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={async () => {
                  setExportLoading(true);
                  try { await adminService.downloadExport("orders"); }
                  catch { alert("Không thể xuất báo cáo"); }
                  finally { setExportLoading(false); }
                }}
                disabled={exportLoading}
                className="px-4 py-2 bg-zinc-700 hover:bg-zinc-600 text-white text-sm rounded-lg transition flex items-center gap-2 disabled:opacity-50"
              >
                {exportLoading ? "Đang xuất..." : "⬇ Xuất Excel"}
              </button>
              <Link href="/" className="text-gray-400 hover:text-white text-sm transition">
                ← Quay lại trang chủ
              </Link>
            </div>
          </div>

          {/* Filters */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-4 mb-6 flex flex-wrap gap-3">
            <div className="flex gap-2 flex-1 min-w-0 w-full sm:w-auto">
              <input
                type="text"
                placeholder="Tìm theo tên, email khách hàng..."
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
              <option value="PENDING">Chờ thanh toán</option>
              <option value="PAID">Đã thanh toán</option>
              <option value="COMPLETED">Hoàn thành</option>
              <option value="CANCELLED">Đã hủy</option>
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
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Mã đơn</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Khách hàng</th>
                    <th className="hidden lg:table-cell text-left text-gray-400 text-xs font-medium px-4 py-3">Sự kiện</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Tổng tiền</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Trạng thái</th>
                    <th className="hidden sm:table-cell text-left text-gray-400 text-xs font-medium px-4 py-3">Thanh toán</th>
                    <th className="hidden md:table-cell text-left text-gray-400 text-xs font-medium px-4 py-3">Ngày đặt</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={7} className="text-center py-12">
                        <div className="flex justify-center">
                          <div className="w-6 h-6 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                        </div>
                      </td>
                    </tr>
                  ) : data?.content.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="text-center text-gray-400 py-12">
                        Không có đơn hàng nào
                      </td>
                    </tr>
                  ) : (
                    data?.content.map((order) => (
                      <tr
                        key={order.id}
                        className="border-b border-zinc-700 last:border-0 hover:bg-zinc-750 transition"
                      >
                        <td className="px-4 py-3">
                          <Link
                            href={`/admin/orders/${order.id}`}
                            className="text-primary hover:underline text-sm font-medium"
                          >
                            #{order.id}
                          </Link>
                        </td>
                        <td className="px-4 py-3">
                          <p className="text-white text-sm">{order.customerName}</p>
                          <p className="text-gray-400 text-xs">{order.customerEmail}</p>
                        </td>
                        <td className="hidden lg:table-cell px-4 py-3 text-gray-300 text-sm max-w-48 truncate">
                          {order.eventTitle}
                        </td>
                        <td className="px-4 py-3 text-primary font-semibold text-sm whitespace-nowrap">
                          {formatCurrency(order.totalAmount)}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`text-xs px-2 py-1 rounded-full font-medium ${
                              ORDER_STATUS_COLORS[order.status] || "bg-zinc-700 text-gray-300"
                            }`}
                          >
                            {ORDER_STATUS_LABELS[order.status] || order.status}
                          </span>
                        </td>
                        <td className="hidden sm:table-cell px-4 py-3 text-gray-400 text-xs">
                          {PAYMENT_STATUS_LABELS[order.paymentStatus] || order.paymentStatus}
                        </td>
                        <td className="hidden md:table-cell px-4 py-3 text-gray-400 text-sm whitespace-nowrap">
                          {formatDate(order.createdDate)}
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
