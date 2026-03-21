"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import api from "@/lib/api";
import { ApiResponse } from "@/types/auth";
import { RefundResponse, REFUND_STATUSES } from "@/types/order";
import { PageResponse } from "@/types/event";

const FILTER_TABS = [
  { value: "", label: "Tất cả" },
  { value: "PROCESSING", label: "Đang xử lý" },
  { value: "COMPLETED", label: "Thành công" },
  { value: "FAILED", label: "Thất bại" },
];

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

const formatPrice = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);

export default function AdminRefundsPage() {
  const [data, setData] = useState<PageResponse<RefundResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(0);

  const fetchRefunds = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const res = await api.get<ApiResponse<PageResponse<RefundResponse>>>(
        "/v1/admin/refunds",
        { params }
      );
      setData(res.data.data);
    } catch (err) {
      console.error("Failed to fetch refunds", err);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, page]);

  useEffect(() => {
    fetchRefunds();
  }, [fetchRefunds]);

  const handleFilterChange = (value: string) => {
    setStatusFilter(value);
    setPage(0);
  };

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-6xl mx-auto px-4 py-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold text-white">Quản lý Hoàn tiền</h1>
              <p className="text-gray-400 text-sm mt-1">
                {data?.totalElements ?? 0} yêu cầu hoàn tiền
              </p>
            </div>
          </div>

          {/* Filter tabs */}
          <div className="flex gap-2 mb-6 flex-wrap">
            {FILTER_TABS.map((tab) => (
              <button
                key={tab.value}
                onClick={() => handleFilterChange(tab.value)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                  statusFilter === tab.value
                    ? "bg-primary text-white"
                    : "bg-zinc-800 text-gray-400 hover:bg-zinc-700"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="text-center text-gray-400 py-12">Đang tải...</div>
          ) : !data || data.content.length === 0 ? (
            <div className="text-center text-gray-400 py-12">Chưa có yêu cầu hoàn tiền nào.</div>
          ) : (
            <>
              <div className="bg-zinc-800 rounded-lg overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-zinc-700">
                      <th className="text-left text-gray-400 text-sm font-medium px-4 py-3">ID</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-4 py-3">Đơn hàng</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-4 py-3">Số tiền</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-4 py-3">Trạng thái</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-4 py-3">VNPay Code</th>
                      <th className="text-left text-gray-400 text-sm font-medium px-4 py-3">Ngày tạo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((refund) => {
                      const statusInfo = REFUND_STATUSES[refund.status] || { label: refund.status, color: "bg-gray-500/20 text-gray-400" };
                      return (
                        <tr key={refund.id} className="border-b border-zinc-700 hover:bg-zinc-750 last:border-0">
                          <td className="px-4 py-3 text-gray-300 text-sm">#{refund.id}</td>
                          <td className="px-4 py-3">
                            <Link
                              href={`/admin/orders/${refund.orderId}`}
                              className="text-blue-400 hover:text-blue-300 text-sm font-medium"
                            >
                              Đơn #{refund.orderId}
                            </Link>
                          </td>
                          <td className="px-4 py-3 text-white text-sm font-medium">
                            {formatPrice(refund.amount)}
                          </td>
                          <td className="px-4 py-3">
                            <span className={`px-2 py-1 text-xs rounded font-medium ${statusInfo.color}`}>
                              {statusInfo.label}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            {refund.vnpayResponseCode ? (
                              <div>
                                <span className={`text-xs font-mono ${refund.vnpayResponseCode === "00" ? "text-green-400" : "text-red-400"}`}>
                                  {refund.vnpayResponseCode}
                                </span>
                                {refund.vnpayResponseMessage && (
                                  <p className="text-gray-500 text-xs mt-0.5 truncate max-w-[200px]" title={refund.vnpayResponseMessage}>
                                    {refund.vnpayResponseMessage}
                                  </p>
                                )}
                              </div>
                            ) : (
                              <span className="text-gray-600 text-xs">—</span>
                            )}
                          </td>
                          <td className="px-4 py-3 text-gray-400 text-sm">
                            {formatDate(refund.createdDate)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {data.totalPages > 1 && (
                <div className="mt-4 flex justify-center">
                  <Pagination
                    currentPage={page}
                    totalPages={data.totalPages}
                    onPageChange={setPage}
                  />
                </div>
              )}
            </>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
