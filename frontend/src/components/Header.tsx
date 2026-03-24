"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useCart } from "@/contexts/CartContext";
import { useRouter } from "next/navigation";

export default function Header() {
  const { user, logout } = useAuth();
  const { cartCount } = useCart();
  const router = useRouter();
  const [adminDropdownOpen, setAdminDropdownOpen] = useState(false);

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <header className="bg-zinc-800">
      <div className="border-b border-zinc-700">
        {/* Row 1: Logo + Sự kiện + (Vé của tôi | Yêu thích | Đơn hàng của tôi) + Cart + User Info */}
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <Link href="/" className="text-2xl font-bold text-primary">
            Ticketbox
          </Link>

          <nav className="flex items-center gap-6">
            <Link href="/events" className="text-gray-300 hover:text-white text-sm transition">
              Sự kiện
            </Link>
            {user ? (
              <>
                <Link href="/tickets" className="text-gray-300 hover:text-white text-sm transition">
                  Vé của tôi
                </Link>
                <Link href="/wishlist" className="text-gray-300 hover:text-white text-sm transition">
                  Yêu thích
                </Link>
                <Link href="/orders" className="text-gray-300 hover:text-white text-sm transition">
                  Đơn hàng của tôi
                </Link>
                {user.role === "CUSTOMER" && (
                  <Link href="/organizer-application" className="text-gray-300 hover:text-white text-sm transition">
                    Đăng ký Organizer
                  </Link>
                )}
                <Link href="/cart" className="relative text-gray-300 hover:text-white transition">
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z" />
                  </svg>
                  {cartCount > 0 && (
                    <span className="absolute -top-2 -right-2 bg-primary text-white text-xs w-5 h-5 rounded-full flex items-center justify-center font-medium">
                      {cartCount > 99 ? "99+" : cartCount}
                    </span>
                  )}
                </Link>
                <span className="text-gray-300 text-sm">
                  Xin chào, <span className="text-primary font-medium">{user.fullName}</span>
                </span>
                <span className="text-zinc-500 text-xs px-2 py-1 bg-zinc-700 rounded">
                  {user.role === "ORGANIZER" ? "Tổ chức" : user.role === "ADMIN" ? "Quản trị" : "Khách"}
                </span>
                <button
                  onClick={handleLogout}
                  className="text-gray-400 hover:text-white text-sm transition"
                >
                  Đăng xuất
                </button>
              </>
            ) : (
              <>
                <Link href="/login" className="text-gray-300 hover:text-white text-sm transition">
                  Đăng nhập
                </Link>
                <Link
                  href="/register"
                  className="px-4 py-2 bg-primary text-white text-sm rounded-lg hover:bg-green-600 transition"
                >
                  Đăng ký
                </Link>
              </>
            )}
          </nav>
        </div>

        {/* Row 2: Organizer + Admin Links */}
        {user && (
          <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-end gap-6">
            {(user.role === "ORGANIZER" || user.role === "ADMIN") && (
              <>
                <Link href="/organizer/dashboard" className="text-gray-300 hover:text-white text-sm transition">
                  Dashboard
                </Link>
                <Link href="/events/my-events" className="text-gray-300 hover:text-white text-sm transition">
                  Sự kiện của tôi
                </Link>
                <Link href="/events/create" className="text-gray-300 hover:text-white text-sm transition">
                  Tạo sự kiện
                </Link>
                <Link href="/organizer/check-in" className="text-gray-300 hover:text-white text-sm transition">
                  Kiểm tra vé
                </Link>
              </>
            )}
            {user.role === "ADMIN" && (
              <>
                <span className="text-zinc-600">|</span>
                <div className="relative">
                  <button
                    onClick={() => setAdminDropdownOpen(!adminDropdownOpen)}
                    className="text-gray-300 hover:text-white text-sm transition flex items-center gap-2 px-3 py-2 rounded hover:bg-zinc-700"
                  >
                    Quản lý
                    <svg
                      className={`w-4 h-4 transition ${adminDropdownOpen ? "rotate-180" : ""}`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                    </svg>
                  </button>
                  {adminDropdownOpen && (
                    <div className="absolute top-full left-0 mt-2 w-48 bg-zinc-700 border border-zinc-600 rounded-lg shadow-xl z-50">
                      <Link
                        href="/admin/dashboard"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition first:rounded-t-lg"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        📊 Dashboard
                      </Link>
                      <Link
                        href="/admin/users"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        👥 Quản lý người dùng
                      </Link>
                      <Link
                        href="/admin/orders"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        📦 Quản lý đơn hàng
                      </Link>
                      <Link
                        href="/admin/events"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        🎭 Quản lý sự kiện
                      </Link>
                      <Link
                        href="/admin/categories"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        🗂️ Danh mục
                      </Link>
                      <Link
                        href="/admin/organizer-applications"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        📝 Đơn Organizer
                      </Link>
                      <Link
                        href="/admin/refunds"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        💸 Hoàn tiền
                      </Link>
                      <Link
                        href="/admin/audit-logs"
                        className="block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition last:rounded-b-lg"
                        onClick={() => setAdminDropdownOpen(false)}
                      >
                        📋 Audit Log
                      </Link>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </header>
  );
}
