"use client";

import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useAuth } from "@/contexts/AuthContext";

export default function Home() {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <Dashboard />
      </div>
    </ProtectedRoute>
  );
}

function Dashboard() {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <main className="max-w-7xl mx-auto px-4 py-8">
      <h2 className="text-2xl font-bold text-white mb-6">Welcome back!</h2>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-zinc-800 rounded-lg p-6">
          <h3 className="text-gray-400 text-sm mb-2">Profile</h3>
          <div className="space-y-3">
            <div>
              <span className="text-gray-500 text-xs">Name</span>
              <p className="text-white">{user.fullName}</p>
            </div>
            <div>
              <span className="text-gray-500 text-xs">Email</span>
              <p className="text-white">{user.email}</p>
            </div>
            <div>
              <span className="text-gray-500 text-xs">Phone</span>
              <p className="text-white">{user.phone || "Not set"}</p>
            </div>
            <div>
              <span className="text-gray-500 text-xs">Address</span>
              <p className="text-white">{user.address || "Not set"}</p>
            </div>
          </div>
        </div>

        <div className="bg-zinc-800 rounded-lg p-6">
          <h3 className="text-gray-400 text-sm mb-2">Account Status</h3>
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${user.emailVerified ? "bg-primary" : "bg-yellow-500"}`} />
              <span className="text-white text-sm">
                Email {user.emailVerified ? "Verified" : "Not Verified"}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-primary" />
              <span className="text-white text-sm">Role: {user.role}</span>
            </div>
          </div>
        </div>

        <div className="bg-zinc-800 rounded-lg p-6">
          <h3 className="text-gray-400 text-sm mb-2">Quick Actions</h3>
          <div className="space-y-3">
            <button className="w-full py-2 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm">
              Browse Events
            </button>
            <button className="w-full py-2 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm">
              My Tickets
            </button>
            <button className="w-full py-2 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm">
              Edit Profile
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
