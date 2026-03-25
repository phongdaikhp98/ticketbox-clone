"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { orderService } from "@/lib/order-service";
import { OrderResponse, RefundResponse, ORDER_STATUSES, REFUND_STATUSES, PAYMENT_METHODS } from "@/types/order";

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
  const [refund, setRefund] = useState<RefundResponse | null>(null);
  const [refunding, setRefunding] = useState(false);
  const [refundError, setRefundError] = useState("");

  const fetchOrder = async () => {
    try {
      const data = await orderService.getOrderDetail(id);
      setOrder(data);
      // Fetch refund status for COMPLETED or CANCELLED orders
      if (data.status === "COMPLETED" || data.status === "CANCELLED") {
        try {
          const refundData = await orderService.getRefundStatus(id);
          setRefund(refundData);
        } catch {
          // no refund exists — fine
        }
      }
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

  const handleRefund = async () => {
    if (!confirm("Bạn có chắc muốn yêu cầu hoàn tiền cho đơn hàng này không?\nVé sẽ bị hủy và tiền sẽ được hoàn lại trong 3-5 ngày làm việc.")) return;
    setRefunding(true);
    setRefundError("");
    try {
      const result = await orderService.requestRefund(id);
      setRefund(result);
      await fetchOrder();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message || "Yêu cầu hoàn tiền thất bại"
          : "Yêu cầu hoàn tiền thất bại";
      setRefundError(msg);
    } finally {
      setRefunding(false);
    }
  };

  const canRefund = (order: OrderResponse): boolean => {
    if (order.status !== "COMPLETED") return false;
    if (refund && refund.status !== "FAILED") return false; // has active/completed refund
    const cutoff = new Date();
    cutoff.setHours(cutoff.getHours() + 24);
    return order.orderItems.every(
      (item) => !item.event.eventDate || new Date(item.event.eventDate) > cutoff
    );
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

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm mb-4">
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
                      className="flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4 p-3 bg-zinc-700 rounded-lg"
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

                <div className="border-t border-zinc-700 mt-4 pt-4 space-y-2">
                  {order.discountAmount && order.discountAmount > 0 ? (
                    <>
                      <div className="flex justify-between text-sm">
                        <span className="text-gray-400">Tạm tính</span>
                        <span className="text-gray-300">
                          {formatPrice(order.originalAmount ?? order.totalAmount)}
                        </span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-green-400">
                          Giảm giá{order.promoCode ? ` (${order.promoCode})` : ""}
                        </span>
                        <span className="text-green-400">
                          -{formatPrice(order.discountAmount)}
                        </span>
                      </div>
                      <div className="flex justify-between pt-2 border-t border-zinc-600">
                        <span className="text-gray-400 font-medium">Tổng cộng</span>
                        <span className="text-2xl font-bold text-primary">
                          {formatPrice(order.totalAmount)}
                        </span>
                      </div>
                    </>
                  ) : (
                    <div className="flex justify-between">
                      <span className="text-gray-400 font-medium">Tổng cộng</span>
                      <span className="text-2xl font-bold text-primary">
                        {formatPrice(order.totalAmount)}
                      </span>
                    </div>
                  )}
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

              {order.status === "COMPLETED" && !refund && (
                <div className="space-y-3">
                  <div className="bg-green-500/10 border border-green-500 rounded-lg p-4 text-center">
                    <p className="text-green-400 font-medium">
                      Thanh toán thành công! Chúc bạn có một trải nghiệm tuyệt vời.
                    </p>
                  </div>
                  {canRefund(order) && (
                    <>
                      {refundError && (
                        <div className="bg-red-500/10 border border-red-500 rounded-lg p-3 text-red-400 text-sm">
                          {refundError}
                        </div>
                      )}
                      <button
                        onClick={handleRefund}
                        disabled={refunding}
                        className="w-full py-3 bg-zinc-700 text-orange-400 border border-orange-500/30 rounded-lg hover:bg-orange-500/10 transition font-medium disabled:opacity-50"
                      >
                        {refunding ? "Đang xử lý..." : "💸 Yêu cầu hoàn tiền"}
                      </button>
                      <p className="text-xs text-gray-500 text-center">
                        Chỉ áp dụng khi sự kiện còn hơn 24 giờ nữa mới bắt đầu
                      </p>
                    </>
                  )}
                </div>
              )}

              {/* Refund status display */}
              {refund && (
                <div className={`rounded-lg p-4 border ${
                  refund.status === "COMPLETED"
                    ? "bg-blue-500/10 border-blue-500"
                    : refund.status === "FAILED"
                    ? "bg-red-500/10 border-red-500"
                    : "bg-yellow-500/10 border-yellow-500"
                }`}>
                  <div className="flex items-center justify-between mb-2">
                    <p className={`font-medium ${
                      refund.status === "COMPLETED" ? "text-blue-400"
                      : refund.status === "FAILED" ? "text-red-400"
                      : "text-yellow-400"
                    }`}>
                      {(REFUND_STATUSES[refund.status] || { label: refund.status }).label}
                    </p>
                    <span className="text-gray-400 text-sm">
                      {formatPrice(refund.amount)}
                    </span>
                  </div>
                  {refund.status === "COMPLETED" && (
                    <p className="text-gray-400 text-sm">
                      Tiền sẽ được hoàn vào tài khoản trong 3–5 ngày làm việc.
                    </p>
                  )}
                  {refund.status === "FAILED" && (
                    <div className="space-y-2">
                      {refund.vnpayResponseMessage && (
                        <p className="text-gray-400 text-sm">Lý do: {refund.vnpayResponseMessage}</p>
                      )}
                      {canRefund(order) && (
                        <>
                          {refundError && (
                            <div className="bg-red-500/10 border border-red-500 rounded p-2 text-red-400 text-xs">
                              {refundError}
                            </div>
                          )}
                          <button
                            onClick={handleRefund}
                            disabled={refunding}
                            className="w-full py-2 bg-zinc-700 text-orange-400 border border-orange-500/30 rounded-lg hover:bg-orange-500/10 transition text-sm disabled:opacity-50"
                          >
                            {refunding ? "Đang xử lý..." : "Thử lại hoàn tiền"}
                          </button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              )}

              {order.status === "COMPLETED" && refund?.status === "COMPLETED" && (
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
