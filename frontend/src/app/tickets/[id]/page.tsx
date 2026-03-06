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

  useEffect(() => {
    ticketService
      .getTicketDetail(ticketId)
      .then(setTicket)
      .catch(() => setError("Failed to load ticket"))
      .finally(() => setLoading(false));

    // Fetch QR image via authenticated API call
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
                <div className="flex justify-between items-start">
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

                {/* Download PDF button */}
                <button
                  onClick={handleDownloadPdf}
                  disabled={downloading}
                  className="w-full mt-6 py-3 bg-primary text-white font-medium rounded-lg hover:bg-green-600 transition disabled:opacity-50 flex items-center justify-center gap-2"
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
              </div>
            </div>
          ) : null}
        </main>
      </div>
    </ProtectedRoute>
  );
}
