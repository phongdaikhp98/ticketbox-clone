package com.example.ticketbox.model;

public enum SeatStatus {
    AVAILABLE,
    SOLD,
    BLOCKED
    // RESERVED exists only in Redis (TTL), never persisted to DB
}
