"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import ProtectedRoute from "@/components/ProtectedRoute";
import { adminService } from "@/lib/admin-service";
import { promoService } from "@/lib/promo-service";
import { AdminOverview } from "@/types/admin";
import { PromoCodeResponse, PromoCodeRequest } from "@/types/promo";

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr);
  return d.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ thanh toán",
  PAID: "Đã thanh toán",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const ORDER_STATUS_COLORS: Record<string, string> = {
  COMPLETED: "bg-green-900/30 text-green-400",
  PAID: "bg-blue-900/30 text-blue-400",
  PENDING: "bg-yellow-900/30 text-yellow-400",
  CANCELLED: "bg-red-900/30 text-red-400",
};

const EMPTY_PROMO: PromoCodeRequest = {
  code: "",
  discountType: "PERCENTAGE",
  discountValue: 0,
  minOrderAmount: undefined,
  usageLimit: undefined,
  startDate: undefined,
  endDate: undefined,
  active: true,
};

export default function AdminDashboardPage() {
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Promo code states
  const [promos, setPromos] = useState<PromoCodeResponse[]>([]);
  const [promoLoading, setPromoLoading] = useState(false);
  const [showPromoModal, setShowPromoModal] = useState(false);
  const [editingPromo, setEditingPromo] = useState<PromoCodeResponse | null>(null);
  const [promoForm, setPromoForm] = useState<PromoCodeRequest>(EMPTY_PROMO);
  const [promoSaving, setPromoSaving] = useState(false);
  const [promoError, setPromoError] = useState("");

  useEffect(() => {
    adminService
      .getOverview()
      .then(setOverview)
      .catch(() => setError("Không thể tải dữ liệu dashboard"))
      .finally(() => setLoading(false));
    loadPromos();
  }, []);

  const loadPromos = () => {
    setPromoLoading(true);
    promoService.getAll().then(setPromos).catch(() => {}).finally(() => setPromoLoading(false));
  };

  const openCreatePromo = () => {
    setEditingPromo(null);
    setPromoForm(EMPTY_PROMO);
    setPromoError("");
    setShowPromoModal(true);
  };

  const openEditPromo = (promo: PromoCodeResponse) => {
    setEditingPromo(promo);
    setPromoForm({
      code: promo.code,
      discountType: promo.discountType,
      discountValue: promo.discountValue,
      minOrderAmount: promo.minOrderAmount,
      usageLimit: promo.usageLimit,
      startDate: promo.startDate ? promo.startDate.slice(0, 16) : undefined,
      endDate: promo.endDate ? promo.endDate.slice(0, 16) : undefined,
      active: promo.active,
    });
    setPromoError("");
    setShowPromoModal(true);
  };

  const handleSavePromo = async () => {
    setPromoSaving(true);
    setPromoError("");
    try {
      if (editingPromo) {
        await promoService.update(editingPromo.id, promoForm);
      } else {
        await promoService.create(promoForm);
      }
      setShowPromoModal(false);
      loadPromos();
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message || "Lưu thất bại"
          : "Lưu thất bại";
      setPromoError(msg);
    } finally {
      setPromoSaving(false);
    }
  };

  const handleTogglePromo = async (id: number) => {
    try {
      const updated = await promoService.toggle(id);
      setPromos((prev) => prev.map((p) => (p.id === id ? updated : p)));
    } catch {}
  };

  const formatCurrencyShort = (val?: number) =>
    val != null ? new Intl.NumberFormat("vi-VN").format(val) + "đ" : "—";

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-7xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-white">Quản trị hệ thống</h1>
              <p className="text-gray-400 mt-1">Thống kê tổng quan toàn hệ thống</p>
            </div>
            <Link
              href="/"
              className="text-gray-400 hover:text-white text-sm transition flex items-center gap-2"
            >
              ← Quay lại trang chủ
            </Link>
          </div>

          {loading && (
            <div className="flex justify-center py-20">
              <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
          )}

          {error && (
            <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-4">
              {error}
            </div>
          )}

          {overview && (
            <>
              {/* KPI Cards */}
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng người dùng</p>
                  <p className="text-2xl sm:text-3xl font-bold text-white mt-1">
                    {overview.totalUsers.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng sự kiện</p>
                  <p className="text-2xl sm:text-3xl font-bold text-white mt-1">
                    {overview.totalEvents.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng doanh thu</p>
                  <p className="text-2xl font-bold text-primary mt-1">
                    {formatCurrency(overview.totalRevenue)}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Tổng đơn hàng</p>
                  <p className="text-2xl sm:text-3xl font-bold text-white mt-1">
                    {overview.totalOrders.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Vé đã bán</p>
                  <p className="text-2xl sm:text-3xl font-bold text-white mt-1">
                    {overview.totalTicketsSold.toLocaleString("vi-VN")}
                  </p>
                </div>
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-5">
                  <p className="text-gray-400 text-sm">Đã check-in</p>
                  <p className="text-2xl sm:text-3xl font-bold text-white mt-1">
                    {overview.totalCheckedIn.toLocaleString("vi-VN")}
                  </p>
                </div>
              </div>

              {/* Quick Links */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8">
                <Link
                  href="/admin/users"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Người dùng</p>
                  <p className="text-gray-400 text-xs mt-1">Quản lý tài khoản</p>
                </Link>
                <Link
                  href="/admin/orders"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Đơn hàng</p>
                  <p className="text-gray-400 text-xs mt-1">Xem tất cả đơn</p>
                </Link>
                <Link
                  href="/admin/events"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Sự kiện</p>
                  <p className="text-gray-400 text-xs mt-1">Quản lý sự kiện</p>
                </Link>
                <Link
                  href="/organizer/dashboard"
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Organizer View</p>
                  <p className="text-gray-400 text-xs mt-1">Dashboard tổ chức</p>
                </Link>
                <button
                  onClick={() => document.getElementById("promo-section")?.scrollIntoView({ behavior: "smooth" })}
                  className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-xl p-4 text-center transition"
                >
                  <p className="text-primary font-semibold">Mã giảm giá</p>
                  <p className="text-gray-400 text-xs mt-1">Quản lý promo codes</p>
                </button>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Top Events by Revenue */}
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6">
                  <h2 className="text-lg font-semibold text-white mb-4">
                    Sự kiện doanh thu cao nhất
                  </h2>
                  {overview.topEventsByRevenue.length === 0 ? (
                    <p className="text-gray-400 text-sm">Chưa có dữ liệu</p>
                  ) : (
                    <div className="space-y-3">
                      {overview.topEventsByRevenue.map((item, idx) => (
                        <div
                          key={item.eventId}
                          className="flex items-center justify-between py-2 border-b border-zinc-700 last:border-0"
                        >
                          <div className="flex items-center gap-3">
                            <span className="w-6 h-6 bg-zinc-700 rounded-full flex items-center justify-center text-xs text-gray-400 font-medium">
                              {idx + 1}
                            </span>
                            <Link
                              href={`/organizer/dashboard/events/${item.eventId}`}
                              className="text-white hover:text-primary text-sm transition line-clamp-1"
                            >
                              {item.eventTitle}
                            </Link>
                          </div>
                          <span className="text-primary font-semibold text-sm whitespace-nowrap ml-4">
                            {formatCurrency(item.revenue)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Recent Orders */}
                <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6">
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-semibold text-white">Đơn hàng gần đây</h2>
                    <Link
                      href="/admin/orders"
                      className="text-primary text-sm hover:underline"
                    >
                      Xem tất cả
                    </Link>
                  </div>
                  {overview.recentOrders.length === 0 ? (
                    <p className="text-gray-400 text-sm">Chưa có đơn hàng</p>
                  ) : (
                    <div className="space-y-3">
                      {overview.recentOrders.map((order) => (
                        <div
                          key={order.orderId}
                          className="flex items-center justify-between py-2 border-b border-zinc-700 last:border-0"
                        >
                          <div className="min-w-0 flex-1">
                            <Link
                              href={`/admin/orders/${order.orderId}`}
                              className="text-white hover:text-primary text-sm font-medium transition"
                            >
                              #{order.orderId}
                            </Link>
                            <p className="text-gray-400 text-xs truncate">{order.customerName}</p>
                          </div>
                          <div className="text-right ml-4 flex-shrink-0">
                            <p className="text-primary text-sm font-semibold">
                              {formatCurrency(order.totalAmount)}
                            </p>
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full ${
                                ORDER_STATUS_COLORS[order.status] || "bg-zinc-700 text-gray-400"
                              }`}
                            >
                              {ORDER_STATUS_LABELS[order.status] || order.status}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Promo Code Management */}
              <div id="promo-section" className="mt-8 bg-zinc-800 border border-zinc-700 rounded-xl p-6">
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-lg font-semibold text-white">Mã giảm giá</h2>
                  <button
                    onClick={openCreatePromo}
                    className="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-green-600 transition"
                  >
                    + Tạo mã mới
                  </button>
                </div>

                {promoLoading ? (
                  <div className="text-center text-gray-400 py-8">Đang tải...</div>
                ) : promos.length === 0 ? (
                  <div className="text-center text-gray-400 py-8">Chưa có mã giảm giá nào</div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="text-gray-400 border-b border-zinc-700">
                          <th className="text-left pb-3 pr-4">Mã</th>
                          <th className="text-left pb-3 pr-4">Loại</th>
                          <th className="text-left pb-3 pr-4">Giá trị</th>
                          <th className="text-left pb-3 pr-4">Đã dùng / Giới hạn</th>
                          <th className="text-left pb-3 pr-4">Đơn tối thiểu</th>
                          <th className="text-left pb-3 pr-4">Hiệu lực</th>
                          <th className="text-left pb-3 pr-4">Trạng thái</th>
                          <th className="text-left pb-3">Hành động</th>
                        </tr>
                      </thead>
                      <tbody>
                        {promos.map((promo) => (
                          <tr key={promo.id} className="border-b border-zinc-700/50 last:border-0">
                            <td className="py-3 pr-4 font-mono text-white font-medium">{promo.code}</td>
                            <td className="py-3 pr-4 text-gray-300">
                              {promo.discountType === "PERCENTAGE" ? "%" : "Cố định"}
                            </td>
                            <td className="py-3 pr-4 text-gray-300">
                              {promo.discountType === "PERCENTAGE"
                                ? `${promo.discountValue}%`
                                : formatCurrencyShort(promo.discountValue)}
                            </td>
                            <td className="py-3 pr-4 text-gray-300">
                              {promo.usedCount} / {promo.usageLimit ?? "∞"}
                            </td>
                            <td className="py-3 pr-4 text-gray-300">
                              {formatCurrencyShort(promo.minOrderAmount)}
                            </td>
                            <td className="py-3 pr-4 text-gray-400 text-xs">
                              {promo.startDate
                                ? new Date(promo.startDate).toLocaleDateString("vi-VN")
                                : "—"}{" "}
                              →{" "}
                              {promo.endDate
                                ? new Date(promo.endDate).toLocaleDateString("vi-VN")
                                : "∞"}
                            </td>
                            <td className="py-3 pr-4">
                              <button
                                onClick={() => handleTogglePromo(promo.id)}
                                className={`px-2 py-1 rounded text-xs font-medium transition ${
                                  promo.active
                                    ? "bg-green-500/20 text-green-400 hover:bg-green-500/30"
                                    : "bg-zinc-600 text-zinc-300 hover:bg-zinc-500"
                                }`}
                              >
                                {promo.active ? "Hoạt động" : "Tắt"}
                              </button>
                            </td>
                            <td className="py-3">
                              <button
                                onClick={() => openEditPromo(promo)}
                                className="text-primary hover:underline text-xs"
                              >
                                Sửa
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>

      {/* Promo Code Modal */}
      {showPromoModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6 w-full max-w-md">
            <h3 className="text-white font-semibold text-lg mb-4">
              {editingPromo ? "Sửa mã giảm giá" : "Tạo mã giảm giá mới"}
            </h3>

            <div className="space-y-4">
              <div>
                <label className="text-gray-400 text-sm block mb-1">Mã *</label>
                <input
                  type="text"
                  value={promoForm.code}
                  onChange={(e) => setPromoForm({ ...promoForm, code: e.target.value.toUpperCase() })}
                  placeholder="VD: SUMMER20"
                  className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-gray-400 text-sm block mb-1">Loại giảm giá *</label>
                  <select
                    value={promoForm.discountType}
                    onChange={(e) =>
                      setPromoForm({ ...promoForm, discountType: e.target.value as "PERCENTAGE" | "FLAT" })
                    }
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  >
                    <option value="PERCENTAGE">Phần trăm (%)</option>
                    <option value="FLAT">Số tiền cố định</option>
                  </select>
                </div>
                <div>
                  <label className="text-gray-400 text-sm block mb-1">
                    Giá trị * {promoForm.discountType === "PERCENTAGE" ? "(%)" : "(đ)"}
                  </label>
                  <input
                    type="number"
                    value={promoForm.discountValue || ""}
                    onChange={(e) => setPromoForm({ ...promoForm, discountValue: Number(e.target.value) })}
                    min={1}
                    max={promoForm.discountType === "PERCENTAGE" ? 100 : undefined}
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-gray-400 text-sm block mb-1">Đơn tối thiểu (đ)</label>
                  <input
                    type="number"
                    value={promoForm.minOrderAmount || ""}
                    onChange={(e) =>
                      setPromoForm({ ...promoForm, minOrderAmount: e.target.value ? Number(e.target.value) : undefined })
                    }
                    placeholder="Không giới hạn"
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div>
                  <label className="text-gray-400 text-sm block mb-1">Giới hạn lượt dùng</label>
                  <input
                    type="number"
                    value={promoForm.usageLimit || ""}
                    onChange={(e) =>
                      setPromoForm({ ...promoForm, usageLimit: e.target.value ? Number(e.target.value) : undefined })
                    }
                    placeholder="Không giới hạn"
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-gray-400 text-sm block mb-1">Ngày bắt đầu</label>
                  <input
                    type="datetime-local"
                    value={promoForm.startDate || ""}
                    onChange={(e) =>
                      setPromoForm({ ...promoForm, startDate: e.target.value || undefined })
                    }
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
                <div>
                  <label className="text-gray-400 text-sm block mb-1">Ngày kết thúc</label>
                  <input
                    type="datetime-local"
                    value={promoForm.endDate || ""}
                    onChange={(e) =>
                      setPromoForm({ ...promoForm, endDate: e.target.value || undefined })
                    }
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              </div>

              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={promoForm.active}
                  onChange={(e) => setPromoForm({ ...promoForm, active: e.target.checked })}
                  className="accent-primary"
                />
                <span className="text-gray-300 text-sm">Kích hoạt ngay</span>
              </label>
            </div>

            {promoError && (
              <p className="text-red-400 text-sm mt-3">{promoError}</p>
            )}

            <div className="flex gap-3 mt-5">
              <button
                onClick={() => setShowPromoModal(false)}
                className="flex-1 py-2 bg-zinc-700 text-gray-300 rounded-lg text-sm hover:bg-zinc-600 transition"
              >
                Hủy
              </button>
              <button
                onClick={handleSavePromo}
                disabled={promoSaving}
                className="flex-1 py-2 bg-primary text-white rounded-lg text-sm hover:bg-green-600 transition disabled:opacity-50"
              >
                {promoSaving ? "Đang lưu..." : "Lưu"}
              </button>
            </div>
          </div>
        </div>
      )}
    </ProtectedRoute>
  );
}
