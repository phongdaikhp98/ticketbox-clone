"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { cartService } from "@/lib/cart-service";
import { orderService } from "@/lib/order-service";
import { CartResponse } from "@/types/cart";
import { PAYMENT_METHODS, ValidatePromoCodeResponse } from "@/types/order";
import { useCart } from "@/contexts/CartContext";

export default function CheckoutPage() {
  const router = useRouter();
  const { refreshCart } = useCart();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState("E_WALLET");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  // Promo code states
  const [promoInput, setPromoInput] = useState("");
  const [appliedPromo, setAppliedPromo] = useState("");
  const [promoResult, setPromoResult] = useState<ValidatePromoCodeResponse | null>(null);
  const [promoLoading, setPromoLoading] = useState(false);
  const [promoError, setPromoError] = useState("");

  useEffect(() => {
    cartService
      .getCart()
      .then((data) => {
        if (data.items.length === 0) {
          router.push("/cart");
          return;
        }
        setCart(data);
      })
      .catch(() => router.push("/cart"))
      .finally(() => setLoading(false));
  }, [router]);

  const handleApplyPromo = async () => {
    const code = promoInput.trim().toUpperCase();
    if (!code) return;
    setPromoLoading(true);
    setPromoError("");
    setPromoResult(null);
    try {
      const subtotal = cart?.totalAmount ?? 0;
      const result = await orderService.validatePromoCode(code, subtotal);
      if (result.valid) {
        setPromoResult(result);
        setAppliedPromo(code);
        setPromoError("");
      } else {
        setPromoError(result.message || "Mã không hợp lệ");
        setAppliedPromo("");
      }
    } catch {
      setPromoError("Không thể kiểm tra mã giảm giá");
      setAppliedPromo("");
    } finally {
      setPromoLoading(false);
    }
  };

  const handleRemovePromo = () => {
    setPromoInput("");
    setAppliedPromo("");
    setPromoResult(null);
    setPromoError("");
  };

  const handleCheckout = async () => {
    setSubmitting(true);
    setError("");
    try {
      const order = await orderService.checkout({
        paymentMethod,
        promoCode: appliedPromo || undefined,
      });
      await refreshCart();

      // Create VNPay payment URL and redirect
      const payment = await orderService.createPaymentUrl(order.id);
      window.location.href = payment.paymentUrl;
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message || "Checkout failed"
          : "Checkout failed";
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const discountedTotal =
    cart && promoResult?.valid && promoResult.discountAmount
      ? Math.max(0, cart.totalAmount - promoResult.discountAmount)
      : cart?.totalAmount ?? 0;

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-2xl mx-auto px-4 py-8">
          <h1 className="text-2xl font-bold text-white mb-6">Thanh toán</h1>

          {loading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : cart ? (
            <div className="space-y-6">
              {/* Order summary */}
              <div className="bg-zinc-800 rounded-lg p-6">
                <h2 className="text-white font-semibold mb-4">Tóm tắt đơn hàng</h2>
                <div className="space-y-3">
                  {cart.items.map((item) => (
                    <div key={item.id} className="flex justify-between text-sm">
                      <div className="text-gray-300">
                        <span>{item.event.title}</span>
                        <span className="text-gray-500"> - {item.ticketType.name}</span>
                        <span className="text-gray-500"> x{item.quantity}</span>
                      </div>
                      <span className="text-white">
                        {formatPrice(item.ticketType.price * item.quantity)}
                      </span>
                    </div>
                  ))}
                </div>
                <div className="border-t border-zinc-700 mt-4 pt-4 space-y-2">
                  {promoResult?.valid && promoResult.discountAmount ? (
                    <>
                      <div className="flex justify-between text-sm">
                        <span className="text-gray-400">Tạm tính</span>
                        <span className="text-gray-300">{formatPrice(cart.totalAmount)}</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-green-400">
                          Giảm giá ({appliedPromo})
                        </span>
                        <span className="text-green-400">
                          -{formatPrice(promoResult.discountAmount)}
                        </span>
                      </div>
                      <div className="flex justify-between pt-1 border-t border-zinc-700">
                        <span className="text-gray-400 font-medium">Tổng cộng</span>
                        <span className="text-xl font-bold text-primary">
                          {formatPrice(discountedTotal)}
                        </span>
                      </div>
                    </>
                  ) : (
                    <div className="flex justify-between">
                      <span className="text-gray-400 font-medium">Tổng cộng</span>
                      <span className="text-xl font-bold text-primary">
                        {formatPrice(cart.totalAmount)}
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* Promo code */}
              <div className="bg-zinc-800 rounded-lg p-6">
                <h2 className="text-white font-semibold mb-4">Mã giảm giá</h2>
                {appliedPromo && promoResult?.valid ? (
                  <div className="flex items-center justify-between bg-green-500/10 border border-green-500/30 rounded-lg px-4 py-3">
                    <div>
                      <span className="text-green-400 font-medium">{appliedPromo}</span>
                      <span className="text-green-400 text-sm ml-2">
                        — Giảm{" "}
                        {promoResult.discountType === "PERCENTAGE"
                          ? `${promoResult.discountValue}%`
                          : formatPrice(promoResult.discountValue ?? 0)}
                      </span>
                    </div>
                    <button
                      onClick={handleRemovePromo}
                      className="text-gray-400 hover:text-white text-sm transition"
                    >
                      Xóa
                    </button>
                  </div>
                ) : (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={promoInput}
                      onChange={(e) => setPromoInput(e.target.value.toUpperCase())}
                      onKeyDown={(e) => e.key === "Enter" && handleApplyPromo()}
                      placeholder="Nhập mã giảm giá"
                      className="flex-1 bg-zinc-700 text-white rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary placeholder-gray-500"
                    />
                    <button
                      onClick={handleApplyPromo}
                      disabled={promoLoading || !promoInput.trim()}
                      className="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-green-600 transition disabled:opacity-50"
                    >
                      {promoLoading ? "..." : "Áp dụng"}
                    </button>
                  </div>
                )}
                {promoError && (
                  <p className="text-red-400 text-sm mt-2">{promoError}</p>
                )}
              </div>

              {/* Payment method */}
              <div className="bg-zinc-800 rounded-lg p-6">
                <h2 className="text-white font-semibold mb-4">Chọn phương thức thanh toán</h2>
                <div className="space-y-2">
                  {Object.entries(PAYMENT_METHODS).map(([value, label]) => (
                    <label
                      key={value}
                      className={`flex items-center p-3 rounded-lg cursor-pointer transition ${
                        paymentMethod === value
                          ? "bg-primary/10 border border-primary"
                          : "bg-zinc-700 border border-transparent hover:border-zinc-600"
                      }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value={value}
                        checked={paymentMethod === value}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                        className="mr-3 accent-primary"
                      />
                      <span className="text-white">{label}</span>
                    </label>
                  ))}
                </div>
              </div>

              {error && (
                <div className="bg-red-500/10 border border-red-500 rounded-lg p-3 text-red-400 text-sm">
                  {error}
                </div>
              )}

              <button
                onClick={handleCheckout}
                disabled={submitting}
                className="w-full py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition font-medium disabled:opacity-50"
              >
                {submitting ? "Đang xử lý..." : "Đặt hàng"}
              </button>
            </div>
          ) : null}
        </main>
      </div>
    </ProtectedRoute>
  );
}
