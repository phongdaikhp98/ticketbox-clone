"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import { eventService } from "@/lib/event-service";
import { cartService } from "@/lib/cart-service";
import { wishlistService } from "@/lib/wishlist-service";
import { Event } from "@/types/event";
import { useAuth } from "@/contexts/AuthContext";
import { useCart } from "@/contexts/CartContext";

export default function EventDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const { user } = useAuth();
  const { refreshCart } = useCart();
  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const [addingToCart, setAddingToCart] = useState<number | null>(null);
  const [cartMessage, setCartMessage] = useState<Record<number, string>>({});
  const [wishlisted, setWishlisted] = useState(false);
  const [wishlistLoading, setWishlistLoading] = useState(false);

  useEffect(() => {
    if (!id) return;
    eventService
      .getEventById(id)
      .then(setEvent)
      .catch(() => setError("Event not found"))
      .finally(() => setLoading(false));
  }, [id]);

  const checkWishlistStatus = async () => {
    if (!user || !id) return;
    try {
      const res = await wishlistService.checkWishlist(id);
      setWishlisted(res.wishlisted);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    checkWishlistStatus();
  }, [user, id]);

  const handleQuantityChange = (ticketTypeId: number, value: number, max: number) => {
    const qty = Math.max(1, Math.min(value, max));
    setQuantities((prev) => ({ ...prev, [ticketTypeId]: qty }));
  };

  const handleAddToCart = async (ticketTypeId: number) => {
    if (!user) {
      setCartMessage((prev) => ({ ...prev, [ticketTypeId]: "Please login first" }));
      return;
    }
    setAddingToCart(ticketTypeId);
    setCartMessage((prev) => ({ ...prev, [ticketTypeId]: "" }));
    try {
      const qty = quantities[ticketTypeId] || 1;
      await cartService.addToCart({ ticketTypeId, quantity: qty });
      setCartMessage((prev) => ({ ...prev, [ticketTypeId]: "Added to cart!" }));
      await refreshCart();
      setTimeout(() => {
        setCartMessage((prev) => ({ ...prev, [ticketTypeId]: "" }));
      }, 2000);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message || "Failed to add to cart"
          : "Failed to add to cart";
      setCartMessage((prev) => ({ ...prev, [ticketTypeId]: msg }));
    } finally {
      setAddingToCart(null);
    }
  };

  const handleToggleWishlist = async () => {
    if (!user) return;
    setWishlistLoading(true);
    try {
      if (wishlisted) {
        await wishlistService.removeFromWishlist(id);
      } else {
        await wishlistService.addToWishlist(id);
      }
      await checkWishlistStatus();
    } catch {
      // ignore
    } finally {
      setWishlistLoading(false);
    }
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

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  return (
    <div className="min-h-screen bg-secondary">
      <Header />
      <main className="max-w-4xl mx-auto px-4 py-8">
        <Link href="/events" className="text-gray-400 hover:text-white text-sm transition mb-4 inline-block">
          &larr; Back to Events
        </Link>

        {loading ? (
          <div className="text-center text-gray-400 py-12">Loading...</div>
        ) : error ? (
          <div className="text-center text-red-400 py-12">{error}</div>
        ) : event ? (
          <div className="space-y-6">
            {event.imageUrl && (
              <div className="rounded-lg overflow-hidden h-80 relative">
                <img
                  src={event.imageUrl}
                  alt={event.title}
                  className="w-full h-full object-cover"
                />
                {user && (
                  <button
                    onClick={handleToggleWishlist}
                    disabled={wishlistLoading}
                    className="absolute top-4 right-4 p-2 bg-black/50 rounded-full hover:bg-black/70 transition"
                    title={wishlisted ? "Remove from wishlist" : "Add to wishlist"}
                  >
                    <svg
                      className={`w-6 h-6 ${wishlisted ? "text-red-500 fill-red-500" : "text-white"}`}
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 24 24"
                      fill={wishlisted ? "currentColor" : "none"}
                      stroke="currentColor"
                      strokeWidth={2}
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
                      />
                    </svg>
                  </button>
                )}
              </div>
            )}

            {!event.imageUrl && user && (
              <div className="flex justify-end">
                <button
                  onClick={handleToggleWishlist}
                  disabled={wishlistLoading}
                  className="flex items-center gap-2 px-3 py-1.5 bg-zinc-700 rounded hover:bg-zinc-600 transition text-sm"
                >
                  <svg
                    className={`w-5 h-5 ${wishlisted ? "text-red-500 fill-red-500" : "text-gray-400"}`}
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    fill={wishlisted ? "currentColor" : "none"}
                    stroke="currentColor"
                    strokeWidth={2}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
                    />
                  </svg>
                  <span className="text-gray-300">{wishlisted ? "Saved" : "Save"}</span>
                </button>
              </div>
            )}

            <div className="bg-zinc-800 rounded-lg p-6 space-y-4">
              <div className="flex items-center gap-3 flex-wrap">
                <span className="px-2 py-1 bg-purple-500/20 text-purple-400 text-xs rounded">
                  {event.category}
                </span>
                {event.isFeatured && (
                  <span className="px-2 py-1 bg-primary/20 text-primary text-xs rounded">
                    Featured
                  </span>
                )}
              </div>

              <h1 className="text-3xl font-bold text-white">{event.title}</h1>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-500">Date</span>
                  <p className="text-white">{formatDate(event.eventDate)}</p>
                  {event.endDate && (
                    <p className="text-gray-400">to {formatDate(event.endDate)}</p>
                  )}
                </div>
                <div>
                  <span className="text-gray-500">Location</span>
                  <p className="text-white">{event.location}</p>
                </div>
                <div>
                  <span className="text-gray-500">Organizer</span>
                  <p className="text-white">{event.organizer.fullName}</p>
                </div>
              </div>

              {event.description && (
                <div>
                  <h3 className="text-gray-400 text-sm mb-2">Description</h3>
                  <p className="text-gray-300 whitespace-pre-line">{event.description}</p>
                </div>
              )}
            </div>

            <div className="bg-zinc-800 rounded-lg p-6">
              <h3 className="text-white font-semibold mb-4">Tickets</h3>
              <div className="space-y-3">
                {event.ticketTypes.map((tt) => (
                  <div
                    key={tt.id}
                    className="flex items-center justify-between p-4 bg-zinc-700 rounded-lg"
                  >
                    <div>
                      <p className="text-white font-medium">{tt.name}</p>
                      <p className="text-gray-400 text-sm">
                        {tt.availableCount} / {tt.capacity} available
                      </p>
                    </div>
                    <div className="text-right flex items-center gap-3">
                      <p className="text-primary font-semibold">
                        {tt.price > 0 ? formatPrice(tt.price) : "Free"}
                      </p>
                      {tt.availableCount > 0 ? (
                        <div className="flex items-center gap-2">
                          <div className="flex items-center border border-zinc-600 rounded">
                            <button
                              onClick={() =>
                                handleQuantityChange(tt.id, (quantities[tt.id] || 1) - 1, tt.availableCount)
                              }
                              className="px-2 py-1 text-gray-400 hover:text-white transition"
                            >
                              -
                            </button>
                            <span className="px-2 py-1 text-white text-sm min-w-[2rem] text-center">
                              {quantities[tt.id] || 1}
                            </span>
                            <button
                              onClick={() =>
                                handleQuantityChange(tt.id, (quantities[tt.id] || 1) + 1, tt.availableCount)
                              }
                              className="px-2 py-1 text-gray-400 hover:text-white transition"
                            >
                              +
                            </button>
                          </div>
                          <button
                            onClick={() => handleAddToCart(tt.id)}
                            disabled={addingToCart === tt.id}
                            className="px-4 py-1.5 bg-primary text-white text-sm rounded hover:bg-green-600 transition disabled:opacity-50"
                          >
                            {addingToCart === tt.id ? "..." : "Add to Cart"}
                          </button>
                        </div>
                      ) : (
                        <span className="px-4 py-1.5 bg-zinc-600 text-gray-400 text-sm rounded">
                          Sold Out
                        </span>
                      )}
                    </div>
                    {cartMessage[tt.id] && (
                      <p
                        className={`text-xs mt-1 ${
                          cartMessage[tt.id] === "Added to cart!"
                            ? "text-green-400"
                            : "text-red-400"
                        }`}
                      >
                        {cartMessage[tt.id]}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
}
