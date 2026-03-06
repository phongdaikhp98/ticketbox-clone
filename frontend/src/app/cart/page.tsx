"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { cartService } from "@/lib/cart-service";
import { CartResponse } from "@/types/cart";
import { useCart } from "@/contexts/CartContext";

export default function CartPage() {
  const router = useRouter();
  const { refreshCart } = useCart();
  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [error, setError] = useState("");

  const fetchCart = async () => {
    try {
      const data = await cartService.getCart();
      setCart(data);
      setError("");
    } catch {
      setError("Failed to load cart. Please refresh the page.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCart();
  }, []);

  const handleUpdateQuantity = async (itemId: number, quantity: number) => {
    if (updatingId) return;
    setUpdatingId(itemId);
    setError("");
    try {
      await cartService.updateCartItem(itemId, { quantity });
      await fetchCart();
      refreshCart();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data
              ?.message || "Failed to update"
          : "Failed to update. Please try again.";
      setError(msg);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (updatingId) return;
    setUpdatingId(itemId);
    setError("");
    try {
      await cartService.removeCartItem(itemId);
      await fetchCart();
      refreshCart();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data
              ?.message || "Failed to remove"
          : "Failed to remove. Please try again.";
      setError(msg);
    } finally {
      setUpdatingId(null);
    }
  };

  const handleClearCart = async () => {
    if (updatingId) return;
    setError("");
    try {
      await cartService.clearCart();
      await fetchCart();
      refreshCart();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data
              ?.message || "Failed to clear cart"
          : "Failed to clear cart. Please try again.";
      setError(msg);
    }
  };

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
          <h1 className="text-2xl font-bold text-white mb-6">Shopping Cart</h1>

          {error && (
            <div className="bg-red-500/10 border border-red-500 rounded-lg p-3 text-red-400 text-sm mb-4">
              {error}
            </div>
          )}

          {loading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : !cart || cart.items.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-400 mb-4">Your cart is empty</p>
              <Link
                href="/events"
                className="text-primary hover:text-green-400 transition"
              >
                Browse Events
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {cart.items.map((item) => (
                <div
                  key={item.id}
                  className="bg-zinc-800 rounded-lg p-4 flex items-center gap-4"
                >
                  <Link href={`/events/${item.event.id}`} className="shrink-0">
                    {item.event.imageUrl ? (
                      <img
                        src={item.event.imageUrl}
                        alt={item.event.title}
                        className="w-20 h-20 object-cover rounded"
                      />
                    ) : (
                      <div className="w-20 h-20 bg-zinc-700 rounded flex items-center justify-center text-gray-500 text-xs">
                        No Image
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
                    <p className="text-gray-400 text-sm">{item.ticketType.name}</p>
                    <p className="text-gray-500 text-xs">
                      {formatDate(item.event.eventDate)} - {item.event.location}
                    </p>
                    <p className="text-primary text-sm font-medium mt-1">
                      {formatPrice(item.ticketType.price)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex items-center border border-zinc-600 rounded">
                      <button
                        onClick={() =>
                          handleUpdateQuantity(item.id, Math.max(1, item.quantity - 1))
                        }
                        disabled={updatingId === item.id || item.quantity <= 1}
                        className="px-2 py-1 text-gray-400 hover:text-white transition disabled:opacity-50"
                      >
                        -
                      </button>
                      <span className="px-3 py-1 text-white text-sm">
                        {item.quantity}
                      </span>
                      <button
                        onClick={() =>
                          handleUpdateQuantity(item.id, item.quantity + 1)
                        }
                        disabled={
                          updatingId === item.id ||
                          item.quantity >= item.ticketType.availableCount
                        }
                        className="px-2 py-1 text-gray-400 hover:text-white transition disabled:opacity-50"
                      >
                        +
                      </button>
                    </div>
                    <p className="text-white font-semibold min-w-[100px] text-right">
                      {formatPrice(item.ticketType.price * item.quantity)}
                    </p>
                    <button
                      onClick={() => handleRemoveItem(item.id)}
                      disabled={updatingId === item.id}
                      className="text-gray-400 hover:text-red-400 transition p-1"
                      title="Remove"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </div>
              ))}

              <div className="bg-zinc-800 rounded-lg p-6 mt-6">
                <div className="flex justify-between items-center mb-4">
                  <span className="text-gray-400">
                    Total ({cart.totalItems} items)
                  </span>
                  <span className="text-2xl font-bold text-primary">
                    {formatPrice(cart.totalAmount)}
                  </span>
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={handleClearCart}
                    className="px-4 py-2 border border-zinc-600 text-gray-400 rounded hover:border-red-500 hover:text-red-400 transition"
                  >
                    Clear Cart
                  </button>
                  <button
                    onClick={() => router.push("/checkout")}
                    className="flex-1 px-6 py-2 bg-primary text-white rounded hover:bg-green-600 transition font-medium"
                  >
                    Proceed to Checkout
                  </button>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
