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

  // Advanced filters
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [priceMin, setPriceMin] = useState("");
  const [priceMax, setPriceMax] = useState("");
  const [location, setLocation] = useState("");

  useEffect(() => {
    categoryService.getCategories().then(setCategories).catch(() => {});
  }, []);

  const hasAdvancedFilters =
    !!dateFrom || !!dateTo || !!priceMin || !!priceMax || !!location;

  const handleApply = () => {
    const params: EventFilterParams = {
      sort,
      direction,
      page: 0,
    };
    if (search) params.search = search;
    if (categoryId) params.categoryId = Number(categoryId);
    if (tag) params.tag = tag.trim();
    if (dateFrom) params.dateFrom = dateFrom;
    if (dateTo) params.dateTo = dateTo;
    if (priceMin) params.priceMin = Number(priceMin);
    if (priceMax) params.priceMax = Number(priceMax);
    if (location) params.location = location;
    onFilter(params);
  };

  const handleReset = () => {
    setSearch("");
    setCategoryId("");
    setTag("");
    setSort("date");
    setDirection("asc");
    setDateFrom("");
    setDateTo("");
    setPriceMin("");
    setPriceMax("");
    setLocation("");
    onFilter({});
  };

  return (
    <div className="bg-zinc-800 rounded-lg p-4 space-y-4">
      {/* Basic search */}
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

      {/* Primary filter row */}
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

      {/* Advanced filter toggle */}
      <div>
        <button
          onClick={() => setShowAdvanced((prev) => !prev)}
          className="flex items-center gap-1.5 text-zinc-400 hover:text-white text-sm transition"
        >
          <span>Bộ lọc nâng cao</span>
          <span
            className="transition-transform duration-200"
            style={{
              display: "inline-block",
              transform: showAdvanced ? "rotate(180deg)" : "rotate(0deg)",
            }}
          >
            ▾
          </span>
          {hasAdvancedFilters && !showAdvanced && (
            <span className="ml-1 px-1.5 py-0.5 bg-primary/20 text-primary text-xs rounded">
              Đang lọc
            </span>
          )}
        </button>
      </div>

      {/* Advanced filters panel */}
      {showAdvanced && (
        <div className="border border-zinc-700 rounded-lg p-4 space-y-4 bg-zinc-900/40">
          {/* Date range */}
          <div>
            <p className="text-zinc-400 text-xs mb-2">Khoảng thời gian</p>
            <div className="flex gap-2">
              <input
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                className="flex-1 bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-primary"
                placeholder="Từ ngày"
              />
              <input
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                className="flex-1 bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-primary"
                placeholder="Đến ngày"
              />
            </div>
          </div>

          {/* Price range */}
          <div>
            <p className="text-zinc-400 text-xs mb-2">Khoảng giá (VND)</p>
            <div className="flex gap-2">
              <input
                type="number"
                value={priceMin}
                onChange={(e) => setPriceMin(e.target.value)}
                placeholder="Giá từ"
                min={0}
                className="flex-1 bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-white text-sm placeholder-zinc-500 focus:outline-none focus:border-primary"
              />
              <input
                type="number"
                value={priceMax}
                onChange={(e) => setPriceMax(e.target.value)}
                placeholder="Đến"
                min={0}
                className="flex-1 bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-white text-sm placeholder-zinc-500 focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          {/* Location */}
          <div>
            <p className="text-zinc-400 text-xs mb-2">Địa điểm</p>
            <input
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleApply()}
              placeholder="Địa điểm..."
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-white text-sm placeholder-zinc-500 focus:outline-none focus:border-primary"
            />
          </div>

          {/* Apply button inside advanced panel for convenience */}
          <div className="flex gap-2 pt-1">
            <button
              onClick={handleApply}
              className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition text-sm"
            >
              Áp dụng bộ lọc
            </button>
            {hasAdvancedFilters && (
              <button
                onClick={() => {
                  setDateFrom("");
                  setDateTo("");
                  setPriceMin("");
                  setPriceMax("");
                  setLocation("");
                }}
                className="px-4 py-2 bg-zinc-700 text-zinc-300 rounded-lg hover:bg-zinc-600 transition text-sm"
              >
                Xóa bộ lọc nâng cao
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
