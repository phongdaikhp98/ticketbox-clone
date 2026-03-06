"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { cartService } from "@/lib/cart-service";
import { orderService } from "@/lib/order-service";
import { CartResponse } from "@/types/cart";
import { PAYMENT_METHODS } from "@/types/order";
import { useCart } from "@/contexts/CartContext";

export default function CheckoutPage() {
  const router = useRouter();
  const { refreshCart } = useCart();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState("E_WALLET");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

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

  const handleCheckout = async () => {
    setSubmitting(true);
    setError("");
    try {
      const order = await orderService.checkout({ paymentMethod });
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

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-2xl mx-auto px-4 py-8">
          <h1 className="text-2xl font-bold text-white mb-6">Checkout</h1>

          {loading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : cart ? (
            <div className="space-y-6">
              <div className="bg-zinc-800 rounded-lg p-6">
                <h2 className="text-white font-semibold mb-4">Order Summary</h2>
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
                <div className="border-t border-zinc-700 mt-4 pt-4 flex justify-between">
                  <span className="text-gray-400 font-medium">Total</span>
                  <span className="text-xl font-bold text-primary">
                    {formatPrice(cart.totalAmount)}
                  </span>
                </div>
              </div>

              <div className="bg-zinc-800 rounded-lg p-6">
                <h2 className="text-white font-semibold mb-4">Payment Method</h2>
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
                {submitting ? "Processing..." : "Place Order"}
              </button>
            </div>
          ) : null}
        </main>
      </div>
    </ProtectedRoute>
  );
}
