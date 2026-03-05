"use client";

import { useState } from "react";
import { EventFilterParams, EVENT_CATEGORIES } from "@/types/event";

interface EventFilterProps {
  onFilter: (params: EventFilterParams) => void;
}

export default function EventFilter({ onFilter }: EventFilterProps) {
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [sort, setSort] = useState("date");
  const [direction, setDirection] = useState("asc");

  const handleApply = () => {
    const params: EventFilterParams = { sort, direction, page: 0 };
    if (search) params.search = search;
    if (category) params.category = category;
    onFilter(params);
  };

  const handleReset = () => {
    setSearch("");
    setCategory("");
    setSort("date");
    setDirection("asc");
    onFilter({});
  };

  return (
    <div className="bg-zinc-800 rounded-lg p-4 space-y-4">
      <div>
        <input
          type="text"
          placeholder="Search events..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleApply()}
          className="w-full px-4 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-primary"
        />
      </div>

      <div className="flex flex-wrap gap-3">
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
        >
          <option value="">All Categories</option>
          {EVENT_CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </select>

        <select
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          className="px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
        >
          <option value="date">Sort by Date</option>
          <option value="title">Sort by Title</option>
        </select>

        <select
          value={direction}
          onChange={(e) => setDirection(e.target.value)}
          className="px-3 py-2 bg-zinc-700 border border-zinc-600 rounded-lg text-white focus:outline-none focus:border-primary text-sm"
        >
          <option value="asc">Ascending</option>
          <option value="desc">Descending</option>
        </select>

        <button
          onClick={handleApply}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-green-600 transition text-sm"
        >
          Apply
        </button>
        <button
          onClick={handleReset}
          className="px-4 py-2 bg-zinc-700 text-gray-300 rounded-lg hover:bg-zinc-600 transition text-sm"
        >
          Reset
        </button>
      </div>
    </div>
  );
}
