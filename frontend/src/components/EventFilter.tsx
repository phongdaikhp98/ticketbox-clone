"use client";

import { useState, useEffect } from "react";
import { EventFilterParams } from "@/types/event";
import { CategoryInfo } from "@/types/event";
import { categoryService } from "@/lib/category-service";

interface EventFilterProps {
  onFilter: (params: EventFilterParams) => void;
  initialTag?: string;
}

export default function EventFilter({ onFilter, initialTag }: EventFilterProps) {
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState<string>("");
  const [tag, setTag] = useState(initialTag ?? "");
  const [sort, setSort] = useState("date");
  const [direction, setDirection] = useState("asc");
  const [categories, setCategories] = useState<CategoryInfo[]>([]);

  useEffect(() => {
    categoryService.getCategories().then(setCategories).catch(() => {});
  }, []);

  const handleApply = () => {
    const params: EventFilterParams = { sort, direction, page: 0 };
    if (search) params.search = search;
    if (categoryId) params.categoryId = Number(categoryId);
    if (tag) params.tag = tag.trim();
    onFilter(params);
  };

  const handleReset = () => {
    setSearch("");
    setCategoryId("");
    setTag("");
    setSort("date");
    setDirection("asc");
    onFilter({});
  };

  return (
    <div className="bg-zinc-800 rounded-lg p-4 space-y-4">
      <div>
        <input
          type="text"
          placeholder="Tìm kiếm sự kiện..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleApply()}
          className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
        />
      </div>

      <div className="flex flex-wrap gap-3">
        <select
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value)}
          className="px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
        >
          <option value="">Tất cả thể loại</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {cat.icon} {cat.name}
            </option>
          ))}
        </select>

        <input
          type="text"
          placeholder="#tag"
          value={tag}
          onChange={(e) => setTag(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleApply()}
          className="px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary text-sm w-32"
        />

        <select
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          className="px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
        >
          <option value="date">Sắp xếp theo Ngày</option>
          <option value="title">Sắp xếp theo Tên</option>
        </select>

        <select
          value={direction}
          onChange={(e) => setDirection(e.target.value)}
          className="px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
        >
          <option value="asc">Tăng dần</option>
          <option value="desc">Giảm dần</option>
        </select>

        <button
          onClick={handleApply}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition text-sm"
        >
          Áp dụng
        </button>
        <button
          onClick={handleReset}
          className="px-4 py-2 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
        >
          Đặt lại
        </button>
      </div>
    </div>
  );
}
