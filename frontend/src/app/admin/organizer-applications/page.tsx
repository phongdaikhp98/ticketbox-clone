"use client";

import { useCallback, useEffect, useState } from "react";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import Pagination from "@/components/Pagination";
import {
  organizerApplicationService,
  OrganizerApplicationResponse,
  ReviewRequest,
} from "@/lib/organizer-application-service";
import { PageResponse } from "@/types/event";

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Từ chối",
};

const STATUS_COLORS: Record<string, string> = {
  PENDING: "bg-yellow-900/30 text-yellow-400",
  APPROVED: "bg-green-900/30 text-green-400",
  REJECTED: "bg-red-900/30 text-red-400",
};

const FILTER_TABS = [
  { value: "", label: "Tất cả" },
  { value: "PENDING", label: "Chờ duyệt" },
  { value: "APPROVED", label: "Đã duyệt" },
  { value: "REJECTED", label: "Từ chối" },
];

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

interface ReviewForm {
  status: "APPROVED" | "REJECTED" | "";
  reviewNote: string;
}

const EMPTY_REVIEW: ReviewForm = { status: "", reviewNote: "" };

export default function AdminOrganizerApplicationsPage() {
  const [data, setData] = useState<PageResponse<OrganizerApplicationResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState("");

  // Modal state
  const [selectedApp, setSelectedApp] =
    useState<OrganizerApplicationResponse | null>(null);
  const [reviewForm, setReviewForm] = useState<ReviewForm>(EMPTY_REVIEW);
  const [reviewError, setReviewError] = useState("");
  const [reviewSubmitting, setReviewSubmitting] = useState(false);

  const fetchApplications = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const result = await organizerApplicationService.getApplications(
        statusFilter || undefined,
        page,
        10
      );
      setData(result);
    } catch {
      setError("Không thể tải danh sách đơn đăng ký.");
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  const handleTabChange = (value: string) => {
    setStatusFilter(value);
    setPage(0);
  };

  const openModal = (app: OrganizerApplicationResponse) => {
    setSelectedApp(app);
    setReviewForm(EMPTY_REVIEW);
    setReviewError("");
  };

  const closeModal = () => {
    setSelectedApp(null);
    setReviewForm(EMPTY_REVIEW);
    setReviewError("");
  };

  const handleReviewSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedApp) return;
    if (!reviewForm.status) {
      setReviewError("Vui lòng chọn kết quả xét duyệt.");
      return;
    }
    if (reviewForm.status === "REJECTED" && !reviewForm.reviewNote.trim()) {
      setReviewError("Vui lòng nhập ghi chú khi từ chối đơn.");
      return;
    }

    setReviewSubmitting(true);
    setReviewError("");
    try {
      const payload: ReviewRequest = {
        status: reviewForm.status,
        reviewNote: reviewForm.reviewNote.trim() || undefined,
      };
      const updated = await organizerApplicationService.reviewApplication(
        selectedApp.id,
        payload
      );
      setData((prev) =>
        prev
          ? {
              ...prev,
              content: prev.content.map((a) =>
                a.id === updated.id ? updated : a
              ),
            }
          : prev
      );
      closeModal();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setReviewError(
        axiosErr?.response?.data?.message ||
          "Xét duyệt thất bại. Vui lòng thử lại."
      );
    } finally {
      setReviewSubmitting(false);
    }
  };

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <Header />
        <div className="max-w-7xl mx-auto px-4 py-8">
          {/* Page header */}
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-white">
                Đơn đăng ký Organizer
              </h1>
              <p className="text-gray-400 mt-1">
                {data
                  ? `${data.totalElements.toLocaleString("vi-VN")} đơn`
                  : ""}
              </p>
            </div>
          </div>

          {/* Filter tabs */}
          <div className="flex gap-2 mb-6 flex-wrap">
            {FILTER_TABS.map((tab) => (
              <button
                key={tab.value}
                onClick={() => handleTabChange(tab.value)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                  statusFilter === tab.value
                    ? "bg-primary text-white"
                    : "bg-zinc-800 text-gray-300 hover:text-white border border-zinc-700"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Error */}
          {error && (
            <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-4 mb-4 text-sm">
              {error}
            </div>
          )}

          {/* Table */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-zinc-700">
                  <tr>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      STT
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      Họ tên
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      Email
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      Tổ chức
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      MST / CCCD
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      SĐT
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      Trạng thái
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      Ngày nộp
                    </th>
                    <th className="text-left text-gray-400 text-xs font-medium px-4 py-3 whitespace-nowrap">
                      Thao tác
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={9} className="text-center py-12">
                        <div className="flex justify-center">
                          <div className="w-6 h-6 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                        </div>
                      </td>
                    </tr>
                  ) : data?.content.length === 0 ? (
                    <tr>
                      <td
                        colSpan={9}
                        className="text-center text-gray-400 py-12 text-sm"
                      >
                        Không có đơn nào
                      </td>
                    </tr>
                  ) : (
                    data?.content.map((app, index) => (
                      <tr
                        key={app.id}
                        className="border-b border-zinc-700 last:border-0 hover:bg-zinc-750 transition"
                      >
                        <td className="px-4 py-3 text-gray-400 text-sm">
                          {page * 10 + index + 1}
                        </td>
                        <td className="px-4 py-3">
                          <p className="text-white text-sm font-medium whitespace-nowrap">
                            {app.userFullName}
                          </p>
                        </td>
                        <td className="px-4 py-3 text-gray-300 text-sm">
                          {app.userEmail}
                        </td>
                        <td className="px-4 py-3 text-gray-300 text-sm whitespace-nowrap">
                          {app.orgName}
                        </td>
                        <td className="px-4 py-3 text-gray-300 text-sm">
                          {app.taxNumber}
                        </td>
                        <td className="px-4 py-3 text-gray-300 text-sm whitespace-nowrap">
                          {app.contactPhone}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={`text-xs px-2 py-1 rounded-full font-medium whitespace-nowrap ${
                              STATUS_COLORS[app.status] ||
                              "bg-zinc-700 text-gray-300"
                            }`}
                          >
                            {STATUS_LABELS[app.status] || app.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-400 text-sm whitespace-nowrap">
                          {formatDate(app.submittedAt)}
                        </td>
                        <td className="px-4 py-3">
                          <button
                            onClick={() => openModal(app)}
                            className="text-xs px-3 py-1.5 bg-zinc-700 text-gray-300 hover:text-white hover:bg-zinc-600 rounded transition whitespace-nowrap"
                          >
                            Xem &amp; Duyệt
                          </button>
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

        {/* Review modal */}
        {selectedApp && (
          <div
            className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 px-4"
            onClick={(e) => {
              if (e.target === e.currentTarget) closeModal();
            }}
          >
            <div className="bg-zinc-800 border border-zinc-700 rounded-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
              {/* Modal header */}
              <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-700">
                <h2 className="text-lg font-semibold text-white">
                  Chi tiết đơn đăng ký
                </h2>
                <button
                  onClick={closeModal}
                  className="text-gray-400 hover:text-white transition"
                >
                  <svg
                    className="w-5 h-5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>

              {/* Application details */}
              <div className="px-6 py-4 space-y-3 border-b border-zinc-700">
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">Họ tên</p>
                    <p className="text-white font-medium">
                      {selectedApp.userFullName}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">Email</p>
                    <p className="text-white">{selectedApp.userEmail}</p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">Tên tổ chức</p>
                    <p className="text-white font-medium">
                      {selectedApp.orgName}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">MST / CCCD</p>
                    <p className="text-white">{selectedApp.taxNumber}</p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">SĐT liên hệ</p>
                    <p className="text-white">{selectedApp.contactPhone}</p>
                  </div>
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">Ngày nộp</p>
                    <p className="text-white">
                      {formatDate(selectedApp.submittedAt)}
                    </p>
                  </div>
                </div>
                {selectedApp.reason && (
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">
                      Lý do đăng ký
                    </p>
                    <p className="text-gray-300 text-sm bg-zinc-700 rounded-lg px-3 py-2">
                      {selectedApp.reason}
                    </p>
                  </div>
                )}
                <div className="flex items-center gap-2">
                  <p className="text-gray-400 text-xs">Trạng thái hiện tại:</p>
                  <span
                    className={`text-xs px-2 py-1 rounded-full font-medium ${
                      STATUS_COLORS[selectedApp.status] ||
                      "bg-zinc-700 text-gray-300"
                    }`}
                  >
                    {STATUS_LABELS[selectedApp.status] || selectedApp.status}
                  </span>
                </div>
                {selectedApp.reviewedByName && (
                  <div className="grid grid-cols-2 gap-3 text-sm">
                    <div>
                      <p className="text-gray-400 text-xs mb-0.5">
                        Người duyệt
                      </p>
                      <p className="text-white">{selectedApp.reviewedByName}</p>
                    </div>
                    {selectedApp.reviewedAt && (
                      <div>
                        <p className="text-gray-400 text-xs mb-0.5">
                          Ngày duyệt
                        </p>
                        <p className="text-white">
                          {formatDate(selectedApp.reviewedAt)}
                        </p>
                      </div>
                    )}
                  </div>
                )}
                {selectedApp.reviewNote && (
                  <div>
                    <p className="text-gray-400 text-xs mb-0.5">Ghi chú</p>
                    <p className="text-gray-300 text-sm bg-zinc-700 rounded-lg px-3 py-2">
                      {selectedApp.reviewNote}
                    </p>
                  </div>
                )}
              </div>

              {/* Review form — only show if still PENDING */}
              {selectedApp.status === "PENDING" ? (
                <form onSubmit={handleReviewSubmit} className="px-6 py-4 space-y-4">
                  <h3 className="text-white font-medium text-sm">
                    Xét duyệt đơn
                  </h3>

                  <div className="flex gap-3">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        name="reviewStatus"
                        value="APPROVED"
                        checked={reviewForm.status === "APPROVED"}
                        onChange={() =>
                          setReviewForm((prev) => ({
                            ...prev,
                            status: "APPROVED",
                          }))
                        }
                        className="accent-primary"
                      />
                      <span className="text-green-400 text-sm font-medium">
                        Duyệt
                      </span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        name="reviewStatus"
                        value="REJECTED"
                        checked={reviewForm.status === "REJECTED"}
                        onChange={() =>
                          setReviewForm((prev) => ({
                            ...prev,
                            status: "REJECTED",
                          }))
                        }
                        className="accent-primary"
                      />
                      <span className="text-red-400 text-sm font-medium">
                        Từ chối
                      </span>
                    </label>
                  </div>

                  <div>
                    <label className="block text-sm text-gray-300 mb-1">
                      Ghi chú
                      {reviewForm.status === "REJECTED" && (
                        <span className="text-red-400 ml-1">*</span>
                      )}
                      {reviewForm.status === "APPROVED" && (
                        <span className="text-gray-500 ml-1 font-normal">
                          (tùy chọn)
                        </span>
                      )}
                    </label>
                    <textarea
                      value={reviewForm.reviewNote}
                      onChange={(e) =>
                        setReviewForm((prev) => ({
                          ...prev,
                          reviewNote: e.target.value,
                        }))
                      }
                      rows={3}
                      placeholder={
                        reviewForm.status === "REJECTED"
                          ? "Nêu rõ lý do từ chối..."
                          : "Ghi chú thêm (nếu có)..."
                      }
                      className="w-full bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-500 focus:outline-none focus:border-primary resize-none"
                    />
                  </div>

                  {reviewError && (
                    <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg px-4 py-3 text-sm">
                      {reviewError}
                    </div>
                  )}

                  <div className="flex gap-3 pt-1">
                    <button
                      type="button"
                      onClick={closeModal}
                      className="flex-1 py-2 bg-zinc-700 text-gray-300 hover:text-white rounded-lg text-sm transition"
                    >
                      Hủy
                    </button>
                    <button
                      type="submit"
                      disabled={reviewSubmitting}
                      className={`flex-1 py-2 rounded-lg text-sm font-medium transition disabled:opacity-50 disabled:cursor-not-allowed ${
                        reviewForm.status === "REJECTED"
                          ? "bg-red-700 hover:bg-red-600 text-white"
                          : "bg-primary hover:bg-green-600 text-white"
                      }`}
                    >
                      {reviewSubmitting
                        ? "Đang xử lý..."
                        : reviewForm.status === "APPROVED"
                        ? "Xác nhận duyệt"
                        : reviewForm.status === "REJECTED"
                        ? "Xác nhận từ chối"
                        : "Xác nhận"}
                    </button>
                  </div>
                </form>
              ) : (
                <div className="px-6 py-4">
                  <button
                    onClick={closeModal}
                    className="w-full py-2 bg-zinc-700 text-gray-300 hover:text-white rounded-lg text-sm transition"
                  >
                    Đóng
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </ProtectedRoute>
  );
}
