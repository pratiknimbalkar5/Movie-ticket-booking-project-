package com.moviebooking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowRequest {

    @NotNull(message = "Movie ID is required")
    private Long movieId;

    @NotNull(message = "Screen ID is required")
    private Long screenId;

    @NotNull(message = "Show time is required")
    @Future(message = "Show time must be scheduled in the future")
    private LocalDateTime showTime;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be positive")
    private BigDecimal basePrice;

    @NotNull(message = "Premium price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Premium price must be positive")
    private BigDecimal premiumPrice;

    @NotNull(message = "VIP price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "VIP price must be positive")
    private BigDecimal vipPrice;
}
