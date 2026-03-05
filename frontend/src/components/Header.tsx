"use client";

import Link from "next/link";
import { useAuth } from "@/contexts/AuthContext";
import { useRouter } from "next/navigation";

export default function Header() {
  const { user, logout } = useAuth();
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
            Events
          </Link>
          {user ? (
            <>
              {(user.role === "ORGANIZER" || user.role === "ADMIN") && (
                <>
                  <Link href="/events/my-events" className="text-gray-300 hover:text-white text-sm transition">
                    My Events
                  </Link>
                  <Link href="/events/create" className="text-gray-300 hover:text-white text-sm transition">
                    Create Event
                  </Link>
                </>
              )}
              <span className="text-gray-300 text-sm">
                Hi, <span className="text-primary font-medium">{user.fullName}</span>
              </span>
              <span className="text-zinc-500 text-xs px-2 py-1 bg-zinc-700 rounded">
                {user.role}
              </span>
              <button
                onClick={handleLogout}
                className="text-gray-400 hover:text-white text-sm transition"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="text-gray-300 hover:text-white text-sm transition">
                Sign In
              </Link>
              <Link
                href="/register"
                className="px-4 py-2 bg-primary text-white text-sm rounded-lg hover:bg-green-600 transition"
              >
                Sign Up
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
