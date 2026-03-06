"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { wishlistService } from "@/lib/wishlist-service";
import { WishlistResponse } from "@/types/wishlist";
import { PageResponse } from "@/types/event";

export default function WishlistPage() {
  const [wishlist, setWishlist] = useState<PageResponse<WishlistResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [removingId, setRemovingId] = useState<number | null>(null);

  const fetchWishlist = async () => {
    setLoading(true);
    try {
      const data = await wishlistService.getMyWishlist(page, 12);
      setWishlist(data);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWishlist();
  }, [page]);

  const handleRemove = async (eventId: number) => {
    setRemovingId(eventId);
    try {
      await wishlistService.removeFromWishlist(eventId);
      await fetchWishlist();
    } catch {
      // ignore
    } finally {
      setRemovingId(null);
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
    });
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-6xl mx-auto px-4 py-8">
          <h1 className="text-2xl font-bold text-white mb-6">My Wishlist</h1>

          {loading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : !wishlist || wishlist.content.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-400 mb-4">Your wishlist is empty</p>
              <Link href="/events" className="text-primary hover:text-green-400 transition">
                Browse Events
              </Link>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {wishlist.content.map((item) => (
                  <div
                    key={item.id}
                    className="bg-zinc-800 rounded-lg overflow-hidden group"
                  >
                    <Link href={`/events/${item.eventId}`}>
                      {item.eventImageUrl ? (
                        <img
                          src={item.eventImageUrl}
                          alt={item.eventTitle}
                          className="w-full h-40 object-cover group-hover:opacity-90 transition"
                        />
                      ) : (
                        <div className="w-full h-40 bg-zinc-700 flex items-center justify-center text-gray-500">
                          No Image
                        </div>
                      )}
                    </Link>
                    <div className="p-4">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="px-2 py-0.5 bg-purple-500/20 text-purple-400 text-xs rounded">
                          {item.eventCategory}
                        </span>
                      </div>
                      <Link
                        href={`/events/${item.eventId}`}
                        className="text-white font-medium hover:text-primary transition block truncate"
                      >
                        {item.eventTitle}
                      </Link>
                      <p className="text-gray-500 text-sm mt-1">
                        {formatDate(item.eventDate)} - {item.eventLocation}
                      </p>
                      <div className="flex items-center justify-between mt-3">
                        <p className="text-primary font-semibold text-sm">
                          From {formatPrice(item.minPrice)}
                        </p>
                        <button
                          onClick={() => handleRemove(item.eventId)}
                          disabled={removingId === item.eventId}
                          className="text-gray-400 hover:text-red-400 transition text-sm disabled:opacity-50"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {wishlist.totalPages > 1 && (
                <div className="mt-6">
                  <Pagination
                    currentPage={wishlist.number}
                    totalPages={wishlist.totalPages}
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
