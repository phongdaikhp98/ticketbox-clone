"use client";

import Link from "next/link";
import { Event } from "@/types/event";

interface EventCardProps {
  event: Event;
  showStatus?: boolean;
  onEdit?: (id: number) => void;
  onDelete?: (id: number) => void;
}

const categoryColorMap: Record<string, string> = {
  music: "bg-purple-500/20 text-purple-400",
  sports: "bg-blue-500/20 text-blue-400",
  conference: "bg-yellow-500/20 text-yellow-400",
  theater: "bg-pink-500/20 text-pink-400",
  film: "bg-red-500/20 text-red-400",
  workshop: "bg-cyan-500/20 text-cyan-400",
  other: "bg-gray-500/20 text-gray-400",
};

const statusColors: Record<string, string> = {
  DRAFT: "bg-yellow-500/20 text-yellow-400",
  PUBLISHED: "bg-green-500/20 text-green-400",
  CANCELLED: "bg-red-500/20 text-red-400",
};

export default function EventCard({ event, showStatus, onEdit, onDelete }: EventCardProps) {
  const minPrice = event.ticketTypes.length > 0
    ? Math.min(...event.ticketTypes.map((t) => t.price))
    : 0;

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(price);
  };

  const categoryColorClass = event.category
    ? categoryColorMap[event.category.slug] || "bg-gray-500/20 text-gray-400"
    : "bg-gray-500/20 text-gray-400";

  const displayTags = event.tags?.slice(0, 3) ?? [];

  return (
    <div className="bg-zinc-800 rounded-lg overflow-hidden hover:ring-1 hover:ring-zinc-600 transition">
      <Link href={onEdit ? `/events/${event.id}/edit` : `/events/${event.id}`}>
        <div className="h-48 bg-zinc-700 relative">
          {event.imageUrl ? (
            <img
              src={event.imageUrl}
              alt={event.title}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-zinc-500">
              No Image
            </div>
          )}
          {event.isFeatured && (
            <span className="absolute top-2 right-2 px-2 py-1 bg-primary text-white text-xs rounded">
              Featured
            </span>
          )}
        </div>
      </Link>

      <div className="p-4 space-y-2">
        <div className="flex items-center gap-2 flex-wrap">
          {event.category && (
            <span className={`px-2 py-0.5 text-xs rounded ${categoryColorClass}`}>
              {event.category.icon && <span className="mr-1">{event.category.icon}</span>}
              {event.category.name}
            </span>
          )}
          {showStatus && (
            <span className={`px-2 py-0.5 text-xs rounded ${statusColors[event.status] || ""}`}>
              {event.status}
            </span>
          )}
        </div>

        <Link href={`/events/${event.id}`}>
          <h3 className="text-white font-semibold hover:text-primary transition line-clamp-2">
            {event.title}
          </h3>
        </Link>

        <p className="text-gray-400 text-sm">{formatDate(event.eventDate)}</p>
        <p className="text-gray-500 text-sm truncate">{event.location}</p>

        {displayTags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {displayTags.map((tag) => (
              <Link
                key={tag.id}
                href={`/events?tag=${encodeURIComponent(tag.name)}`}
                onClick={(e) => e.stopPropagation()}
                className="px-1.5 py-0.5 bg-zinc-700 text-zinc-400 text-xs rounded hover:bg-zinc-600 hover:text-white transition"
              >
                #{tag.name}
              </Link>
            ))}
            {(event.tags?.length ?? 0) > 3 && (
              <span className="px-1.5 py-0.5 text-zinc-500 text-xs">
                +{(event.tags?.length ?? 0) - 3}
              </span>
            )}
          </div>
        )}

        <div className="flex items-center justify-between pt-2 border-t border-zinc-700">
          <span className="text-primary font-medium text-sm">
            {minPrice > 0 ? `From ${formatPrice(minPrice)}` : "Free"}
          </span>
          <span className="text-gray-500 text-xs">
            by {event.organizer.fullName}
          </span>
        </div>

        {(onEdit || onDelete) && (
          <div className="flex gap-2 pt-2">
            {onEdit && (
              <button
                onClick={() => onEdit(event.id)}
                className="flex-1 py-1.5 bg-zinc-700 text-gray-300 rounded hover:bg-zinc-600 transition text-sm"
              >
                Edit
              </button>
            )}
            {onDelete && event.status === "DRAFT" && (
              <button
                onClick={() => onDelete(event.id)}
                className="flex-1 py-1.5 bg-red-500/10 text-red-400 rounded hover:bg-red-500/20 transition text-sm"
              >
                Delete
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
