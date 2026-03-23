"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { ticketService } from "@/lib/ticket-service";
import { TicketTransferResponse } from "@/types/ticket";

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  PENDING: { label: "Đang chờ xác nhận", color: "text-yellow-400" },
  COMPLETED: { label: "Đã hoàn tất", color: "text-green-400" },
  CANCELLED: { label: "Đã hủy", color: "text-red-400" },
  EXPIRED: { label: "Đã hết hạn", color: "text-gray-400" },
};

export default function TransferAcceptPage() {
  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <TransferAcceptContent />
      </div>
    </ProtectedRoute>
  );
}

function TransferAcceptContent() {
  const params = useParams();
  const router = useRouter();
  const token = params.token as string;

  const [transfer, setTransfer] = useState<TicketTransferResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [accepted, setAccepted] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    ticketService
      .getTransferByToken(token)
      .then(setTransfer)
      .catch(() => setError("Không tìm thấy yêu cầu chuyển nhượng này"))
      .finally(() => setLoading(false));
  }, [token]);

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  const handleAccept = async () => {
    setAccepting(true);
    try {
      await ticketService.acceptTransfer(token);
      setAccepted(true);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : null;
      setError(msg || "Không thể chấp nhận chuyển nhượng");
    } finally {
      setAccepting(false);
    }
  };

  if (loading) {
    return (
      <main className="max-w-lg mx-auto px-4 py-16 text-center text-gray-400">
        Đang tải...
      </main>
    );
  }

  if (accepted) {
    return (
      <main className="max-w-lg mx-auto px-4 py-16 text-center">
        <div className="text-green-400 text-5xl mb-4">✓</div>
        <h2 className="text-white text-2xl font-bold mb-2">Chuyển nhượng thành công!</h2>
        <p className="text-gray-400 mb-6">Vé đã được thêm vào tài khoản của bạn.</p>
        <button
          onClick={() => router.push("/tickets")}
          className="px-6 py-3 bg-primary text-white rounded-lg hover:bg-green-600 transition"
        >
          Xem vé của tôi
        </button>
      </main>
    );
  }

  if (error && !transfer) {
    return (
      <main className="max-w-lg mx-auto px-4 py-16 text-center">
        <p className="text-red-400 mb-4">{error}</p>
        <Link href="/" className="text-primary hover:underline text-sm">
          Về trang chủ
        </Link>
      </main>
    );
  }

  if (!transfer) return null;

  const statusInfo = STATUS_LABELS[transfer.status] || { label: transfer.status, color: "text-gray-400" };
  const isExpired = new Date(transfer.expiresAt) < new Date();
  const canAccept = transfer.status === "PENDING" && !isExpired;

  return (
    <main className="max-w-lg mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-white mb-6">Chuyển nhượng vé</h1>

      <div className="bg-zinc-800 border border-zinc-700 rounded-xl overflow-hidden mb-4">
        {/* Status */}
        <div className="px-5 py-3 border-b border-zinc-700 flex items-center justify-between">
          <span className="text-gray-400 text-sm">Trạng thái</span>
          <span className={`text-sm font-medium ${statusInfo.color}`}>{statusInfo.label}</span>
        </div>

        {/* Details */}
        <div className="p-5 space-y-3">
          <div className="flex justify-between">
            <span className="text-gray-400 text-sm">Sự kiện</span>
            <span className="text-white text-sm font-medium text-right max-w-[60%] truncate">{transfer.eventTitle}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-400 text-sm">Ngày diễn</span>
            <span className="text-white text-sm">{formatDate(transfer.eventDate)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-400 text-sm">Mã vé</span>
            <span className="text-primary text-sm font-mono">{transfer.ticketCode}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-400 text-sm">Người gửi</span>
            <span className="text-white text-sm">{transfer.fromUserName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-400 text-sm">Người nhận</span>
            <span className="text-white text-sm">{transfer.toEmail}</span>
          </div>
          {canAccept && (
            <div className="flex justify-between">
              <span className="text-gray-400 text-sm">Hết hạn lúc</span>
              <span className="text-yellow-400 text-sm">{formatDate(transfer.expiresAt)}</span>
            </div>
          )}
        </div>
      </div>

      {error && (
        <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-3 mb-4 text-sm">
          {error}
        </div>
      )}

      {canAccept ? (
        <button
          onClick={handleAccept}
          disabled={accepting}
          className="w-full py-3 bg-primary hover:bg-green-600 text-white font-medium rounded-lg transition disabled:opacity-50"
        >
          {accepting ? "Đang xử lý..." : "Chấp nhận nhận vé"}
        </button>
      ) : (
        <Link
          href="/tickets"
          className="block w-full py-3 bg-zinc-700 hover:bg-zinc-600 text-white text-center font-medium rounded-lg transition"
        >
          Xem vé của tôi
        </Link>
      )}
    </main>
  );
}
