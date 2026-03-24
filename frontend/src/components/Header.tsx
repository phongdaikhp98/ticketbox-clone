"use client";

import Link from "next/link";
import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useCart } from "@/contexts/CartContext";
import { useRouter } from "next/navigation";

const ADMIN_LINKS = [
  { href: "/admin/dashboard",              label: "📊 Dashboard" },
  { href: "/admin/users",                  label: "👥 Quản lý người dùng" },
  { href: "/admin/orders",                 label: "📦 Quản lý đơn hàng" },
  { href: "/admin/events",                 label: "🎭 Quản lý sự kiện" },
  { href: "/admin/categories",             label: "🗂️ Danh mục" },
  { href: "/admin/organizer-applications", label: "📝 Đơn Organizer" },
  { href: "/admin/refunds",                label: "💸 Hoàn tiền" },
  { href: "/admin/audit-logs",             label: "📋 Audit Log" },
];

function CartIcon({ count }: { count: number }) {
  return (
    <Link href="/cart" className="relative text-gray-300 hover:text-white transition shrink-0">
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z" />
      </svg>
      {count > 0 && (
        <span className="absolute -top-2 -right-2 bg-primary text-white text-xs w-5 h-5 rounded-full flex items-center justify-center font-medium">
          {count > 99 ? "99+" : count}
        </span>
      )}
    </Link>
  );
}

function MobileLink({ href, onClick, children }: { href: string; onClick: () => void; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className="block px-3 py-2.5 text-gray-300 hover:text-white hover:bg-zinc-700 rounded-lg text-sm transition"
    >
      {children}
    </Link>
  );
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div className="pt-2 pb-1 px-3">
      <p className="text-gray-500 text-xs font-semibold uppercase tracking-wider">{children}</p>
    </div>
  );
}

