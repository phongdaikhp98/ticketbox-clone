"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import ImageUpload from "@/components/ImageUpload";
import { useAuth } from "@/contexts/AuthContext";
import { userService } from "@/lib/user-service";
import { UpdateProfileRequest } from "@/types/auth";

export default function ProfilePage() {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <ProfileForm />
      </div>
    </ProtectedRoute>
  );
}

function ProfileForm() {
  const { user, updateUser } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [form, setForm] = useState<UpdateProfileRequest>({
    fullName: user?.fullName || "",
    phone: user?.phone || "",
    address: user?.address || "",
    avatarUrl: user?.avatarUrl || "",
  });

  if (!user) return null;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError("");
    setSuccess("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const updated = await userService.updateProfile(form);
      updateUser(updated);
      setSuccess("Profile updated successfully!");
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setError(error.response?.data?.message || "Failed to update profile");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="max-w-2xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-white">Edit Profile</h2>
        <button
          onClick={() => router.push("/")}
          className="text-gray-400 hover:text-white text-sm transition"
        >
          Back to Dashboard
        </button>
      </div>

      <div className="bg-zinc-800 rounded-lg p-6">
        {/* Avatar display + upload */}
        <div className="flex items-center gap-5 mb-6 pb-6 border-b border-zinc-700">
          <ImageUpload
            folder="avatars"
            aspectRatio="square"
            currentUrl={form.avatarUrl || undefined}
            onUpload={(url) => setForm((prev) => ({ ...prev, avatarUrl: url }))}
          />
          <div>
            <p className="text-white font-medium text-lg">{user.fullName || user.email}</p>
            <p className="text-gray-400 text-sm">{user.email}</p>
            <p className="text-gray-500 text-xs mt-1">Click vào ảnh để thay đổi avatar</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}
          {success && (
            <div className="p-3 bg-green-500/10 border border-green-500/20 rounded-lg text-green-400 text-sm">
              {success}
            </div>
          )}

          <div>
            <label className="block text-gray-400 text-sm mb-1">Full Name</label>
            <input
              type="text"
              name="fullName"
              value={form.fullName}
              onChange={handleChange}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
              maxLength={255}
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Phone</label>
            <input
              type="text"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
              maxLength={20}
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Address</label>
            <textarea
              name="address"
              value={form.address}
              onChange={handleChange}
              rows={3}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary resize-none"
              maxLength={500}
            />
          </div>


          <div className="pt-4">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition disabled:opacity-50"
            >
              {loading ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
