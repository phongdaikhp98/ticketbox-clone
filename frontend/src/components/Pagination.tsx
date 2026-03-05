"use client";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const pages: number[] = [];
  const start = Math.max(0, currentPage - 2);
  const end = Math.min(totalPages - 1, currentPage + 2);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return (
    <div className="flex items-center justify-center gap-2 mt-8">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className="px-3 py-1.5 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 transition text-sm disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Prev
      </button>

      {start > 0 && (
        <>
          <button
            onClick={() => onPageChange(0)}
            className="px-3 py-1.5 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 transition text-sm"
          >
            1
          </button>
          {start > 1 && <span className="text-gray-500">...</span>}
        </>
      )}

      {pages.map((page) => (
        <button
          key={page}
          onClick={() => onPageChange(page)}
          className={`px-3 py-1.5 rounded text-sm transition ${
            page === currentPage
              ? "bg-primary text-white"
              : "bg-zinc-700 text-gray-300 hover:bg-zinc-600"
          }`}
        >
          {page + 1}
        </button>
      ))}

      {end < totalPages - 1 && (
        <>
          {end < totalPages - 2 && <span className="text-gray-500">...</span>}
          <button
            onClick={() => onPageChange(totalPages - 1)}
            className="px-3 py-1.5 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 transition text-sm"
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages - 1}
        className="px-3 py-1.5 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 transition text-sm disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Next
      </button>
    </div>
  );
}
