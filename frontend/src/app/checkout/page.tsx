"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { cartService } from "@/lib/cart-service";
import { orderService } from "@/lib/order-service";
import { CartResponse } from "@/types/cart";
import { ValidatePromoCodeResponse } from "@/types/order";
import { useCart } from "@/contexts/CartContext";

const PAYMENT_OPTIONS = [
  {
    id: "E_WALLET",
    label: "VNPay",
    sublabel: "Thanh toán qua cổng VNPay",
    available: true,
    badge: { text: "VNPay", color: "bg-blue-600" },
  },
  {
    id: "MOMO",
    label: "MoMo",
    sublabel: "Ví điện tử MoMo",
    available: false,
    badge: { text: "MoMo", color: "bg-pink-600" },
  },
  {
    id: "ZALOPAY",
    label: "ZaloPay",
    sublabel: "Ví điện tử ZaloPay",
    available: false,
    badge: { text: "ZaloPay", color: "bg-sky-500" },
  },
  {
    id: "BANKING_QR",
    label: "QR Banking",
    sublabel: "Quét mã QR chuyển khoản ngân hàng",
    available: false,
    badge: { text: "QR", color: "bg-orange-500" },
  },
];

export default function CheckoutPage() {
  const router = useRouter();
  const { refreshCart } = useCart();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState("E_WALLET");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [comingSoonToast, setComingSoonToast] = useState(false);

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

  const showComingSoon = () => {
    setComingSoonToast(true);
    setTimeout(() => setComingSoonToast(false), 3000);
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
                  {PAYMENT_OPTIONS.map((option) => (
                    <div
                      key={option.id}
                      onClick={() => {
                        if (option.available) {
                          setPaymentMethod(option.id);
                        } else {
                          showComingSoon();
                        }
                      }}
                      className={`flex items-center justify-between p-3 rounded-lg cursor-pointer transition ${
                        !option.available
                          ? "bg-zinc-700/50 border border-transparent opacity-60 hover:opacity-80"
                          : paymentMethod === option.id
                          ? "bg-primary/10 border border-primary"
                          : "bg-zinc-700 border border-transparent hover:border-zinc-600"
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center flex-shrink-0 ${
                          option.available && paymentMethod === option.id
                            ? "border-primary"
                            : "border-zinc-500"
                        }`}>
                          {option.available && paymentMethod === option.id && (
                            <div className="w-2.5 h-2.5 rounded-full bg-primary" />
                          )}
                        </div>
                        <span className={`min-w-[4rem] text-center text-xs font-bold text-white rounded px-2 py-0.5 shrink-0 ${option.badge.color}`}>
                          {option.badge.text}
                        </span>
                        <div>
                          <p className="text-white text-sm font-medium">{option.label}</p>
                          <p className="text-gray-400 text-xs">{option.sublabel}</p>
                        </div>
                      </div>
                      {!option.available && (
                        <span className="text-xs text-yellow-500 border border-yellow-500/40 rounded px-2 py-0.5 flex-shrink-0">
                          Sắp ra mắt
                        </span>
                      )}
                    </div>
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
                disabled={submitting || paymentMethod !== "E_WALLET"}
                className="w-full py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition font-medium disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? "Đang xử lý..." : "Đặt hàng"}
              </button>
            </div>
          ) : null}
        </main>
      </div>

      {/* Coming soon toast */}
      {comingSoonToast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 bg-zinc-800 border border-yellow-500/40 text-white px-5 py-3 rounded-xl shadow-lg animate-fade-in">
          <span className="text-yellow-400 text-lg">🚧</span>
          <div>
            <p className="font-medium text-sm">Chức năng đang phát triển</p>
            <p className="text-xs text-gray-400">Vui lòng sử dụng VNPay để thanh toán</p>
          </div>
        </div>
      )}
    </ProtectedRoute>
  );
}
