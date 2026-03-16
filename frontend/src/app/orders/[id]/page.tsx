"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { orderService } from "@/lib/order-service";
import { OrderResponse, ORDER_STATUSES, PAYMENT_METHODS } from "@/types/order";

export default function OrderDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState("");
  const [cancelling, setCancelling] = useState(false);
  const [cancelError, setCancelError] = useState("");

  const fetchOrder = async () => {
    try {
      const data = await orderService.getOrderDetail(id);
      setOrder(data);
    } catch {
      setError("Order not found");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!id) return;
    fetchOrder();
  }, [id]);

  const handlePay = async () => {
    setPaying(true);
    setPayError("");
    try {
      const payment = await orderService.createPaymentUrl(id);
      window.location.href = payment.paymentUrl;
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message || "Payment failed"
          : "Payment failed";
      setPayError(msg);
    } finally {
      setPaying(false);
    }
  };

  const handleCancel = async () => {
    if (!confirm("Bạn có chắc muốn hủy đơn hàng này không?")) return;
    setCancelling(true);
    setCancelError("");
    try {
      await orderService.cancelOrder(id);
      await fetchOrder();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message || "Hủy đơn hàng thất bại"
          : "Hủy đơn hàng thất bại";
      setCancelError(msg);
    } finally {
      setCancelling(false);
    }
  };

  const canCancel = (order: OrderResponse): boolean => {
    if (order.status !== "PENDING") return false;
    const eventDate = order.orderItems[0]?.event?.eventDate;
    if (!eventDate) return true;
    const cutoff = new Date();
    cutoff.setHours(cutoff.getHours() + 24);
    return new Date(eventDate) > cutoff;
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      weekday: "long",
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
        <main className="max-w-3xl mx-auto px-4 py-8">
          <Link
            href="/orders"
            className="text-gray-400 hover:text-white text-sm transition mb-4 inline-block"
          >
            &larr; Back to My Orders
          </Link>

          {loading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : error ? (
            <div className="text-center text-red-400 py-12">{error}</div>
          ) : order ? (
            <div className="space-y-6">
              <div className="bg-zinc-800 rounded-lg p-6">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h1 className="text-xl font-bold text-white">
                      Đơn hàng #{order.id}
                    </h1>
                    <p className="text-gray-500 text-sm">
                      {formatDate(order.createdDate)}
                    </p>
                  </div>
                  <span
                    className={`px-3 py-1 text-sm rounded ${
                      (ORDER_STATUSES[order.status] || { color: "bg-gray-100 text-gray-800" }).color
                    }`}
                  >
                    {(ORDER_STATUSES[order.status] || { label: order.status }).label}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-4 text-sm mb-4">
                  <div>
                    <span className="text-gray-500">Phương thức thanh toán</span>
                    <p className="text-white">
                      {order.paymentMethod
                        ? PAYMENT_METHODS[order.paymentMethod] || order.paymentMethod
                        : "-"}
                    </p>
                  </div>
                  <div>
                    <span className="text-gray-500">Trạng thái thanh toán</span>
                    <p className="text-white">{order.paymentStatus}</p>
                  </div>
                </div>
              </div>

              <div className="bg-zinc-800 rounded-lg p-6">
                <h2 className="text-white font-semibold mb-4">Chi tiết đơn hàng</h2>
                <div className="space-y-3">
                  {order.orderItems.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center gap-4 p-3 bg-zinc-700 rounded-lg"
                    >
                      <Link href={`/events/${item.event.id}`} className="shrink-0">
                        {item.event.imageUrl ? (
                          <img
                            src={item.event.imageUrl}
                            alt={item.event.title}
                            className="w-16 h-16 object-cover rounded"
                          />
                        ) : (
                          <div className="w-16 h-16 bg-zinc-600 rounded flex items-center justify-center text-gray-500 text-xs">
                            No img
                          </div>
                        )}
                      </Link>
                      <div className="flex-1 min-w-0">
                        <Link
                          href={`/events/${item.event.id}`}
                          className="text-white font-medium hover:text-primary transition block truncate"
                        >
                          {item.event.title}
                        </Link>
                        <p className="text-gray-400 text-sm">
                          {item.ticketTypeName} x{item.quantity}
                        </p>
                        <p className="text-gray-500 text-xs">{item.event.location}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-white font-medium">
                          {formatPrice(item.unitPrice * item.quantity)}
                        </p>
                        <p className="text-gray-500 text-xs">
                          {formatPrice(item.unitPrice)} each
                        </p>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="border-t border-zinc-700 mt-4 pt-4 flex justify-between">
                  <span className="text-gray-400 font-medium">Tổng cộng</span>
                  <span className="text-2xl font-bold text-primary">
                    {formatPrice(order.totalAmount)}
                  </span>
                </div>
              </div>

              {order.status === "PENDING" && (
                <div className="space-y-3">
                  {payError && (
                    <div className="bg-red-500/10 border border-red-500 rounded-lg p-3 text-red-400 text-sm">
                      {payError}
                    </div>
                  )}
                  {cancelError && (
                    <div className="bg-red-500/10 border border-red-500 rounded-lg p-3 text-red-400 text-sm">
                      {cancelError}
                    </div>
                  )}
                  <button
                    onClick={handlePay}
                    disabled={paying || cancelling}
                    className="w-full py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition font-medium disabled:opacity-50"
                  >
                    {paying ? "Đang xử lý..." : "Thanh toán"}
                  </button>
                  {canCancel(order) && (
                    <button
                      onClick={handleCancel}
                      disabled={cancelling || paying}
                      className="w-full py-3 bg-zinc-700 text-red-400 border border-red-500/30 rounded-lg hover:bg-red-500/10 transition font-medium disabled:opacity-50"
                    >
                      {cancelling ? "Đang hủy..." : "Hủy đơn hàng"}
                    </button>
                  )}
                </div>
              )}

              {order.status === "COMPLETED" && (
                <div className="bg-green-500/10 border border-green-500 rounded-lg p-4 text-center">
                  <p className="text-green-400 font-medium">
                    Thanh toán thành công! Chúc bạn có một trải nghiệm tuyệt vời.
                  </p>
                </div>
              )}

              {order.status === "CANCELLED" && (
                <div className="bg-zinc-700/50 border border-zinc-600 rounded-lg p-4 text-center">
                  <p className="text-gray-400 font-medium">Đơn hàng đã được hủy.</p>
                </div>
              )}
            </div>
          ) : null}
        </main>
      </div>
    </ProtectedRoute>
  );
}
