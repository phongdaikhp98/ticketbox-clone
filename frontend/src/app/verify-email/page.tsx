"use client";

import { useEffect, useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { authService } from "@/lib/auth-service";

type Status = "loading" | "success" | "error" | "no-token";

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const [status, setStatus] = useState<Status>(token ? "loading" : "no-token");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    if (!token) return;

    // [SECURITY] Basic format/length validation before sending token to backend (M2).
    // Accepts URL-safe characters and common base64 variants; rejects oversized strings.
    const isValidTokenFormat = (t: string) =>
      t.length >= 8 && t.length <= 512 && /^[\w\-=.+/]+$/.test(t);

    if (!isValidTokenFormat(token)) {
      setStatus("no-token");
      return;
    }

    authService
      .verifyEmail(token)
      .then(() => setStatus("success"))
      .catch((err: unknown) => {
        const apiError = err as { response?: { data?: { message?: string } } };
        setErrorMsg(
          apiError.response?.data?.message || "Link xác thực không hợp lệ hoặc đã hết hạn."
        );
        setStatus("error");
      });
  }, [token]);

  if (status === "loading") {
    return (
      <div className="text-center space-y-4">
        <div className="flex justify-center">
          <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        </div>
        <p className="text-gray-400">Đang xác thực email...</p>
      </div>
    );
  }

  if (status === "success") {
    return (
      <div className="text-center space-y-4">
        <div className="text-5xl">🎉</div>
        <h2 className="text-white text-xl font-semibold">Email đã được xác thực!</h2>
        <p className="text-gray-400 text-sm">
          Tài khoản của bạn đã được xác thực thành công. Bạn có thể đăng nhập và sử dụng đầy đủ tính năng.
        </p>
        <Link
          href="/login"
          className="inline-block px-6 py-3 bg-primary hover:bg-green-600 text-white font-semibold rounded-lg transition"
        >
          Đăng nhập ngay
        </Link>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="text-center space-y-4">
        <div className="text-5xl">❌</div>
        <h2 className="text-white text-xl font-semibold">Xác thực thất bại</h2>
        <p className="text-red-400 text-sm">{errorMsg}</p>
        <p className="text-gray-500 text-xs">
          Link xác thực chỉ có hiệu lực 60 phút và chỉ dùng được 1 lần.
        </p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
          <Link
            href="/login"
            className="px-5 py-2.5 bg-primary hover:bg-green-600 text-white text-sm font-medium rounded-lg transition"
          >
            Đăng nhập
          </Link>
          <Link
            href="/profile"
            className="px-5 py-2.5 bg-zinc-700 hover:bg-zinc-600 text-gray-300 text-sm rounded-lg transition"
          >
            Gửi lại email xác thực
          </Link>
        </div>
      </div>
    );
  }

  // no-token
  return (
    <div className="text-center space-y-4">
      <div className="text-5xl">⚠️</div>
      <p className="text-red-400">Link xác thực không hợp lệ.</p>
      <Link href="/profile" className="text-primary hover:underline text-sm">
        Gửi lại email xác thực
      </Link>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <div className="min-h-screen bg-secondary flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <Link href="/">
            <h1 className="text-4xl font-bold text-primary">Ticketbox</h1>
          </Link>
          <p className="text-gray-400 mt-2">Xác thực email</p>
        </div>

        <div className="bg-zinc-800 rounded-lg p-8">
          <Suspense fallback={<div className="text-gray-400 text-center">Đang tải...</div>}>
            <VerifyEmailContent />
          </Suspense>
        </div>
      </div>
    </div>
  );
}
