package com.moviebooking.service;

import com.moviebooking.dto.request.BookingRequest;
import com.moviebooking.entity.*;
import com.moviebooking.exception.SeatAlreadyBookedException;
import com.moviebooking.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingConcurrencyTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private ShowRepository showRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    @DisplayName("Simulate competing booking requests for the same seat: exactly 1 succeeds and 1 fails with SeatAlreadyBookedException")
    void testConcurrentBookingAttempts() throws InterruptedException {
        User user1 = User.builder().id(1L).username("alice").role(User.Role.USER).build();
        User user2 = User.builder().id(2L).username("bob").role(User.Role.USER).build();

        Theatre theatre = Theatre.builder().id(1L).name("PVR Cinemas").city("Mumbai").build();
        Screen screen = Screen.builder().id(10L).name("Screen 1").theatre(theatre).build();
        Movie movie = Movie.builder().id(20L).title("Inception").build();
        Show show = Show.builder()
                .id(100L)
                .movie(movie)
                .screen(screen)
                .showTime(LocalDateTime.now().plusDays(2))
                .basePrice(new BigDecimal("250.00"))
                .premiumPrice(new BigDecimal("350.00"))
                .vipPrice(new BigDecimal("500.00"))
                .status(Show.ShowStatus.SCHEDULED)
                .build();

        Seat targetSeat = Seat.builder()
                .id(55L)
                .screen(screen)
                .seatNumber("C5")
                .seatType(Seat.SeatType.REGULAR)
                .build();

        BookingRequest bookingReq = BookingRequest.builder()
                .showId(100L)
                .seatIds(List.of(55L))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(showRepository.findById(100L)).thenReturn(Optional.of(show));
        when(seatRepository.findByIdInWithPessimisticLock(List.of(55L))).thenReturn(List.of(targetSeat));

        // When user 1 calls, seat is not booked. User 2 checks, seat is already booked!
        when(bookingSeatRepository.existsByShowIdAndSeatIdIn(100L, List.of(55L)))
                .thenReturn(false)  // First caller gets false (available)
                .thenReturn(true);  // Second caller gets true (already booked)

        Booking mockBooking = Booking.builder()
                .id(1L)
                .user(user1)
                .show(show)
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("250.00"))
                .bookingSeats(new ArrayList<>())
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // Thread 1: User 1 tries to book seat 55
        executor.submit(() -> {
            try {
                startLatch.await();
                bookingService.bookSeats(1L, bookingReq);
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e instanceof SeatAlreadyBookedException) {
                    conflictCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: User 2 tries to book the same seat 55
        executor.submit(() -> {
            try {
                startLatch.await();
                bookingService.bookSeats(2L, bookingReq);
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e instanceof SeatAlreadyBookedException) {
                    conflictCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        });

        // Release latch to start both threads simultaneously
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one should succeed, and one should fail with 409 conflict
        assertEquals(1, successCount.get(), "Expected exactly 1 booking to succeed");
        assertEquals(1, conflictCount.get(), "Expected exactly 1 booking to fail with SeatAlreadyBookedException");
    }
}
