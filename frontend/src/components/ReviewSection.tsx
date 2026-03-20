"use client";

import { useEffect, useState, useCallback } from "react";
import reviewService from "@/lib/review-service";
import { Review, ReviewRequest } from "@/types/review";
import { PageResponse } from "@/types/event";
import StarRating from "@/components/StarRating";

interface ReviewSectionProps {
  eventId: number;
  currentUserId?: number;
  isAuthenticated: boolean;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .map((w) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export default function ReviewSection({
  eventId,
  currentUserId,
  isAuthenticated,
}: ReviewSectionProps) {
  const [page, setPage] = useState<PageResponse<Review> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // My review state
  const [myReview, setMyReview] = useState<Review | null>(null);
  const [canReview, setCanReview] = useState(false);
  const [myReviewLoading, setMyReviewLoading] = useState(false);

  // Form state
  const [formMode, setFormMode] = useState<"none" | "create" | "edit">("none");
  const [formRating, setFormRating] = useState(5);
  const [formComment, setFormComment] = useState("");
  const [formError, setFormError] = useState("");
  const [formSubmitting, setFormSubmitting] = useState(false);

  // Delete state
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const loadReviews = useCallback(
    async (pg: number) => {
      setLoading(true);
      setError("");
      try {
        const data = await reviewService.getEventReviews(eventId, pg, 5);
        setPage(data);
        setCurrentPage(pg);
      } catch {
        setError("Không thể tải đánh giá. Vui lòng thử lại.");
      } finally {
        setLoading(false);
      }
    },
    [eventId]
  );

  const loadMyReviewStatus = useCallback(async () => {
    if (!isAuthenticated) return;
    setMyReviewLoading(true);
    try {
      const [mine, can] = await Promise.all([
        reviewService.getMyReview(eventId),
        reviewService.canReview(eventId),
      ]);
      setMyReview(mine);
      setCanReview(can);
    } catch {
      // Ignore — not critical
    } finally {
      setMyReviewLoading(false);
    }
  }, [eventId, isAuthenticated]);

  useEffect(() => {
    loadReviews(0);
  }, [loadReviews]);

  useEffect(() => {
    loadMyReviewStatus();
  }, [loadMyReviewStatus]);

  const openCreateForm = () => {
    setFormRating(5);
    setFormComment("");
    setFormError("");
    setFormMode("create");
  };

  const openEditForm = () => {
    if (!myReview) return;
    setFormRating(myReview.rating);
    setFormComment(myReview.comment ?? "");
    setFormError("");
    setFormMode("edit");
  };

  const cancelForm = () => {
    setFormMode("none");
    setFormError("");
  };

  const handleSubmit = async () => {
    if (formRating < 1 || formRating > 5) {
      setFormError("Vui lòng chọn số sao từ 1 đến 5.");
      return;
    }
    setFormSubmitting(true);
    setFormError("");
    const payload: ReviewRequest = {
      rating: formRating,
      comment: formComment.trim() || undefined,
    };
    try {
      if (formMode === "create") {
        const created = await reviewService.createReview(eventId, payload);
        setMyReview(created);
      } else if (formMode === "edit" && myReview) {
        const updated = await reviewService.updateReview(
          eventId,
          myReview.id,
          payload
        );
        setMyReview(updated);
      }
      setFormMode("none");
      // Refresh list to reflect new/updated review
      await loadReviews(0);
    } catch (err: unknown) {
      const msg =
        err &&
        typeof err === "object" &&
        "response" in err
          ? (
              err as {
                response?: { data?: { message?: string } };
              }
            ).response?.data?.message ?? "Đã xảy ra lỗi. Vui lòng thử lại."
          : "Đã xảy ra lỗi. Vui lòng thử lại.";
      setFormError(msg);
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleDelete = async (reviewId: number) => {
    if (!confirm("Bạn có chắc muốn xóa đánh giá này không?")) return;
    setDeletingId(reviewId);
    try {
      await reviewService.deleteReview(eventId, reviewId);
      if (myReview?.id === reviewId) {
        setMyReview(null);
        setCanReview(true);
      }
      await loadReviews(currentPage);
    } catch {
      alert("Không thể xóa đánh giá. Vui lòng thử lại.");
    } finally {
      setDeletingId(null);
    }
  };

  // Derived averages from page data
  const reviews = page?.content ?? [];
  const totalElements = page?.totalElements ?? 0;
  const totalPages = page?.totalPages ?? 0;

  // Compute average from loaded reviews (approximate)
  const avgRating =
    reviews.length > 0
      ? reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length
      : 0;

  return (
    <div className="bg-zinc-800 rounded-lg p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h2 className="text-white font-semibold text-lg">
            Đánh giá &amp; Nhận xét
          </h2>
          {totalElements > 0 && (
            <div className="flex items-center gap-2 mt-1">
              <StarRating
                rating={avgRating}
                size="md"
                showValue={true}
              />
              <span className="text-zinc-400 text-sm">
                {totalElements} đánh giá
              </span>
            </div>
          )}
          {totalElements === 0 && !loading && (
            <p className="text-zinc-500 text-sm mt-1">
              Chưa có đánh giá nào.
            </p>
          )}
        </div>

        {/* CTA button — only when not already showing form */}
        {isAuthenticated && !myReviewLoading && formMode === "none" && (
          <div>
            {myReview ? (
              <button
                onClick={openEditForm}
                className="px-4 py-2 bg-zinc-700 text-zinc-300 text-sm rounded-lg hover:bg-zinc-600 transition"
              >
                Chỉnh sửa đánh giá
              </button>
            ) : canReview ? (
              <button
                onClick={openCreateForm}
                className="px-4 py-2 bg-primary text-white text-sm rounded-lg hover:bg-green-600 transition"
              >
                Viết đánh giá
              </button>
            ) : (
              <span className="text-zinc-500 text-sm italic">
                Mua vé để có thể đánh giá
              </span>
            )}
          </div>
        )}
      </div>

      {/* My existing review banner */}
      {isAuthenticated && myReview && formMode === "none" && (
        <div className="border border-zinc-600 rounded-lg p-4 bg-zinc-700/40 space-y-1">
          <p className="text-zinc-400 text-xs uppercase tracking-wide mb-1">
            Đánh giá của bạn
          </p>
          <div className="flex items-center gap-2">
            <StarRating rating={myReview.rating} size="sm" />
            <span className="text-zinc-300 text-sm">
              {formatDate(myReview.createdDate)}
            </span>
          </div>
          {myReview.comment && (
            <p className="text-zinc-300 text-sm">{myReview.comment}</p>
          )}
        </div>
      )}

      {/* Review form */}
      {formMode !== "none" && (
        <div className="border border-zinc-600 rounded-lg p-4 bg-zinc-700/30 space-y-4">
          <h3 className="text-white font-medium text-sm">
            {formMode === "create" ? "Viết đánh giá" : "Chỉnh sửa đánh giá"}
          </h3>

          <div>
            <p className="text-zinc-400 text-xs mb-1">Số sao</p>
            <StarRating
              rating={formRating}
              interactive={true}
              size="lg"
              onChange={setFormRating}
            />
          </div>

          <div>
            <p className="text-zinc-400 text-xs mb-1">Nhận xét (tùy chọn)</p>
            <textarea
              value={formComment}
              onChange={(e) => setFormComment(e.target.value)}
              rows={3}
              placeholder="Chia sẻ cảm nhận của bạn về sự kiện này..."
              className="w-full bg-zinc-800 border border-zinc-600 rounded-lg px-3 py-2 text-white text-sm placeholder-zinc-500 resize-none focus:outline-none focus:border-primary"
            />
          </div>

          {formError && (
            <p className="text-red-400 text-sm">{formError}</p>
          )}

          <div className="flex gap-2">
            <button
              onClick={handleSubmit}
              disabled={formSubmitting}
              className="px-4 py-2 bg-primary text-white text-sm rounded-lg hover:bg-green-600 transition disabled:opacity-50"
            >
              {formSubmitting
                ? "Đang gửi..."
                : formMode === "create"
                ? "Gửi đánh giá"
                : "Cập nhật"}
            </button>
            <button
              onClick={cancelForm}
              disabled={formSubmitting}
              className="px-4 py-2 bg-zinc-700 text-zinc-300 text-sm rounded-lg hover:bg-zinc-600 transition disabled:opacity-50"
            >
              Hủy
            </button>
          </div>
        </div>
      )}

      {/* Reviews list */}
      {loading ? (
        <div className="text-center text-zinc-400 py-8 text-sm">
          Đang tải đánh giá...
        </div>
      ) : error ? (
        <div className="text-center text-red-400 py-8 text-sm">{error}</div>
      ) : reviews.length === 0 ? (
        <div className="text-center text-zinc-500 py-8 text-sm">
          Hãy là người đầu tiên đánh giá sự kiện này!
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <div
              key={review.id}
              className="border border-zinc-700 rounded-lg p-4 bg-zinc-800/50 space-y-2"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-3">
                  {review.userAvatarUrl ? (
                    <img
                      src={review.userAvatarUrl}
                      alt={review.userName}
                      className="w-9 h-9 rounded-full object-cover flex-shrink-0"
                    />
                  ) : (
                    <div className="w-9 h-9 rounded-full bg-zinc-600 flex items-center justify-center text-zinc-300 text-xs font-semibold flex-shrink-0">
                      {getInitials(review.userName)}
                    </div>
                  )}
                  <div>
                    <p className="text-white text-sm font-medium leading-tight">
                      {review.userName}
                    </p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <StarRating rating={review.rating} size="sm" />
                      <span className="text-zinc-500 text-xs">
                        {formatDate(review.createdDate)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Delete button — own review */}
                {currentUserId === review.userId && (
                  <button
                    onClick={() => handleDelete(review.id)}
                    disabled={deletingId === review.id}
                    className="text-zinc-500 hover:text-red-400 transition text-xs disabled:opacity-50 flex-shrink-0"
                    title="Xóa đánh giá"
                  >
                    {deletingId === review.id ? "..." : "Xóa"}
                  </button>
                )}
              </div>

              {review.comment && (
                <p className="text-zinc-300 text-sm leading-relaxed pl-12">
                  {review.comment}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
          <button
            onClick={() => loadReviews(currentPage - 1)}
            disabled={currentPage === 0 || loading}
            className="px-3 py-1.5 bg-zinc-700 text-zinc-300 text-sm rounded hover:bg-zinc-600 transition disabled:opacity-40"
          >
            &larr; Trước
          </button>
          <span className="text-zinc-400 text-sm">
            {currentPage + 1} / {totalPages}
          </span>
          <button
            onClick={() => loadReviews(currentPage + 1)}
            disabled={currentPage >= totalPages - 1 || loading}
            className="px-3 py-1.5 bg-zinc-700 text-zinc-300 text-sm rounded hover:bg-zinc-600 transition disabled:opacity-40"
          >
            Sau &rarr;
          </button>
        </div>
      )}
    </div>
  );
}
