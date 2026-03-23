"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { adminService } from "@/lib/admin-service";
import { AdminUser } from "@/types/admin";
import { PageResponse } from "@/types/event";

const ROLE_LABELS: Record<string, string> = {
  CUSTOMER: "Khách",
  ORGANIZER: "Tổ chức",
  ADMIN: "Quản trị",
};

const ROLE_COLORS: Record<string, string> = {
  CUSTOMER: "bg-zinc-700 text-gray-300",
  ORGANIZER: "bg-blue-900/30 text-blue-400",
  ADMIN: "bg-red-900/30 text-red-400",
};

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

export default function AdminUsersPage() {
  const [data, setData] = useState<PageResponse<AdminUser> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [activeFilter, setActiveFilter] = useState("");
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [exportLoading, setExportLoading] = useState(false);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page, size: 10 };
      if (search) params.search = search;
      if (roleFilter) params.role = roleFilter;
      if (activeFilter !== "") params.isActive = activeFilter === "true";
      const result = await adminService.getUsers(params);
      setData(result);
    } catch {
      setError("Không thể tải danh sách người dùng");
    } finally {
      setLoading(false);
    }
  }, [page, search, roleFilter, activeFilter]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleSearch = () => {
    setSearch(searchInput);
    setPage(0);
  };

  const handleChangeRole = async (userId: number, newRole: string) => {
    setActionLoading(userId);
    try {
      const updated = await adminService.changeRole(userId, newRole);
      setData((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((u) => (u.id === userId ? updated : u)),
            }
          : prev
      );
    } catch {
      alert("Không thể thay đổi vai trò");
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleActive = async (userId: number) => {
    setActionLoading(userId);
    try {
      const updated = await adminService.toggleActive(userId);
      setData((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((u) => (u.id === userId ? updated : u)),
            }
          : prev
      );
    } catch {
      alert("Không thể thay đổi trạng thái");
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-7xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-white">Quản lý người dùng</h1>
              <p className="text-gray-400 mt-1">
                {data ? `${data.totalElements.toLocaleString("vi-VN")} người dùng` : ""}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={async () => {
                  setExportLoading(true);
                  try { await adminService.downloadExport("users"); }
                  catch { alert("Không thể xuất báo cáo"); }
                  finally { setExportLoading(false); }
                }}
                disabled={exportLoading}
                className="px-4 py-2 bg-zinc-700 hover:bg-zinc-600 text-white text-sm rounded-lg transition flex items-center gap-2 disabled:opacity-50"
              >
                {exportLoading ? "Đang xuất..." : "⬇ Xuất Excel"}
              </button>
              <Link href="/" className="text-gray-400 hover:text-white text-sm transition">
                ← Quay lại trang chủ
              </Link>
            </div>
          </div>

          {/* Filters */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-4 mb-6 flex flex-wrap gap-3">
            <div className="flex gap-2 flex-1 min-w-64">
              <input
                type="text"
                placeholder="Tìm kiếm theo tên hoặc email..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                className="flex-1 bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-400 focus:outline-none focus:border-primary"
              />
              <button
                onClick={handleSearch}
                className="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-green-600 transition"
              >
                Tìm
              </button>
            </div>
            <select
              value={roleFilter}
              onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
              className="bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-primary"
            >
              <option value="">Tất cả vai trò</option>
              <option value="CUSTOMER">Khách</option>
              <option value="ORGANIZER">Tổ chức</option>
              <option value="ADMIN">Quản trị</option>
            </select>
            <select
              value={activeFilter}
              onChange={(e) => { setActiveFilter(e.target.value); setPage(0); }}
              className="bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-primary"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="true">Hoạt động</option>
              <option value="false">Đã khóa</option>
            </select>
          </div>

          {/* Error */}
          {error && (
            <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-4 mb-4">
              {error}
            </div>
          )}

          {/* Table */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-zinc-700">
                  <tr>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">ID</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Họ tên</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Email</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Vai trò</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Trạng thái</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Ngày tạo</th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3">Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={7} className="text-center py-12">
                        <div className="flex justify-center">
                          <div className="w-6 h-6 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                        </div>
                      </td>
                    </tr>
                  ) : data?.content.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="text-center text-gray-400 py-12">
                        Không có người dùng nào
                      </td>
                    </tr>
                  ) : (
                    data?.content.map((user) => (
                      <tr
                        key={user.id}
                        className="border-b border-zinc-700 last:border-0 hover:bg-zinc-750 transition"
                      >
                        <td className="px-4 py-3 text-gray-400 text-sm">{user.id}</td>
                        <td className="px-4 py-3">
                          <p className="text-white text-sm font-medium">{user.fullName}</p>
                          {user.phone && (
                            <p className="text-gray-400 text-xs">{user.phone}</p>
                          )}
                        </td>
                        <td className="px-4 py-3 text-gray-300 text-sm">{user.email}</td>
                        <td className="px-4 py-3">
                          <span
                            className={`text-xs px-2 py-1 rounded-full font-medium ${
                              ROLE_COLORS[user.role] || "bg-zinc-700 text-gray-300"
                            }`}
                          >
                            {ROLE_LABELS[user.role] || user.role}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`text-xs px-2 py-1 rounded-full font-medium ${
                              user.isActive
                                ? "bg-green-900/30 text-green-400"
                                : "bg-red-900/30 text-red-400"
                            }`}
                          >
                            {user.isActive ? "Hoạt động" : "Đã khóa"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-sm">
                          {formatDate(user.createdDate)}
                        </td>
                        <td className="px-4 py-3">
                          {user.role !== "ADMIN" ? (
                            <div className="flex items-center gap-2">
                              <select
                                value={user.role}
                                onChange={(e) => handleChangeRole(user.id, e.target.value)}
                                disabled={actionLoading === user.id}
                                className="bg-zinc-700 border border-zinc-600 rounded px-2 py-1 text-white text-xs focus:outline-none focus:border-primary disabled:opacity-50"
                              >
                                <option value="CUSTOMER">Khách</option>
                                <option value="ORGANIZER">Tổ chức</option>
                              </select>
                              <button
                                onClick={() => handleToggleActive(user.id)}
                                disabled={actionLoading === user.id}
                                className={`text-xs px-3 py-1 rounded transition disabled:opacity-50 ${
                                  user.isActive
                                    ? "bg-red-900/30 text-red-400 hover:bg-red-900/50"
                                    : "bg-green-900/30 text-green-400 hover:bg-green-900/50"
                                }`}
                              >
                                {actionLoading === user.id
                                  ? "..."
                                  : user.isActive
                                  ? "Khóa"
                                  : "Mở khóa"}
                              </button>
                            </div>
                          ) : (
                            <span className="text-gray-600 text-xs">—</span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="mt-6">
              <Pagination
                currentPage={page}
                totalPages={data.totalPages}
                onPageChange={setPage}
              />
            </div>
          )}
        </div>
      </div>
    </ProtectedRoute>
  );
}
