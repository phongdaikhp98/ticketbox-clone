"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import ProtectedRoute from "@/components/ProtectedRoute";
import { useAuth } from "@/contexts/AuthContext";
import {
  organizerApplicationService,
  OrganizerApplicationRequest,
  OrganizerApplicationResponse,
} from "@/lib/organizer-application-service";

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

const EMPTY_FORM: OrganizerApplicationRequest = {
  orgName: "",
  taxNumber: "",
  contactPhone: "",
  reason: "",
};

export default function OrganizerApplicationPage() {
  const { user } = useAuth();
  const [application, setApplication] =
    useState<OrganizerApplicationResponse | null>(null);
  const [loadingApp, setLoadingApp] = useState(true);
  const [form, setForm] = useState<OrganizerApplicationRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    if (!user || user.role === "ORGANIZER" || user.role === "ADMIN") {
      setLoadingApp(false);
      return;
    }
    organizerApplicationService
      .getMyApplication()
      .then((app) => setApplication(app))
      .catch(() => setApplication(null))
      .finally(() => setLoadingApp(false));
  }, [user]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError("");
    if (!form.orgName.trim()) {
      setSubmitError("Vui lòng nhập tên tổ chức / công ty.");
      return;
    }
    if (!form.taxNumber.trim()) {
      setSubmitError("Vui lòng nhập mã số thuế / CCCD.");
      return;
    }
    if (!form.contactPhone.trim()) {
      setSubmitError("Vui lòng nhập số điện thoại liên hệ.");
      return;
    }
    setSubmitting(true);
    try {
      const result = await organizerApplicationService.submit(form);
      setApplication(result);
      setSubmitted(true);
    } catch (err: unknown) {
      const axiosErr = err as {
        response?: { data?: { message?: string } };
      };
      setSubmitError(
        axiosErr?.response?.data?.message ||
          "Gửi đơn thất bại. Vui lòng thử lại."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const renderContent = () => {
    if (loadingApp) {
      return (
        <div className="flex justify-center py-16">
          <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        </div>
      );
    }

    // Already an organizer or admin
    if (user?.role === "ORGANIZER" || user?.role === "ADMIN") {
      return (
        <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-8 text-center max-w-lg mx-auto">
          <div className="text-5xl mb-4">✅</div>
          <h2 className="text-xl font-semibold text-white mb-2">
            Bạn đã là Organizer
          </h2>
          <p className="text-gray-400 mb-6">
            Tài khoản của bạn đã có quyền tổ chức sự kiện.
          </p>
          <Link
            href="/organizer/dashboard"
            className="inline-block px-6 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition text-sm font-medium"
          >
            Đến Dashboard
          </Link>
        </div>
      );
    }

    // Just submitted successfully
    if (submitted && application?.status === "PENDING") {
      return (
        <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-8 text-center max-w-lg mx-auto">
          <div className="text-5xl mb-4">📨</div>
          <h2 className="text-xl font-semibold text-white mb-2">
            Đơn đã được gửi thành công
          </h2>
          <p className="text-gray-400 mb-2">
            Đơn đăng ký Organizer của bạn đang chờ xét duyệt.
          </p>
          <p className="text-gray-500 text-sm">
            Ngày nộp: {formatDate(application.submittedAt)}
          </p>
        </div>
      );
    }

    // Existing PENDING application
    if (application?.status === "PENDING") {
      return (
        <div className="bg-zinc-800 border border-yellow-700/50 rounded-xl p-8 text-center max-w-lg mx-auto">
          <div className="text-5xl mb-4">⏳</div>
          <h2 className="text-xl font-semibold text-white mb-2">
            Đơn đang chờ duyệt
          </h2>
          <p className="text-gray-400 mb-4">
            Đơn đăng ký Organizer của bạn đang được xem xét. Chúng tôi sẽ
            thông báo kết quả qua email.
          </p>
          <div className="bg-zinc-700 rounded-lg p-4 text-left text-sm space-y-2">
            <div className="flex justify-between">
              <span className="text-gray-400">Tên tổ chức:</span>
              <span className="text-white font-medium">
                {application.orgName}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Mã số thuế:</span>
              <span className="text-white">{application.taxNumber}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Ngày nộp:</span>
              <span className="text-white">
                {formatDate(application.submittedAt)}
              </span>
            </div>
          </div>
        </div>
      );
    }

    // APPROVED application
    if (application?.status === "APPROVED") {
      return (
        <div className="bg-zinc-800 border border-green-700/50 rounded-xl p-8 text-center max-w-lg mx-auto">
          <div className="text-5xl mb-4">🎉</div>
          <h2 className="text-xl font-semibold text-white mb-2">
            Đơn đã được duyệt
          </h2>
          <p className="text-gray-400 mb-2">
            Chúc mừng! Đơn đăng ký Organizer của bạn đã được phê duyệt.
          </p>
          {application.reviewNote && (
            <p className="text-gray-400 text-sm mb-4 italic">
              Ghi chú: {application.reviewNote}
            </p>
          )}
          <Link
            href="/organizer/dashboard"
            className="inline-block px-6 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition text-sm font-medium"
          >
            Đến Dashboard
          </Link>
        </div>
      );
    }

    // REJECTED — show reason + re-apply form
    const isRejected = application?.status === "REJECTED";

    return (
      <div className="max-w-lg mx-auto space-y-6">
        {isRejected && (
          <div className="bg-red-900/20 border border-red-700/50 rounded-xl p-6">
            <div className="flex items-start gap-3">
              <span className="text-2xl">❌</span>
              <div>
                <h3 className="text-red-400 font-semibold mb-1">
                  Đơn đã bị từ chối
                </h3>
                {application?.reviewNote && (
                  <p className="text-gray-300 text-sm">
                    Lý do: {application.reviewNote}
                  </p>
                )}
                {application?.reviewedAt && (
                  <p className="text-gray-500 text-xs mt-1">
                    Ngày xét duyệt: {formatDate(application.reviewedAt)}
                  </p>
                )}
                <p className="text-gray-400 text-sm mt-2">
                  Bạn có thể nộp lại đơn bên dưới.
                </p>
              </div>
            </div>
          </div>
        )}

        <div className="bg-zinc-800 border border-zinc-700 rounded-xl p-6">
          <h2 className="text-lg font-semibold text-white mb-1">
            {isRejected ? "Nộp lại đơn đăng ký" : "Đăng ký trở thành Organizer"}
          </h2>
          <p className="text-gray-400 text-sm mb-6">
            Điền đầy đủ thông tin để gửi đơn đăng ký. Chúng tôi sẽ xem xét và
            phản hồi trong vòng 1-3 ngày làm việc.
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">
                Tên tổ chức / công ty{" "}
                <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                name="orgName"
                value={form.orgName}
                onChange={handleChange}
                placeholder="VD: Công ty TNHH Sự kiện ABC"
                className="w-full bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-500 focus:outline-none focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">
                Mã số thuế / CCCD <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                name="taxNumber"
                value={form.taxNumber}
                onChange={handleChange}
                placeholder="VD: 0123456789"
                className="w-full bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-500 focus:outline-none focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">
                Số điện thoại liên hệ{" "}
                <span className="text-red-400">*</span>
              </label>
              <input
                type="tel"
                name="contactPhone"
                value={form.contactPhone}
                onChange={handleChange}
                placeholder="VD: 0901234567"
                className="w-full bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-500 focus:outline-none focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">
                Lý do muốn trở thành Organizer
                <span className="text-gray-500 ml-1 font-normal">
                  (tùy chọn)
                </span>
              </label>
              <textarea
                name="reason"
                value={form.reason}
                onChange={handleChange}
                rows={4}
                placeholder="Mô tả ngắn về loại sự kiện bạn muốn tổ chức và kinh nghiệm của bạn..."
                className="w-full bg-zinc-700 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-gray-500 focus:outline-none focus:border-primary resize-none"
              />
            </div>

            {submitError && (
              <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg px-4 py-3 text-sm">
                {submitError}
              </div>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-2.5 bg-primary text-white rounded-lg font-medium text-sm hover:bg-green-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? "Đang gửi..." : "Gửi đơn đăng ký"}
            </button>
          </form>
        </div>
      </div>
    );
  };

  return (
    <ProtectedRoute>
      <div className="min-h-screen bg-secondary">
        <Header />
        <main className="max-w-2xl mx-auto px-4 py-10">
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-white">
              Đăng ký Organizer
            </h1>
            <p className="text-gray-400 text-sm mt-1">
              Trở thành đối tác tổ chức sự kiện trên Ticketbox
            </p>
          </div>
          {renderContent()}
        </main>
      </div>
    </ProtectedRoute>
  );
}
