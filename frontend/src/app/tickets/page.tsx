"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { ticketService } from "@/lib/ticket-service";
import { TicketResponse, TICKET_STATUSES } from "@/types/ticket";
import { PageResponse } from "@/types/event";

export default function TicketsPage() {
  const [tickets, setTickets] = useState<PageResponse<TicketResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>("");

  useEffect(() => {
    setLoading(true);
    ticketService
      .getMyTickets(page, 10, undefined, statusFilter || undefined)
      .then(setTickets)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [page, statusFilter]);

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-4xl mx-auto px-4 py-8">
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-2xl font-bold text-white">Vé của tôi</h1>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="bg-zinc-800 text-gray-300 text-sm rounded-lg px-3 py-2 border border-zinc-700 focus:outline-none focus:border-primary"
            >
              <option value="">Tất cả</option>
              <option value="ISSUED">Chưa sử dụng</option>
              <option value="USED">Đã sử dụng</option>
              <option value="CANCELLED">Đã hủy</option>
            </select>
          </div>

          {loading ? (
            <div className="text-center text-gray-400 py-12">Đang tải...</div>
          ) : !tickets || tickets.content.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-400 mb-4">Bạn chưa có vé nào</p>
              <Link href="/events" className="text-primary hover:text-green-400 transition">
                Xem các sự kiện
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {tickets.content.map((ticket) => {
                const statusInfo = TICKET_STATUSES[ticket.status] || {
                  label: ticket.status,
                  color: "bg-gray-100 text-gray-800",
                };
                return (
                  <Link
                    key={ticket.id}
                    href={`/tickets/${ticket.id}`}
                    className="block bg-zinc-800 rounded-lg overflow-hidden hover:bg-zinc-750 transition"
                  >
                    <div className="flex">
                      {ticket.eventImageUrl && (
                        <div className="w-24 h-24 flex-shrink-0">
                          <img
                            src={ticket.eventImageUrl}
                            alt={ticket.eventTitle}
                            className="w-full h-full object-cover"
                          />
                        </div>
                      )}
                      <div className="flex-1 p-4">
                        <div className="flex justify-between items-start">
                          <div>
                            <p className="text-white font-medium">{ticket.eventTitle}</p>
                            <p className="text-gray-400 text-sm mt-1">
                              {formatDate(ticket.eventDate)} &middot; {ticket.eventLocation}
                            </p>
                            <p className="text-gray-500 text-xs mt-1">
                              {ticket.ticketTypeName} &middot; {ticket.ticketCode}
                            </p>
                          </div>
                          <span className={`px-2 py-1 text-xs rounded ${statusInfo.color}`}>
                            {statusInfo.label}
                          </span>
                        </div>
                      </div>
                    </div>
                  </Link>
                );
              })}

              {tickets.totalPages > 1 && (
                <Pagination
                  currentPage={tickets.number}
                  totalPages={tickets.totalPages}
                  onPageChange={setPage}
                />
              )}
            </div>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
