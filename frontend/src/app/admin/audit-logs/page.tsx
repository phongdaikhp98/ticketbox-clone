"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import auditLogService from "@/lib/audit-log-service";
import { AuditLog } from "@/types/audit-log";
import { PageResponse } from "@/types/event";

// ─── Label & colour maps ────────────────────────────────────────────────────

const ACTION_LABELS: Record<string, string> = {
  CHANGE_ROLE: "Đổi vai trò",
  TOGGLE_ACTIVE: "Bật/Tắt tài khoản",
  TOGGLE_FEATURED: "Nổi bật sự kiện",
  SET_FEATURED_ORDER: "Đặt thứ tự nổi bật",
  CHANGE_EVENT_STATUS: "Đổi trạng thái",
  APPROVE_ORGANIZER: "Duyệt đơn tổ chức",
  REJECT_ORGANIZER: "Từ chối đơn tổ chức",
  CHECK_IN: "Check-in vé",
};

const ACTION_COLORS: Record<string, string> = {
  CHANGE_ROLE: "bg-blue-900/30 text-blue-400",
  TOGGLE_ACTIVE: "bg-yellow-900/30 text-yellow-400",
  TOGGLE_FEATURED: "bg-purple-900/30 text-purple-400",
  SET_FEATURED_ORDER: "bg-indigo-900/30 text-indigo-400",
  CHANGE_EVENT_STATUS: "bg-orange-900/30 text-orange-400",
  APPROVE_ORGANIZER: "bg-green-900/30 text-green-400",
  REJECT_ORGANIZER: "bg-red-900/30 text-red-400",
  CHECK_IN: "bg-teal-900/30 text-teal-400",
};

const ENTITY_TYPE_LABELS: Record<string, string> = {
  USER: "Người dùng",
  EVENT: "Sự kiện",
  OrganizerApplication: "Đơn tổ chức",
  TICKET: "Vé",
};

const ACTION_FILTER_OPTIONS = [
  { value: "", label: "Tất cả hành động" },
  { value: "CHANGE_ROLE", label: "Đổi vai trò" },
  { value: "TOGGLE_ACTIVE", label: "Bật/Tắt tài khoản" },
  { value: "TOGGLE_FEATURED", label: "Nổi bật sự kiện" },
  { value: "CHANGE_EVENT_STATUS", label: "Đổi trạng thái" },
  { value: "APPROVE_ORGANIZER", label: "Duyệt đơn tổ chức" },
  { value: "REJECT_ORGANIZER", label: "Từ chối đơn" },
  { value: "CHECK_IN", label: "Check-in vé" },
];

// ─── Helpers ────────────────────────────────────────────────────────────────

const formatDateTime = (dateStr: string) =>
  new Date(dateStr).toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

// ─── Skeleton row ───────────────────────────────────────────────────────────

function SkeletonRow() {
  return (
    <tr className="border-b border-zinc-700/50">
      {Array.from({ length: 5 }).map((_, i) => (
        <td key={i} className="py-4 pr-4">
          <div className="h-4 bg-zinc-700 rounded animate-pulse" />
        </td>
      ))}
    </tr>
  );
}

// ─── Page ───────────────────────────────────────────────────────────────────

