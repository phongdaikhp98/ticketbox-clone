"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { authService } from "@/lib/auth-service";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await authService.forgotPassword(email);
      setSent(true);
    } catch (err: any) {
      const msg = err.response?.data?.message || "Có lỗi xảy ra, vui lòng thử lại";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-secondary flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <Link href="/">
            <h1 className="text-4xl font-bold text-primary">Ticketbox</h1>
          </Link>
          <p className="text-gray-400 mt-2">Quên mật khẩu</p>
        </div>

        <div className="bg-zinc-800 rounded-lg p-8">
          {sent ? (
            <div className="text-center space-y-4">
              <div className="text-5xl">📧</div>
              <h2 className="text-white font-semibold text-lg">Kiểm tra email của bạn</h2>
              <p className="text-gray-400 text-sm">
                Nếu email <strong className="text-white">{email}</strong> tồn tại trong hệ thống,
                chúng tôi đã gửi link đặt lại mật khẩu. Link có hiệu lực trong 15 phút.
              </p>
              <p className="text-gray-500 text-xs">Không thấy email? Hãy kiểm tra thư mục Spam.</p>
              <Link
                href="/login"
                className="inline-block mt-4 text-primary hover:underline text-sm"
              >
                ← Quay lại đăng nhập
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              <p className="text-gray-400 text-sm">
                Nhập email đăng ký của bạn. Chúng tôi sẽ gửi link để đặt lại mật khẩu.
              </p>

              {error && (
                <div className="bg-red-500/10 border border-red-500 text-red-400 px-4 py-3 rounded text-sm">
                  {error}
                </div>
              )}

              <div>
                <label className="block text-gray-300 text-sm mb-2">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full px-4 py-3 bg-zinc-700 border border-zinc-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
                  placeholder="your@email.com"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-primary text-white font-semibold rounded-lg hover:bg-green-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? "Đang gửi..." : "Gửi link đặt lại mật khẩu"}
              </button>

              <p className="text-center text-gray-400 text-sm">
                Nhớ mật khẩu rồi?{" "}
                <Link href="/login" className="text-primary hover:underline">
                  Đăng nhập
                </Link>
              </p>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
