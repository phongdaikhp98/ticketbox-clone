"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { orderService } from "@/lib/order-service";
import { OrderResponse, ORDER_STATUSES, PAYMENT_METHODS } from "@/types/order";
import { PageResponse } from "@/types/event";

export default function OrdersPage() {
  const [orders, setOrders] = useState<PageResponse<OrderResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  useEffect(() => {
    setLoading(true);
    orderService
      .getMyOrders(page, 10)
      .then(setOrders)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [page]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-4xl mx-auto px-4 py-8">
          <h1 className="text-2xl font-bold text-white mb-6">Đơn hàng của tôi</h1>

          {loading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : !orders || orders.content.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-400 mb-4">Bạn chưa có đơn hàng nào</p>
              <Link href="/events" className="text-primary hover:text-green-400 transition">
                Xem các sự kiện
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {orders.content.map((order) => {
                const statusInfo = ORDER_STATUSES[order.status] || {
                  label: order.status,
                  color: "bg-gray-100 text-gray-800",
                };
                return (
                  <Link
                    key={order.id}
                    href={`/orders/${order.id}`}
                    className="block bg-zinc-800 rounded-lg p-4 hover:bg-zinc-750 transition"
                  >
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <p className="text-white font-medium">Đơn hàng #{order.id}</p>
                        <p className="text-gray-500 text-xs">{formatDate(order.createdDate)}</p>
                      </div>
                      <div className="text-right">
                        <span
                          className={`px-2 py-1 text-xs rounded ${statusInfo.color}`}
                        >
                          {statusInfo.label}
                        </span>
                        <p className="text-primary font-semibold mt-1">
                          {formatPrice(order.totalAmount)}
                        </p>
                      </div>
                    </div>
                    <div className="space-y-1">
                      {order.orderItems.map((item) => (
                        <p key={item.id} className="text-gray-400 text-sm">
                          {item.event.title} - {item.ticketTypeName} x{item.quantity}
                        </p>
                      ))}
                    </div>
                    {order.paymentMethod && (
                      <p className="text-gray-500 text-xs mt-2">
                        {PAYMENT_METHODS[order.paymentMethod] || order.paymentMethod}
                      </p>
                    )}
                  </Link>
                );
              })}

              {orders.totalPages > 1 && (
                <Pagination
                  currentPage={orders.number}
                  totalPages={orders.totalPages}
                  onPageChange={setPage}
                />
              )}
            </div>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
