"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import ProtectedRoute from "@/components/ProtectedRoute";
import { adminService } from "@/lib/admin-service";
import { AdminOrder } from "@/types/admin";

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

const PAYMENT_METHOD_LABELS: Record<string, string> = {
  VNPAY: "VNPay",
  CASH: "Tiền mặt",
};

export default function AdminOrderDetailPage() {
  const params = useParams();
  const orderId = Number(params.id);
  const [order, setOrder] = useState<AdminOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    adminService
      .getOrderDetail(orderId)
      .then(setOrder)
      .catch(() => setError("Không tìm thấy đơn hàng"))
      .finally(() => setLoading(false));
  }, [orderId]);

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-4xl mx-auto px-4 py-8">
          {/* Back */}
          <Link
            href="/admin/orders"
            className="text-gray-400 hover:text-white text-sm transition flex items-center gap-2 mb-6"
          >
            ← Quay lại danh sách đơn hàng
          </Link>

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

          {order && (
            <div className="space-y-6">
              {/* Order Header */}
              <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6">
                <div className="flex items-start justify-between">
                  <div>
                    <h1 className="text-2xl font-bold text-white">Đơn hàng #{order.id}</h1>
                    <p className="text-gray-400 mt-1">{formatDate(order.createdDate)}</p>
                  </div>
                  <span
                    className={`text-sm px-3 py-1 rounded-full font-medium ${
                      ORDER_STATUS_COLORS[order.status] || "bg-zinc-700 text-gray-300"
                    }`}
                  >
                    {ORDER_STATUS_LABELS[order.status] || order.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
                  <div>
                    <p className="text-gray-400 text-xs">Khách hàng</p>
                    <p className="text-white text-sm font-medium mt-1">{order.customerName}</p>
                    <p className="text-gray-400 text-xs">{order.customerEmail}</p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs">Tổng tiền</p>
                    <p className="text-primary text-lg font-bold mt-1">
                      {formatCurrency(order.totalAmount)}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs">Thanh toán</p>
                    <p className="text-white text-sm mt-1">
                      {PAYMENT_STATUS_LABELS[order.paymentStatus] || order.paymentStatus}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs">Phương thức</p>
                    <p className="text-white text-sm mt-1">
                      {order.paymentMethod
                        ? PAYMENT_METHOD_LABELS[order.paymentMethod] || order.paymentMethod
                        : "—"}
                    </p>
                  </div>
                </div>
              </div>

              {/* Order Items */}
              <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6">
                <h2 className="text-lg font-semibold text-white mb-4">Chi tiết đơn hàng</h2>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="border-b border-zinc-700">
                      <tr>
                        <th className="text-left text-gray-400 text-xs font-medium pb-3">Sự kiện</th>
                        <th className="text-left text-gray-400 text-xs font-medium pb-3">Loại vé</th>
                        <th className="text-right text-gray-400 text-xs font-medium pb-3">Đơn giá</th>
                        <th className="text-right text-gray-400 text-xs font-medium pb-3">Số lượng</th>
                        <th className="text-right text-gray-400 text-xs font-medium pb-3">Thành tiền</th>
                      </tr>
                    </thead>
                    <tbody>
                      {order.orderItems.map((item) => (
                        <tr key={item.id} className="border-b border-zinc-700 last:border-0">
                          <td className="py-3 text-white text-sm">{item.eventTitle}</td>
                          <td className="py-3 text-gray-300 text-sm">{item.ticketTypeName}</td>
                          <td className="py-3 text-right text-gray-300 text-sm">
                            {formatCurrency(item.unitPrice)}
                          </td>
                          <td className="py-3 text-right text-gray-300 text-sm">{item.quantity}</td>
                          <td className="py-3 text-right text-primary font-semibold text-sm">
                            {formatCurrency(item.unitPrice * item.quantity)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr className="border-t border-zinc-600">
                        <td colSpan={4} className="pt-3 text-right text-gray-400 text-sm font-medium">
                          Tổng cộng
                        </td>
                        <td className="pt-3 text-right text-primary font-bold">
                          {formatCurrency(order.totalAmount)}
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
}
