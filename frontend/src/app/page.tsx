"use client";

import Link from "next/link";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import ProtectedRoute from "@/components/ProtectedRoute";
import FeaturedEventsCarousel from "@/components/FeaturedEventsCarousel";
import { useAuth } from "@/contexts/AuthContext";

export default function Home() {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary flex flex-col">
        <Header />
        <main className="flex-1">
          <Dashboard />
        </main>
        <Footer />
      </div>
    </ProtectedRoute>
  );
}

function Dashboard() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Featured Events */}
      <FeaturedEventsCarousel />

      {/* Welcome + Quick actions */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Profile card */}
        <div className="bg-zinc-800 rounded-lg p-6">
          <h3 className="text-gray-400 text-sm mb-3">Tài khoản</h3>
          <div className="space-y-2">
            <p className="text-white font-medium">{user.fullName}</p>
            <p className="text-gray-400 text-sm">{user.email}</p>
            {user.phone && <p className="text-gray-500 text-sm">{user.phone}</p>}
            <Link
              href="/profile"
              className="inline-block mt-2 text-xs text-primary hover:underline"
            >
              Chỉnh sửa hồ sơ →
            </Link>
          </div>
        </div>

        {/* Quick actions */}
        <div className="bg-zinc-800 rounded-lg p-6">
          <h3 className="text-gray-400 text-sm mb-3">Truy cập nhanh</h3>
          <div className="space-y-2">
            <Link
              href="/events"
              className="flex items-center gap-2 w-full py-2 px-3 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
            >
              🎫 Khám phá sự kiện
            </Link>
            <Link
              href="/tickets"
              className="flex items-center gap-2 w-full py-2 px-3 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
            >
              🎟 Vé của tôi
            </Link>
            <Link
              href="/orders"
              className="flex items-center gap-2 w-full py-2 px-3 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
            >
              📦 Đơn hàng của tôi
            </Link>
            <Link
              href="/wishlist"
              className="flex items-center gap-2 w-full py-2 px-3 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
            >
              ❤️ Yêu thích
            </Link>
          </div>
        </div>

        {/* Status */}
        <div className="bg-zinc-800 rounded-lg p-6">
          <h3 className="text-gray-400 text-sm mb-3">Trạng thái</h3>
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <span
                className={`w-2 h-2 rounded-full ${
                  user.emailVerified ? "bg-green-400" : "bg-yellow-400"
                }`}
              />
              <span className="text-white text-sm">
                Email {user.emailVerified ? "đã xác thực" : "chưa xác thực"}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-primary" />
              <span className="text-white text-sm">
                Vai trò:{" "}
                <span className="text-primary">
                  {user.role === "ADMIN"
                    ? "Quản trị"
                    : user.role === "ORGANIZER"
                    ? "Tổ chức"
                    : "Khách hàng"}
                </span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
