package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import java.util.List;

public interface BookingService {
    BookingDto createBooking(BookingRequestDto bookingRequestDto, Long userId);

    BookingDto updateBookingStatus(Long bookingId, Long userId, Boolean approved);

    BookingDto getBookingById(Long bookingId, Long userId);

    List<BookingDto> getUserBookings(Long userId, BookingState state, int from, int size);

    List<BookingDto> getOwnerBookings(Long ownerId, BookingState state, int from, int size);
}