export default function AuditLogsPage() {
  const [data, setData] = useState<PageResponse<AuditLog> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [entityTypeFilter, setEntityTypeFilter] = useState("");
  const [actionFilter, setActionFilter] = useState("");

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const params: { entityType?: string; action?: string; page: number; size: number } = {
        page,
        size: 20,
      };
      if (entityTypeFilter) params.entityType = entityTypeFilter;
      if (actionFilter) params.action = actionFilter;
      const result = await auditLogService.getLogs(params);
      setData(result);
    } catch {
      setError("Không thể tải lịch sử hành động. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  }, [page, entityTypeFilter, actionFilter]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleEntityTypeChange = (value: string) => {
    setEntityTypeFilter(value);
    setPage(0);
  };

  const handleActionChange = (value: string) => {
    setActionFilter(value);
    setPage(0);
  };

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-7xl mx-auto px-4 py-8">

          {/* Page header */}
          <div className="mb-8 flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-white">
                Lịch sử hành động Admin
              </h1>
              <p className="text-gray-400 mt-1">
                Theo dõi mọi thay đổi do quản trị viên thực hiện
              </p>
            </div>
            <Link
              href="/admin/dashboard"
              className="text-gray-400 hover:text-white text-sm transition flex items-center gap-2"
            >
              ← Quay lại Dashboard
            </Link>
          </div>

          {/* Filter bar */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-4 mb-6 flex flex-wrap items-center gap-4">
            <div className="flex items-center gap-2">
              <span className="text-gray-400 text-sm whitespace-nowrap">Đối tượng:</span>
              <div className="flex gap-2">
                {[
                  { value: "", label: "Tất cả" },
                  { value: "USER", label: "Người dùng" },
                  { value: "EVENT", label: "Sự kiện" },
                  { value: "OrganizerApplication", label: "Đơn tổ chức" },
                  { value: "TICKET", label: "Vé" },
                ].map((opt) => (
                  <button
                    key={opt.value}
                    onClick={() => handleEntityTypeChange(opt.value)}
                    className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                      entityTypeFilter === opt.value
                        ? "bg-primary text-white"
                        : "bg-zinc-700 text-gray-300 hover:bg-zinc-600"
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-gray-400 text-sm whitespace-nowrap">Hành động:</span>
              <select
                value={actionFilter}
                onChange={(e) => handleActionChange(e.target.value)}
                className="bg-zinc-700 text-gray-200 text-sm rounded-lg px-3 py-1.5 border border-zinc-600 focus:outline-none focus:ring-1 focus:ring-primary"
              >
                {ACTION_FILTER_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Error state */}
          {error && (
            <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-4 mb-6">
              {error}
            </div>
          )}

          {/* Table */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-gray-400 border-b border-zinc-700 bg-zinc-800/80">
                    <th className="text-left px-4 py-3 font-medium whitespace-nowrap">
                      Thời gian
                    </th>
                    <th className="text-left px-4 py-3 font-medium whitespace-nowrap">
                      Admin
                    </th>
                    <th className="text-left px-4 py-3 font-medium whitespace-nowrap">
                      Hành động
                    </th>
                    <th className="text-left px-4 py-3 font-medium whitespace-nowrap">
                      Đối tượng
                    </th>
                    <th className="text-left px-4 py-3 font-medium whitespace-nowrap">
                      Thay đổi
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    Array.from({ length: 8 }).map((_, i) => (
                      <SkeletonRow key={i} />
                    ))
                  ) : data && data.content.length > 0 ? (
                    data.content.map((log) => (
                      <tr
                        key={log.id}
                        className="border-b border-zinc-700/50 last:border-0 hover:bg-zinc-700/30 transition"
                      >
                        {/* Thời gian */}
                        <td className="px-4 py-3 text-gray-400 whitespace-nowrap">
                          {formatDateTime(log.createdDate)}
                        </td>

                        {/* Admin */}
                        <td className="px-4 py-3">
                          <p className="text-white font-medium leading-tight">
                            {log.adminName}
                          </p>
                          <p className="text-gray-400 text-xs mt-0.5">
                            {log.adminEmail}
                          </p>
                        </td>

                        {/* Hành động */}
                        <td className="px-4 py-3">
                          <span
                            className={`inline-block px-2.5 py-1 rounded-full text-xs font-medium whitespace-nowrap ${
                              ACTION_COLORS[log.action] ??
                              "bg-zinc-700 text-gray-300"
                            }`}
                          >
                            {ACTION_LABELS[log.action] ?? log.action}
                          </span>
                        </td>

                        {/* Đối tượng */}
                        <td className="px-4 py-3">
                          <span className="text-xs text-gray-500 bg-zinc-700 px-2 py-0.5 rounded mr-2">
                            {ENTITY_TYPE_LABELS[log.entityType] ??
                              log.entityType}
                          </span>
                          <span className="text-gray-300 text-xs">
                            {log.entityName}
                          </span>
                        </td>

                        {/* Thay đổi */}
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2 text-xs flex-wrap">
                            <span className="text-red-400 bg-red-900/20 px-2 py-0.5 rounded">
                              {log.oldValue || "—"}
                            </span>
                            <span className="text-gray-500">→</span>
                            <span className="text-green-400 bg-green-900/20 px-2 py-0.5 rounded">
                              {log.newValue || "—"}
                            </span>
                          </div>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td
                        colSpan={5}
                        className="px-4 py-16 text-center text-gray-400"
                      >
                        Chưa có lịch sử hành động nào
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <Pagination
              currentPage={page}
              totalPages={data.totalPages}
              onPageChange={setPage}
            />
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
}
