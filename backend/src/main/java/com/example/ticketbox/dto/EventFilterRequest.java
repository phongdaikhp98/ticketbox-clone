package com.example.ticketbox.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventFilterRequest {

    private Long categoryId;
    private String tag;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private String location;
    private String search;
    private String sort;
    private String direction;
    private Integer page = 0;
    private Integer size = 10;
}
