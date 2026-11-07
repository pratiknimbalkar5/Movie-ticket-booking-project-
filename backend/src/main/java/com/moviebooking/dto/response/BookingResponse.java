package com.moviebooking.dto.response;

import com.moviebooking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long showId;
    private String movieTitle;
    private String theatreName;
    private String screenName;
    private LocalDateTime showTime;
    private BigDecimal totalAmount;
    private Booking.BookingStatus status;
    private LocalDateTime bookingTime;
    private LocalDateTime cancellationTime;
    private List<BookingSeatResponse> bookedSeats;
    private String seatSummary;
}
