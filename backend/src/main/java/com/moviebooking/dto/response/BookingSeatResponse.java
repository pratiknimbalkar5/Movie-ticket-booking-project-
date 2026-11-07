package com.moviebooking.dto.response;

import com.moviebooking.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatResponse {

    private Long seatId;
    private String seatNumber;
    private Seat.SeatType seatType;
    private BigDecimal price;
}
