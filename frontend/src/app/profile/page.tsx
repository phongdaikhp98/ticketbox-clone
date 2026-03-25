"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import ImageUpload from "@/components/ImageUpload";
import { useAuth } from "@/contexts/AuthContext";
import { userService } from "@/lib/user-service";
import { authService } from "@/lib/auth-service";
import { ChangePasswordRequest, UpdateProfileRequest } from "@/types/auth";

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
        <h2 className="text-2xl font-bold text-white">Chỉnh sửa hồ sơ</h2>
        <button
          onClick={() => router.push("/")}
          className="text-gray-400 hover:text-white text-sm transition"
        >
          ← Về trang chủ
        </button>
      </div>

      <div className="bg-zinc-800 rounded-lg p-6">
        {/* Avatar display + upload */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-4 mb-6 pb-6 border-b border-zinc-700">
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
              {loading ? "Đang lưu..." : "Lưu thay đổi"}
            </button>
          </div>
        </form>
      </div>

      {/* Email Verification — chỉ hiện với tài khoản LOCAL chưa xác thực */}
      {!user.emailVerified && user.role !== "OAUTH2" && <EmailVerificationSection />}

      {/* Change Password — chỉ hiện với tài khoản đăng ký email */}
      {user.role !== "OAUTH2" && <ChangePasswordSection />}
    </main>
  );
}

function EmailVerificationSection() {
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");

  const handleSend = async () => {
    setLoading(true);
    setError("");
    try {
      await authService.sendVerificationEmail();
      setSent(true);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || "Không thể gửi email, vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-5 mt-6">
      <div className="flex items-start gap-3">
        <span className="text-yellow-400 text-xl mt-0.5">⚠️</span>
        <div className="flex-1 min-w-0">
          <h3 className="text-yellow-300 font-semibold text-sm">Email chưa được xác thực</h3>
          <p className="text-yellow-200/70 text-xs mt-1">
            Xác thực email để bảo vệ tài khoản và nhận thông báo quan trọng.
          </p>

          {sent ? (
            <p className="text-green-400 text-xs mt-2">
              ✅ Email xác thực đã được gửi! Kiểm tra hộp thư (kể cả Spam).
            </p>
          ) : (
            <>
              {error && <p className="text-red-400 text-xs mt-2">{error}</p>}
              <button
                onClick={handleSend}
                disabled={loading}
                className="mt-3 px-4 py-1.5 bg-yellow-500 hover:bg-yellow-400 text-black text-xs font-semibold rounded-lg transition disabled:opacity-50"
              >
                {loading ? "Đang gửi..." : "Gửi email xác thực"}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function ChangePasswordSection() {
  const [form, setForm] = useState<ChangePasswordRequest>({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showForm, setShowForm] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError("");
    setSuccess("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (form.newPassword !== form.confirmPassword) {
      setError("Mật khẩu mới và xác nhận không khớp");
      return;
    }
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      await userService.changePassword(form);
      setSuccess("Đổi mật khẩu thành công!");
      setForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setShowForm(false);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || "Đổi mật khẩu thất bại");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-zinc-800 rounded-lg p-6 mt-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-white font-semibold">Đổi mật khẩu</h3>
          <p className="text-gray-400 text-sm mt-0.5">Cập nhật mật khẩu đăng nhập của bạn</p>
        </div>
        <button
          onClick={() => { setShowForm((v) => !v); setError(""); setSuccess(""); }}
          className="text-sm px-4 py-1.5 rounded-lg bg-zinc-700 text-gray-300 hover:bg-zinc-600 transition"
        >
          {showForm ? "Hủy" : "Thay đổi"}
        </button>
      </div>

      {success && !showForm && (
        <div className="mt-3 p-3 bg-green-500/10 border border-green-500/20 rounded-lg text-green-400 text-sm">
          {success}
        </div>
      )}

      {showForm && (
        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {error && (
            <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}

          <div>
            <label className="block text-gray-400 text-sm mb-1">Mật khẩu hiện tại</label>
            <input
              type="password"
              name="currentPassword"
              value={form.currentPassword}
              onChange={handleChange}
              required
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Mật khẩu mới</label>
            <input
              type="password"
              name="newPassword"
              value={form.newPassword}
              onChange={handleChange}
              required
              minLength={6}
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Xác nhận mật khẩu mới</label>
            <input
              type="password"
              name="confirmPassword"
              value={form.confirmPassword}
              onChange={handleChange}
              required
              className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition disabled:opacity-50"
          >
            {loading ? "Đang lưu..." : "Xác nhận đổi mật khẩu"}
          </button>
        </form>
      )}
    </div>
  );
}
