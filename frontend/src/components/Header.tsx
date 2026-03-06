"use client";

import Link from "next/link";
import { useAuth } from "@/contexts/AuthContext";
import { useCart } from "@/contexts/CartContext";
import { useRouter } from "next/navigation";

export default function Header() {
  const { user, logout } = useAuth();
  const { cartCount } = useCart();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  return (
    <header className="bg-zinc-800 border-b border-zinc-700">
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
              <Link href="/tickets" className="text-gray-300 hover:text-white text-sm transition">
                Vé của tôi
              </Link>
              <Link href="/wishlist" className="text-gray-300 hover:text-white text-sm transition">
                Yêu thích
              </Link>
              <Link href="/orders" className="text-gray-300 hover:text-white text-sm transition">
                Đơn hàng của tôi
              </Link>
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
    </header>
  );
}