export default function Header() {
  const { user, logout } = useAuth();
  const { cartCount } = useCart();
  const router = useRouter();
  const [adminDropdownOpen, setAdminDropdownOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    router.push("/login");
    setMobileMenuOpen(false);
  };

  const closeMobile = () => setMobileMenuOpen(false);
  const isOrganizerOrAdmin = user?.role === "ORGANIZER" || user?.role === "ADMIN";

  return (
    <header className="bg-zinc-800">
      <div className="border-b border-zinc-700">

        {/* ── Top bar (logo + desktop nav + mobile controls) ── */}
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between gap-4">
          <Link href="/" className="text-xl sm:text-2xl font-bold text-primary shrink-0">
            Ticketbox
          </Link>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-5 flex-1 justify-end">
            <Link href="/events" className="text-gray-300 hover:text-white text-sm transition">
              Sự kiện
            </Link>
            {user ? (
              <>
                <Link href="/tickets"  className="text-gray-300 hover:text-white text-sm transition">Vé của tôi</Link>
                <Link href="/wishlist" className="text-gray-300 hover:text-white text-sm transition">Yêu thích</Link>
                <Link href="/orders"   className="text-gray-300 hover:text-white text-sm transition">Đơn hàng</Link>
                {user.role === "CUSTOMER" && (
                  <Link href="/organizer-application" className="text-gray-300 hover:text-white text-sm transition">
                    Đăng ký Organizer
                  </Link>
                )}
                <CartIcon count={cartCount} />
                <span className="text-gray-300 text-sm whitespace-nowrap">
                  Xin chào, <span className="text-primary font-medium">{user.fullName}</span>
                </span>
                <span className="text-zinc-500 text-xs px-2 py-1 bg-zinc-700 rounded whitespace-nowrap">
                  {user.role === "ORGANIZER" ? "Tổ chức" : user.role === "ADMIN" ? "Quản trị" : "Khách"}
                </span>
                <button onClick={handleLogout} className="text-gray-400 hover:text-white text-sm transition whitespace-nowrap">
                  Đăng xuất
                </button>
              </>
            ) : (
              <>
                <Link href="/login" className="text-gray-300 hover:text-white text-sm transition">Đăng nhập</Link>
                <Link href="/register" className="px-4 py-2 bg-primary text-white text-sm rounded-lg hover:bg-green-600 transition whitespace-nowrap">
                  Đăng ký
                </Link>
              </>
            )}
          </nav>

          {/* Mobile: cart + hamburger */}
          <div className="flex md:hidden items-center gap-2">
            {user && <CartIcon count={cartCount} />}
            <button
              onClick={() => setMobileMenuOpen((v) => !v)}
              className="p-2 text-gray-300 hover:text-white transition"
              aria-label="Menu"
            >
              {mobileMenuOpen ? (
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              ) : (
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              )}
            </button>
          </div>
        </div>

        {/* ── Desktop Row 2: Organizer / Admin ── */}
        {isOrganizerOrAdmin && (
          <div className="hidden md:flex max-w-7xl mx-auto px-4 h-12 items-center justify-end gap-6 border-t border-zinc-700/40">
            <Link href="/organizer/dashboard" className="text-gray-300 hover:text-white text-sm transition">Dashboard</Link>
            <Link href="/events/my-events"    className="text-gray-300 hover:text-white text-sm transition">Sự kiện của tôi</Link>
            <Link href="/events/create"       className="text-gray-300 hover:text-white text-sm transition">Tạo sự kiện</Link>
            <Link href="/organizer/check-in"  className="text-gray-300 hover:text-white text-sm transition">Kiểm tra vé</Link>

            {user?.role === "ADMIN" && (
              <>
                <span className="text-zinc-600">|</span>
                <div className="relative">
                  <button
                    onClick={() => setAdminDropdownOpen((v) => !v)}
                    className="text-gray-300 hover:text-white text-sm transition flex items-center gap-2 px-3 py-2 rounded hover:bg-zinc-700"
                  >
                    Quản lý
                    <svg className={`w-4 h-4 transition-transform ${adminDropdownOpen ? "rotate-180" : ""}`}
                      fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
                  {adminDropdownOpen && (
                    <div className="absolute top-full right-0 mt-1 w-52 bg-zinc-700 border border-zinc-600 rounded-lg shadow-xl z-50">
                      {ADMIN_LINKS.map(({ href, label }, i, arr) => (
                        <Link
                          key={href}
                          href={href}
                          onClick={() => setAdminDropdownOpen(false)}
                          className={`block px-4 py-2 text-gray-300 hover:text-white hover:bg-zinc-600 text-sm transition
                            ${i === 0 ? "rounded-t-lg" : ""}
                            ${i === arr.length - 1 ? "rounded-b-lg" : ""}`}
                        >
                          {label}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        )}
      </div>

      {/* ── Mobile Menu Drawer ── */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-zinc-800 border-b border-zinc-700 shadow-2xl z-40">
          <nav className="max-w-7xl mx-auto px-4 py-3 space-y-0.5">

            {/* User info */}
            {user && (
              <div className="flex items-center gap-3 px-3 py-3 mb-2 border-b border-zinc-700">
                <div className="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm shrink-0">
                  {user.fullName?.[0]?.toUpperCase() ?? "U"}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-white text-sm font-medium truncate">{user.fullName}</p>
                  <p className="text-gray-400 text-xs truncate">{user.email}</p>
                </div>
                <span className="text-zinc-400 text-xs px-2 py-1 bg-zinc-700 rounded shrink-0">
                  {user.role === "ORGANIZER" ? "Tổ chức" : user.role === "ADMIN" ? "Quản trị" : "Khách"}
                </span>
              </div>
            )}

            {/* Main links */}
            <MobileLink href="/events" onClick={closeMobile}>🎫 Sự kiện</MobileLink>

            {user ? (
              <>
                <MobileLink href="/tickets"  onClick={closeMobile}>🎟️ Vé của tôi</MobileLink>
                <MobileLink href="/wishlist" onClick={closeMobile}>❤️ Yêu thích</MobileLink>
                <MobileLink href="/orders"   onClick={closeMobile}>📦 Đơn hàng của tôi</MobileLink>
                <MobileLink href="/profile"  onClick={closeMobile}>👤 Hồ sơ cá nhân</MobileLink>
                {user.role === "CUSTOMER" && (
                  <MobileLink href="/organizer-application" onClick={closeMobile}>📋 Đăng ký Organizer</MobileLink>
                )}

                {/* Organizer section */}
                {isOrganizerOrAdmin && (
                  <>
                    <SectionLabel>Organizer</SectionLabel>
                    <MobileLink href="/organizer/dashboard" onClick={closeMobile}>📊 Dashboard</MobileLink>
                    <MobileLink href="/events/my-events"    onClick={closeMobile}>📅 Sự kiện của tôi</MobileLink>
                    <MobileLink href="/events/create"       onClick={closeMobile}>➕ Tạo sự kiện</MobileLink>
                    <MobileLink href="/organizer/check-in"  onClick={closeMobile}>✅ Kiểm tra vé</MobileLink>
                  </>
                )}

                {/* Admin section */}
                {user.role === "ADMIN" && (
                  <>
                    <SectionLabel>Admin</SectionLabel>
                    {ADMIN_LINKS.map(({ href, label }) => (
                      <MobileLink key={href} href={href} onClick={closeMobile}>{label}</MobileLink>
                    ))}
                  </>
                )}

                {/* Logout */}
                <div className="pt-2 mt-1 border-t border-zinc-700">
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-3 py-2.5 text-red-400 hover:bg-zinc-700 rounded-lg text-sm transition"
                  >
                    🚪 Đăng xuất
                  </button>
                </div>
              </>
            ) : (
              <>
                <MobileLink href="/login"    onClick={closeMobile}>🔑 Đăng nhập</MobileLink>
                <MobileLink href="/register" onClick={closeMobile}>📝 Đăng ký</MobileLink>
              </>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}
