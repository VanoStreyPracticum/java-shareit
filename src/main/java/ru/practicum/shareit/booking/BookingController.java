package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    
    @PostMapping
    public ResponseEntity<BookingDto> createBooking(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @Valid @RequestBody BookingRequestDto bookingRequestDto) {
        BookingDto bookingDto = bookingService.createBooking(bookingRequestDto, userId);
        return ResponseEntity.status(201).body(bookingDto);
    }
    
    @PatchMapping("/{bookingId}")
    public ResponseEntity<BookingDto> updateBookingStatus(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @PathVariable Long bookingId,
            @RequestParam Boolean approved) {
        BookingDto bookingDto = bookingService.updateBookingStatus(bookingId, userId, approved);
        return ResponseEntity.ok(bookingDto);
    }
    
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDto> getBookingById(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @PathVariable Long bookingId) {
        BookingDto bookingDto = bookingService.getBookingById(bookingId, userId);
        return ResponseEntity.ok(bookingDto);
    }
    
    @GetMapping
    public ResponseEntity<List<BookingDto>> getUserBookings(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @RequestParam(defaultValue = "ALL") BookingState state,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        List<BookingDto> bookings = bookingService.getUserBookings(userId, state, from, size);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/owner")
    public ResponseEntity<List<BookingDto>> getOwnerBookings(
            @RequestHeader("X-Sharer-User-Id") Long ownerId,
            @RequestParam(defaultValue = "ALL") BookingState state,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        List<BookingDto> bookings = bookingService.getOwnerBookings(ownerId, state, from, size);
        return ResponseEntity.ok(bookings);
    }
}