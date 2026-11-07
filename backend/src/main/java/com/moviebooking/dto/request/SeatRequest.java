package com.moviebooking.dto.request;

import com.moviebooking.entity.Seat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequest {

    @NotBlank(message = "Seat number is required (e.g., A1, B3)")
    private String seatNumber;

    @NotNull(message = "Seat type is required (REGULAR, PREMIUM, VIP)")
    private Seat.SeatType seatType;
}
