package ru.practicum.shareit.booking;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b WHERE b.booker.id = :bookerId ORDER BY b.start DESC")
    List<Booking> findByBookerIdOrderByStartDesc(Long bookerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.booker.id = :bookerId " +
           "AND b.start <= :currentTime AND b.end >= :currentTime ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndCurrentTime(Long bookerId, LocalDateTime currentTime, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.booker.id = :bookerId " +
           "AND b.end < :currentTime ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndEndBefore(Long bookerId, LocalDateTime currentTime, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.booker.id = :bookerId " +
           "AND b.start > :currentTime ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndStartAfter(Long bookerId, LocalDateTime currentTime, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.booker.id = :bookerId " +
           "AND b.status = :status ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndStatus(Long bookerId, BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.item.owner.id = :ownerId ORDER BY b.start DESC")
    List<Booking> findByItemOwnerIdOrderByStartDesc(Long ownerId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.item.owner.id = :ownerId " +
           "AND b.start <= :currentTime AND b.end >= :currentTime ORDER BY b.start DESC")
    List<Booking> findByItemOwnerIdAndCurrentTime(Long ownerId, LocalDateTime currentTime, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.item.owner.id = :ownerId " +
           "AND b.end < :currentTime ORDER BY b.start DESC")
    List<Booking> findByItemOwnerIdAndEndBefore(Long ownerId, LocalDateTime currentTime, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.item.owner.id = :ownerId " +
           "AND b.start > :currentTime ORDER BY b.start DESC")
    List<Booking> findByItemOwnerIdAndStartAfter(Long ownerId, LocalDateTime currentTime, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.item.owner.id = :ownerId " +
           "AND b.status = :status ORDER BY b.start DESC")
    List<Booking> findByItemOwnerIdAndStatus(Long ownerId, BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.item.id = :itemId " +
           "AND b.start <= :date AND b.status = :status " +
           "ORDER BY b.start DESC")
    List<Booking> findLastBookings(Long itemId, LocalDateTime date, BookingStatus status);

    default Booking findLastBooking(Long itemId, LocalDateTime date, BookingStatus status) {
        List<Booking> bookings = findLastBookings(itemId, date, status);
        return bookings.isEmpty() ? null : bookings.get(0);
    }

    @Query("SELECT b FROM Booking b WHERE b.item.id = :itemId " +
           "AND b.start > :date AND b.status = :status " +
           "ORDER BY b.start ASC")
    List<Booking> findNextBookings(Long itemId, LocalDateTime date, BookingStatus status);

    default Booking findNextBooking(Long itemId, LocalDateTime date, BookingStatus status) {
        List<Booking> bookings = findNextBookings(itemId, date, status);
        return bookings.isEmpty() ? null : bookings.get(0);
    }

    @Query("SELECT b FROM Booking b " +
           "WHERE b.item.id = :itemId " +
           "AND b.booker.id = :bookerId " +
           "AND b.status = 'APPROVED' " +
           "AND b.end < CURRENT_TIMESTAMP")
    List<Booking> findPastApprovedBookings(Long itemId, Long bookerId);
}