"use client";

import { useState, useRef, useEffect } from "react";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { ticketService } from "@/lib/ticket-service";
import { CheckInResponse } from "@/types/ticket";

type TabType = "manual" | "scan";

export default function CheckInPage() {
  const [activeTab, setActiveTab] = useState<TabType>("manual");
  const [ticketCode, setTicketCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CheckInResponse | null>(null);
  const [error, setError] = useState("");
  const [scanning, setScanning] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const scannerRef = useRef<unknown>(null);

  const handleCheckIn = async (code: string) => {
    if (!code.trim()) return;
    setLoading(true);
    setResult(null);
    setError("");

    try {
      const response = await ticketService.checkIn({ ticketCode: code.trim() });
      setResult(response);
      setTicketCode("");
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr.response?.data?.message || "Check-in failed");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleCheckIn(ticketCode);
  };

  // QR Scanner using html5-qrcode
  const startScanner = async () => {
    setScanning(true);
    setResult(null);
    setError("");

    try {
      const { Html5Qrcode } = await import("html5-qrcode");
      const scanner = new Html5Qrcode("qr-reader");
      scannerRef.current = scanner;

      await scanner.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (decodedText: string) => {
          // Try to parse QR data (JSON with "code" field)
          let code = decodedText;
          try {
            const parsed = JSON.parse(decodedText);
            if (parsed.code) code = parsed.code;
          } catch {
            // Not JSON, use raw text as ticket code
          }
          scanner.stop().catch(() => {});
          setScanning(false);
          handleCheckIn(code);
        },
        () => {
          // QR scan error (no QR detected yet) - ignore
        }
      );
    } catch {
      setError("Failed to start camera. Please check permissions.");
      setScanning(false);
    }
  };

  const stopScanner = async () => {
    if (scannerRef.current) {
      try {
        await (scannerRef.current as { stop: () => Promise<void> }).stop();
      } catch {
        // ignore
      }
    }
    setScanning(false);
  };

  // Cleanup scanner on unmount or tab switch
  useEffect(() => {
    return () => {
      stopScanner();
    };
  }, []);

  useEffect(() => {
    if (activeTab !== "scan") {
      stopScanner();
    }
  }, [activeTab]);

  const getResultStyles = (status: string) => {
    switch (status) {
      case "SUCCESS":
        return "bg-green-900/30 border-green-500 text-green-400";
      case "ALREADY_USED":
        return "bg-yellow-900/30 border-yellow-500 text-yellow-400";
      case "CANCELLED":
        return "bg-red-900/30 border-red-500 text-red-400";
      default:
        return "bg-zinc-700 border-zinc-500 text-gray-300";
    }
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-lg mx-auto px-4 py-8">
          <h1 className="text-2xl font-bold text-white mb-6">Kiểm tra vé</h1>

          {/* Tabs */}
          <div className="flex bg-zinc-800 rounded-lg p-1 mb-6">
            <button
              onClick={() => setActiveTab("manual")}
              className={`flex-1 py-2 text-sm font-medium rounded-md transition ${
                activeTab === "manual"
                  ? "bg-primary text-white"
                  : "text-gray-400 hover:text-white"
              }`}
            >
              Nhập mã
            </button>
            <button
              onClick={() => setActiveTab("scan")}
              className={`flex-1 py-2 text-sm font-medium rounded-md transition ${
                activeTab === "scan"
                  ? "bg-primary text-white"
                  : "text-gray-400 hover:text-white"
              }`}
            >
              Quét QR
            </button>
          </div>

          {/* Manual input */}
          {activeTab === "manual" && (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-gray-400 text-sm mb-2">Mã vé</label>
                <input
                  type="text"
                  value={ticketCode}
                  onChange={(e) => setTicketCode(e.target.value.toUpperCase())}
                  placeholder="TBX-20260306-A3F8K2"
                  className="w-full bg-zinc-800 text-white px-4 py-3 rounded-lg border border-zinc-700 focus:outline-none focus:border-primary font-mono text-lg tracking-wider"
                  autoFocus
                />
              </div>
              <button
                type="submit"
                disabled={loading || !ticketCode.trim()}
                className="w-full py-3 bg-primary text-white font-medium rounded-lg hover:bg-green-600 transition disabled:opacity-50"
              >
                {loading ? "Đang kiểm tra..." : "Kiểm tra"}
              </button>
            </form>
          )}

          {/* QR Scanner */}
          {activeTab === "scan" && (
            <div className="space-y-4">
              <div
                id="qr-reader"
                className="rounded-lg overflow-hidden"
                style={{ display: scanning ? "block" : "none" }}
              >
                <video ref={videoRef} />
              </div>

              {!scanning ? (
                <button
                  onClick={startScanner}
                  className="w-full py-3 bg-primary text-white font-medium rounded-lg hover:bg-green-600 transition flex items-center justify-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
                    />
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
                    />
                  </svg>
                  Mở camera
                </button>
              ) : (
                <button
                  onClick={stopScanner}
                  className="w-full py-3 bg-red-600 text-white font-medium rounded-lg hover:bg-red-700 transition"
                >
                  Dừng quét
                </button>
              )}
            </div>
          )}

          {/* Error */}
          {error && (
            <div className="mt-6 p-4 bg-red-900/30 border border-red-500 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}

          {/* Result */}
          {result && (
            <div className={`mt-6 p-4 border rounded-lg ${getResultStyles(result.status)}`}>
              <div className="flex items-center gap-2 mb-3">
                {result.status === "SUCCESS" ? (
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                      clipRule="evenodd"
                    />
                  </svg>
                ) : (
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fillRule="evenodd"
                      d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                      clipRule="evenodd"
                    />
                  </svg>
                )}
                <span className="font-medium text-lg">{result.message}</span>
              </div>
              <div className="space-y-1 text-sm">
                <p>
                  <span className="opacity-70">Event:</span> {result.eventTitle}
                </p>
                <p>
                  <span className="opacity-70">Attendee:</span> {result.attendeeName}
                </p>
                <p>
                  <span className="opacity-70">Ticket:</span> {result.ticketTypeName}
                </p>
                <p>
                  <span className="opacity-70">Code:</span>{" "}
                  <span className="font-mono">{result.ticketCode}</span>
                </p>
              </div>
            </div>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
