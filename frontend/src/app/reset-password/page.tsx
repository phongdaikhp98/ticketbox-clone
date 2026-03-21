"use client";

import { useState, FormEvent, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { authService } from "@/lib/auth-service";

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") || "";

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");

    if (newPassword !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp");
      return;
    }

    if (!token) {
      setError("Link đặt lại mật khẩu không hợp lệ");
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword(token, newPassword);
      setSuccess(true);
      setTimeout(() => router.push("/login"), 3000);
    } catch (err: any) {
      const msg = err.response?.data?.message || "Có lỗi xảy ra, vui lòng thử lại";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="text-center space-y-4">
        <div className="text-5xl">⚠️</div>
        <p className="text-red-400">Link đặt lại mật khẩu không hợp lệ.</p>
        <Link href="/forgot-password" className="text-primary hover:underline text-sm">
          Yêu cầu link mới
        </Link>
      </div>
    );
  }

  if (success) {
    return (
      <div className="text-center space-y-4">
        <div className="text-5xl">✅</div>
        <h2 className="text-white font-semibold text-lg">Mật khẩu đã được đặt lại!</h2>
        <p className="text-gray-400 text-sm">
          Đang chuyển đến trang đăng nhập...
        </p>
        <Link href="/login" className="inline-block text-primary hover:underline text-sm">
          Đăng nhập ngay
        </Link>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <p className="text-gray-400 text-sm">
        Nhập mật khẩu mới cho tài khoản của bạn.
      </p>

      {error && (
        <div className="bg-red-500/10 border border-red-500 text-red-400 px-4 py-3 rounded text-sm">
          {error}
        </div>
      )}

      <div>
        <label className="block text-gray-300 text-sm mb-2">Mật khẩu mới</label>
        <input
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
          minLength={6}
          className="w-full px-4 py-3 bg-zinc-700 border border-zinc-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
          placeholder="Ít nhất 6 ký tự"
        />
      </div>

      <div>
        <label className="block text-gray-300 text-sm mb-2">Xác nhận mật khẩu mới</label>
        <input
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
          className="w-full px-4 py-3 bg-zinc-700 border border-zinc-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
          placeholder="Nhập lại mật khẩu mới"
        />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full py-3 bg-primary text-white font-semibold rounded-lg hover:bg-green-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {loading ? "Đang cập nhật..." : "Đặt lại mật khẩu"}
      </button>

      <p className="text-center text-gray-400 text-sm">
        <Link href="/login" className="text-primary hover:underline">
          ← Quay lại đăng nhập
        </Link>
      </p>
    </form>
  );
}

export default function ResetPasswordPage() {
  return (
    <div className="min-h-screen bg-secondary flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <Link href="/">
            <h1 className="text-4xl font-bold text-primary">Ticketbox</h1>
          </Link>
          <p className="text-gray-400 mt-2">Đặt lại mật khẩu</p>
        </div>

        <div className="bg-zinc-800 rounded-lg p-8">
          <Suspense fallback={<div className="text-gray-400 text-center">Đang tải...</div>}>
            <ResetPasswordForm />
          </Suspense>
        </div>
      </div>
    </div>
  );
}
