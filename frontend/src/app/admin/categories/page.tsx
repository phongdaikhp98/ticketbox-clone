"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import ProtectedRoute from "@/components/ProtectedRoute";
import { categoryService, CategoryRequest } from "@/lib/category-service";
import { CategoryInfo } from "@/types/event";

// ─── Modal ───────────────────────────────────────────────────────────────────

interface ModalProps {
  initial?: CategoryInfo | null;
  onClose: () => void;
  onSaved: () => void;
}

function CategoryModal({ initial, onClose, onSaved }: ModalProps) {
  const isEdit = !!initial;
  const [form, setForm] = useState<CategoryRequest>({
    name: initial?.name ?? "",
    slug: initial?.slug ?? "",
    icon: initial?.icon ?? "",
    displayOrder: initial?.displayOrder ?? 0,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({
      ...prev,
      [name]: name === "displayOrder" ? Number(value) : value,
    }));
    setError("");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) { setError("Tên danh mục không được để trống"); return; }
    setLoading(true);
    try {
      if (isEdit && initial) {
        await categoryService.updateCategory(initial.id, form);
      } else {
        await categoryService.createCategory(form);
      }
      onSaved();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-zinc-800 border border-zinc-700 rounded-xl w-full max-w-md">
        <div className="flex items-center justify-between p-5 border-b border-zinc-700">
          <h3 className="text-white font-semibold text-lg">
            {isEdit ? "Chỉnh sửa danh mục" : "Thêm danh mục"}
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-white transition">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {error && (
            <div className="p-3 bg-red-900/30 border border-red-700 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}

          <div>
            <label className="block text-gray-400 text-sm mb-1">Tên danh mục <span className="text-red-400">*</span></label>
            <input
              name="name"
              value={form.name}
              onChange={handleChange}
              required
              className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
              placeholder="Ví dụ: Âm nhạc"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Slug</label>
            <input
              name="slug"
              value={form.slug}
              onChange={handleChange}
              className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
              placeholder="am-nhac (tự sinh nếu để trống)"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Icon (emoji hoặc ký tự)</label>
            <input
              name="icon"
              value={form.icon}
              onChange={handleChange}
              className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
              placeholder="🎵"
            />
          </div>

          <div>
            <label className="block text-gray-400 text-sm mb-1">Thứ tự hiển thị</label>
            <input
              type="number"
              name="displayOrder"
              value={form.displayOrder}
              onChange={handleChange}
              min={0}
              className="w-full px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition disabled:opacity-50 text-sm"
            >
              {loading ? "Đang lưu..." : isEdit ? "Cập nhật" : "Thêm mới"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CategoryInfo | null>(null);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [deleting, setDeleting] = useState(false);

  const fetchCategories = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await categoryService.getCategories();
      setCategories(data);
    } catch {
      setError("Không thể tải danh mục.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchCategories(); }, [fetchCategories]);

  const handleSaved = () => {
    setModalOpen(false);
    setEditing(null);
    fetchCategories();
  };

  const handleDeleteConfirm = async () => {
    if (deleteId === null) return;
    setDeleting(true);
    setDeleteError("");
    try {
      await categoryService.deleteCategory(deleteId);
      setDeleteId(null);
      fetchCategories();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setDeleteError(e.response?.data?.message || "Không thể xóa danh mục");
    } finally {
      setDeleting(false);
    }
  };

  return (
    <ProtectedRoute roles={["ADMIN"]}>
      <div className="min-h-screen bg-secondary">
        <div className="max-w-4xl mx-auto px-4 py-8">

          {/* Header */}
          <div className="mb-8 flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-white">Quản lý danh mục</h1>
              <p className="text-gray-400 mt-1">Thêm, sửa, xóa danh mục sự kiện</p>
            </div>
            <div className="flex items-center gap-3">
              <Link
                href="/admin/dashboard"
                className="text-gray-400 hover:text-white text-sm transition"
              >
                ← Dashboard
              </Link>
              <button
                onClick={() => { setEditing(null); setModalOpen(true); }}
                className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition text-sm font-medium"
              >
                + Thêm danh mục
              </button>
            </div>
          </div>

          {error && (
            <div className="bg-red-900/30 border border-red-700 text-red-400 rounded-lg p-4 mb-6">
              {error}
            </div>
          )}

          {/* Table */}
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-gray-400 border-b border-zinc-700 bg-zinc-800/80">
                  <th className="text-left px-4 py-3 font-medium w-12">#</th>
                  <th className="text-left px-4 py-3 font-medium">Icon</th>
                  <th className="text-left px-4 py-3 font-medium">Tên</th>
                  <th className="text-left px-4 py-3 font-medium">Slug</th>
                  <th className="text-left px-4 py-3 font-medium">Thứ tự</th>
                  <th className="text-right px-4 py-3 font-medium">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i} className="border-b border-zinc-700/50">
                      {Array.from({ length: 6 }).map((_, j) => (
                        <td key={j} className="px-4 py-4">
                          <div className="h-4 bg-zinc-700 rounded animate-pulse" />
                        </td>
                      ))}
                    </tr>
                  ))
                ) : categories.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-16 text-center text-gray-400">
                      Chưa có danh mục nào
                    </td>
                  </tr>
                ) : (
                  categories.map((cat, idx) => (
                    <tr
                      key={cat.id}
                      className="border-b border-zinc-700/50 last:border-0 hover:bg-zinc-700/30 transition"
                    >
                      <td className="px-4 py-3 text-gray-500">{idx + 1}</td>
                      <td className="px-4 py-3 text-xl">{cat.icon || "—"}</td>
                      <td className="px-4 py-3 text-white font-medium">{cat.name}</td>
                      <td className="px-4 py-3 text-gray-400 font-mono text-xs">{cat.slug}</td>
                      <td className="px-4 py-3 text-gray-400">{cat.displayOrder}</td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => { setEditing(cat); setModalOpen(true); }}
                            className="px-3 py-1 text-xs bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition"
                          >
                            Sửa
                          </button>
                          <button
                            onClick={() => { setDeleteId(cat.id); setDeleteError(""); }}
                            className="px-3 py-1 text-xs bg-red-900/40 text-red-400 rounded-lg hover:bg-red-900/60 transition"
                          >
                            Xóa
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          <p className="text-gray-500 text-xs mt-3 text-right">
            {categories.length} danh mục
          </p>
        </div>
      </div>

      {/* Add/Edit modal */}
      {modalOpen && (
        <CategoryModal
          initial={editing}
          onClose={() => { setModalOpen(false); setEditing(null); }}
          onSaved={handleSaved}
        />
      )}

      {/* Delete confirm modal */}
      {deleteId !== null && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-800 border border-zinc-700 rounded-xl w-full max-w-sm p-6">
            <h3 className="text-white font-semibold text-lg mb-2">Xác nhận xóa</h3>
            <p className="text-gray-400 text-sm mb-4">
              Bạn có chắc muốn xóa danh mục{" "}
              <span className="text-white font-medium">
                {categories.find((c) => c.id === deleteId)?.name}
              </span>
              ? Hành động này không thể hoàn tác.
            </p>
            {deleteError && (
              <div className="p-3 bg-red-900/30 border border-red-700 rounded-lg text-red-400 text-sm mb-4">
                {deleteError}
              </div>
            )}
            <div className="flex gap-3">
              <button
                onClick={() => { setDeleteId(null); setDeleteError(""); }}
                className="flex-1 py-2 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
              >
                Hủy
              </button>
              <button
                onClick={handleDeleteConfirm}
                disabled={deleting}
                className="flex-1 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition disabled:opacity-50 text-sm"
              >
                {deleting ? "Đang xóa..." : "Xóa"}
              </button>
            </div>
          </div>
        </div>
      )}
    </ProtectedRoute>
  );
}
