"use client";

import { useState } from "react";

interface StarRatingProps {
  rating: number;
  maxStars?: number;
  interactive?: boolean;
  size?: "sm" | "md" | "lg";
  onChange?: (rating: number) => void;
  showValue?: boolean;
}

export default function StarRating({
  rating,
  maxStars = 5,
  interactive = false,
  size = "md",
  onChange,
  showValue = false,
}: StarRatingProps) {
  const [hovered, setHovered] = useState<number | null>(null);

  const sizeClass =
    size === "sm" ? "text-sm" : size === "lg" ? "text-2xl" : "text-base";

  const displayRating = hovered !== null ? hovered : rating;

  const handleClick = (star: number) => {
    if (interactive && onChange) {
      onChange(star);
    }
  };

  return (
    <span className={`inline-flex items-center gap-0.5 ${sizeClass}`}>
      {Array.from({ length: maxStars }, (_, i) => {
        const starValue = i + 1;
        const filled = displayRating >= starValue;
        const halfFilled =
          !filled && displayRating >= starValue - 0.5 && !interactive;

        return (
          <span
            key={starValue}
            onClick={() => handleClick(starValue)}
            onMouseEnter={() => interactive && setHovered(starValue)}
            onMouseLeave={() => interactive && setHovered(null)}
            className={
              filled
                ? "text-yellow-400"
                : halfFilled
                ? "text-yellow-300"
                : "text-zinc-600"
            }
            style={interactive ? { cursor: "pointer" } : undefined}
            aria-label={`${starValue} sao`}
          >
            {filled || halfFilled ? "★" : "☆"}
          </span>
        );
      })}
      {showValue && rating > 0 && (
        <span className="ml-1 text-zinc-400 text-sm">
          ({rating.toFixed(1)})
        </span>
      )}
    </span>
  );
}
