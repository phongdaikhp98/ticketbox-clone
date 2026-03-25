"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { ticketService } from "@/lib/ticket-service";
import { TicketResponse, TICKET_STATUSES } from "@/types/ticket";

export default function TicketDetailPage() {
  const params = useParams();
  const ticketId = Number(params.id);
  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [downloading, setDownloading] = useState(false);
  const [qrUrl, setQrUrl] = useState<string>("");
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [transferEmail, setTransferEmail] = useState("");
  const [transferring, setTransferring] = useState(false);
  const [transferSuccess, setTransferSuccess] = useState(false);

  useEffect(() => {
    ticketService
      .getTicketDetail(ticketId)
      .then(setTicket)
      .catch(() => setError("Failed to load ticket"))
      .finally(() => setLoading(false));

    ticketService.getQrBlob(ticketId).then((blob) => {
      setQrUrl(URL.createObjectURL(blob));
    }).catch(() => {});

    return () => {
      if (qrUrl) URL.revokeObjectURL(qrUrl);
    };
  }, [ticketId]);

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const handleDownloadPdf = async () => {
    setDownloading(true);
    try {
      await ticketService.downloadPdf(ticketId);
    } catch {
      alert("Failed to download PDF");
    } finally {
      setDownloading(false);
    }
  };

  const handleTransfer = async () => {
    if (!transferEmail.trim()) return;
    setTransferring(true);
    try {
      await ticketService.initiateTransfer(ticketId, transferEmail.trim());
      setTransferSuccess(true);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : null;
      alert(msg || "Không thể tạo yêu cầu chuyển nhượng");
    } finally {
      setTransferring(false);
    }
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-2xl mx-auto px-4 py-8">
          {loading ? (
            <div className="text-center text-gray-400 py-12">Loading...</div>
          ) : error ? (
            <div className="text-center text-red-400 py-12">{error}</div>
          ) : ticket ? (
            <div className="bg-zinc-800 rounded-lg overflow-hidden">
              {/* Ticket header */}
              <div className="bg-gradient-to-r from-primary/20 to-indigo-500/20 p-6">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-3">
                  <div>
                    <h1 className="text-xl font-bold text-white">{ticket.eventTitle}</h1>
                    <p className="text-gray-300 text-sm mt-1">
                      {formatDate(ticket.eventDate)}
                    </p>
                    <p className="text-gray-400 text-sm">{ticket.eventLocation}</p>
                  </div>
                  <span
                    className={`px-3 py-1 text-xs font-medium rounded ${
                      TICKET_STATUSES[ticket.status]?.color || "bg-gray-100 text-gray-800"
                    }`}
                  >
                    {TICKET_STATUSES[ticket.status]?.label || ticket.status}
                  </span>
                </div>
              </div>

              {/* Divider dashed */}
              <div className="relative">
                <div className="absolute left-0 top-0 w-4 h-8 bg-secondary rounded-r-full -translate-y-1/2"></div>
                <div className="absolute right-0 top-0 w-4 h-8 bg-secondary rounded-l-full -translate-y-1/2"></div>
                <div className="border-t border-dashed border-zinc-600 mx-6"></div>
              </div>

              {/* QR Code + Info */}
              <div className="p-6">
                <div className="flex flex-col items-center mb-6">
                  <div className="bg-white p-3 rounded-lg mb-3">
                    {qrUrl ? (
                      <img src={qrUrl} alt="QR Code" width={200} height={200} />
                    ) : (
                      <div className="w-[200px] h-[200px] flex items-center justify-center text-gray-400 text-sm">
                        Loading QR...
                      </div>
                    )}
                  </div>
                  <p className="text-gray-500 text-xs">Scan to check-in</p>
                </div>

                {/* Ticket details */}
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-400 text-sm">Ticket Type</span>
                    <span className="text-white text-sm font-medium">{ticket.ticketTypeName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400 text-sm">Ticket Code</span>
                    <span className="text-primary text-sm font-mono font-medium">
                      {ticket.ticketCode}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-400 text-sm">Issued Date</span>
                    <span className="text-white text-sm">{formatDate(ticket.createdDate)}</span>
                  </div>
                  {ticket.usedAt && (
                    <div className="flex justify-between">
                      <span className="text-gray-400 text-sm">Used At</span>
                      <span className="text-white text-sm">{formatDate(ticket.usedAt)}</span>
                    </div>
                  )}
                </div>

                {/* Action buttons */}
                <div className="mt-6 flex flex-col gap-3">
                  <button
                    onClick={handleDownloadPdf}
                    disabled={downloading}
                    className="w-full py-3 bg-primary text-white font-medium rounded-lg hover:bg-green-600 transition disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                      />
                    </svg>
                    {downloading ? "Downloading..." : "Download PDF"}
                  </button>

                  {ticket.status === "ISSUED" && (
                    <button
                      onClick={() => setShowTransferModal(true)}
                      className="w-full py-3 bg-zinc-700 hover:bg-zinc-600 text-white font-medium rounded-lg transition flex items-center justify-center gap-2"
                    >
                      ↗ Chuyển nhượng vé
                    </button>
                  )}
                </div>
              </div>
            </div>
          ) : null}
        </main>
      </div>

      {/* Transfer Modal */}
      {showTransferModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl w-full max-w-md p-6">
            {transferSuccess ? (
              <div className="text-center">
                <div className="text-green-400 text-4xl mb-3">✓</div>
                <h3 className="text-white font-semibold text-lg mb-2">Yêu cầu đã được gửi!</h3>
                <p className="text-gray-400 text-sm mb-6">
                  Người nhận có <strong className="text-white">48 giờ</strong> để chấp nhận chuyển nhượng bằng cách truy cập link được chia sẻ.
                </p>
                <button
                  onClick={() => { setShowTransferModal(false); setTransferSuccess(false); setTransferEmail(""); }}
                  className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition"
                >
                  Đóng
                </button>
              </div>
            ) : (
              <>
                <h3 className="text-white font-semibold text-lg mb-1">Chuyển nhượng vé</h3>
                <p className="text-gray-400 text-sm mb-4">
                  Vé <span className="text-primary font-mono">{ticket?.ticketCode}</span> sẽ được chuyển cho người nhận sau khi họ xác nhận.
                </p>
                <label className="block text-gray-300 text-sm mb-1">Email người nhận</label>
                <input
                  type="email"
                  value={transferEmail}
                  onChange={(e) => setTransferEmail(e.target.value)}
                  placeholder="email@example.com"
                  className="w-full bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-400 focus:outline-none focus:border-primary mb-4"
                />
                <div className="flex gap-3">
                  <button
                    onClick={() => { setShowTransferModal(false); setTransferEmail(""); }}
                    className="flex-1 py-2 bg-zinc-700 hover:bg-zinc-600 text-white rounded-lg text-sm transition"
                  >
                    Hủy
                  </button>
                  <button
                    onClick={handleTransfer}
                    disabled={transferring || !transferEmail.trim()}
                    className="flex-1 py-2 bg-primary hover:bg-green-600 text-white rounded-lg text-sm font-medium transition disabled:opacity-50"
                  >
                    {transferring ? "Đang gửi..." : "Xác nhận chuyển"}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </ProtectedRoute>
  );
}
