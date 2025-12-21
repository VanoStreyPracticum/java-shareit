package ru.practicum.shareit.booking;

import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.*;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.*;
import ru.practicum.shareit.user.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingDto createBooking(BookingRequestDto bookingRequestDto, Long userId) {
        log.info("Запрос на создание бронирования от пользователя ID {} для вещи ID {}, даты: {} - {}",
                userId, bookingRequestDto.getItemId(), 
                bookingRequestDto.getStart(), bookingRequestDto.getEnd());
        
        User booker = getUser(userId);
        Item item = getItem(bookingRequestDto.getItemId());

        log.debug("Найден пользователь: ID {}, вещь: ID {}, владелец: {}", 
                booker.getId(), item.getId(), item.getOwner().getId());

        if (!item.isAvailable()) {
            log.warn("Вещь ID {} недоступна для бронирования", item.getId());
            throw new BadRequestException("Вещь недоступна для бронирования");
        }

        if (item.getOwner().getId().equals(userId)) {
            log.warn("Пользователь ID {} пытается забронировать свою собственную вещь ID {}", 
                    userId, item.getId());
            throw new NotFoundException("Владелец не может бронировать свою вещь");
        }

        if (bookingRequestDto.getEnd().isBefore(bookingRequestDto.getStart())) {
            log.warn("Некорректные даты бронирования: конец {} раньше начала {}", 
                    bookingRequestDto.getEnd(), bookingRequestDto.getStart());
            throw new BadRequestException("Дата окончания не может быть раньше даты начала");
        }

        if (bookingRequestDto.getStart().isBefore(LocalDateTime.now())) {
            log.warn("Дата начала бронирования {} находится в прошлом", 
                    bookingRequestDto.getStart());
            throw new BadRequestException("Дата начала не может быть в прошлом");
        }

        if (bookingRequestDto.getEnd().isEqual(bookingRequestDto.getStart())) {
            log.warn("Даты начала и окончания бронирования совпадают: {}", 
                    bookingRequestDto.getStart());
            throw new BadRequestException("Дата начала и окончания не могут совпадать");
        }

        Booking booking = Booking.builder()
                .start(bookingRequestDto.getStart())
                .end(bookingRequestDto.getEnd())
                .item(item)
                .booker(booker)
                .status(BookingStatus.WAITING)
                .build();

        log.debug("Создано бронирование: пользователь ID {}, вещь ID {}, статус {}", 
                booker.getId(), item.getId(), BookingStatus.WAITING);
        
        Booking savedBooking = bookingRepository.save(booking);
        
        log.info("Бронирование успешно создано с ID {}", savedBooking.getId());
        return toDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingDto updateBookingStatus(Long bookingId, Long userId, Boolean approved) {
        log.info("Запрос на обновление статуса бронирования ID {} пользователем ID {}, approved = {}", 
                bookingId, userId, approved);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Бронирование с ID {} не найдено", bookingId);
                    return new NotFoundException("Бронирование не найдено");
                });

        log.debug("Найдено бронирование ID {}, владелец вещи: {}, статус: {}", 
                booking.getId(), booking.getItem().getOwner().getId(), booking.getStatus());

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            log.warn("Пользователь ID {} не является владельцем вещи бронирования ID {}", 
                    userId, bookingId);
            throw new ForbiddenException("Только владелец вещи может подтвердить бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            log.warn("Попытка изменить статус уже обработанного бронирования ID {}, текущий статус: {}", 
                    bookingId, booking.getStatus());
            throw new BadRequestException("Бронирование уже было обработано");
        }

        BookingStatus newStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        booking.setStatus(newStatus);
        
        log.debug("Установлен новый статус {} для бронирования ID {}", newStatus, bookingId);
        
        Booking updatedBooking = bookingRepository.save(booking);
        
        log.info("Статус бронирования ID {} успешно обновлен на {}", 
                updatedBooking.getId(), newStatus);
        
        return toDto(updatedBooking);
    }

    @Override
    public BookingDto getBookingById(Long bookingId, Long userId) {
        log.debug("Запрос на получение бронирования ID {} пользователем ID {}", bookingId, userId);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Бронирование с ID {} не найдено", bookingId);
                    return new NotFoundException("Бронирование не найдено");
                });

        log.debug("Найдено бронирование: автор ID {}, владелец вещи ID {}", 
                booking.getBooker().getId(), booking.getItem().getOwner().getId());

        if (!booking.getBooker().getId().equals(userId) &&
            !booking.getItem().getOwner().getId().equals(userId)) {
            log.warn("Пользователь ID {} не имеет доступа к бронированию ID {}", userId, bookingId);
            throw new ForbiddenException("Доступ запрещен");
        }

        log.info("Бронирование ID {} успешно получено пользователем ID {}", bookingId, userId);
        return toDto(booking);
    }

    @Override
    public List<BookingDto> getUserBookings(Long userId, BookingState state, int from, int size) {
        log.info("Запрос на получение бронирований пользователя ID {}, состояние: {}, from: {}, size: {}", 
                userId, state, from, size);
        
        getUser(userId);
        
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        log.debug("Создан Pageable: страница {}, размер {}, сортировка по start DESC", 
                from / size, size);

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL:
                log.debug("Получение всех бронирований пользователя ID {}", userId);
                bookings = bookingRepository.findByBookerIdOrderByStartDesc(userId, pageable);
                break;
            case CURRENT:
                log.debug("Получение текущих бронирований пользователя ID {}", userId);
                bookings = bookingRepository.findByBookerIdAndCurrentTime(userId, now, pageable);
                break;
            case PAST:
                log.debug("Получение прошедших бронирований пользователя ID {}", userId);
                bookings = bookingRepository.findByBookerIdAndEndBefore(userId, now, pageable);
                break;
            case FUTURE:
                log.debug("Получение будущих бронирований пользователя ID {}", userId);
                bookings = bookingRepository.findByBookerIdAndStartAfter(userId, now, pageable);
                break;
            case WAITING:
                log.debug("Получение ожидающих бронирований пользователя ID {}", userId);
                bookings = bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.WAITING, pageable);
                break;
            case REJECTED:
                log.debug("Получение отклоненных бронирований пользователя ID {}", userId);
                bookings = bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.REJECTED, pageable);
                break;
            default:
                log.error("Неизвестное состояние бронирований: {}", state);
                throw new BadRequestException("Unknown state: " + state);
        }

        log.info("Найдено {} бронирований для пользователя ID {} со статусом {}", 
                bookings.size(), userId, state);
        
        return bookings.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getOwnerBookings(Long ownerId, BookingState state, int from, int size) {
        log.info("Запрос на получение бронирований владельца ID {}, состояние: {}, from: {}, size: {}", 
                ownerId, state, from, size);
        
        getUser(ownerId);
        
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        log.debug("Создан Pageable: страница {}, размер {}, сортировка по start DESC", 
                from / size, size);

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL:
                log.debug("Получение всех бронирований владельца ID {}", ownerId);
                bookings = bookingRepository.findByItemOwnerIdOrderByStartDesc(ownerId, pageable);
                break;
            case CURRENT:
                log.debug("Получение текущих бронирований владельца ID {}", ownerId);
                bookings = bookingRepository.findByItemOwnerIdAndCurrentTime(ownerId, now, pageable);
                break;
            case PAST:
                log.debug("Получение прошедших бронирований владельца ID {}", ownerId);
                bookings = bookingRepository.findByItemOwnerIdAndEndBefore(ownerId, now, pageable);
                break;
            case FUTURE:
                log.debug("Получение будущих бронирований владельца ID {}", ownerId);
                bookings = bookingRepository.findByItemOwnerIdAndStartAfter(ownerId, now, pageable);
                break;
            case WAITING:
                log.debug("Получение ожидающих бронирований владельца ID {}", ownerId);
                bookings = bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.WAITING, pageable);
                break;
            case REJECTED:
                log.debug("Получение отклоненных бронирований владельца ID {}", ownerId);
                bookings = bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.REJECTED, pageable);
                break;
            default:
                log.error("Неизвестное состояние бронирований: {}", state);
                throw new BadRequestException("Unknown state: " + state);
        }

        log.info("Найдено {} бронирований для владельца ID {} со статусом {}", 
                bookings.size(), ownerId, state);
        
        return bookings.stream().map(this::toDto).collect(Collectors.toList());
    }

    private User getUser(Long userId) {
        log.debug("Поиск пользователя с ID {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new NotFoundException("Пользователь не найден");
                });
    }

    private Item getItem(Long itemId) {
        log.debug("Поиск вещи с ID {}", itemId);
        return itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("Вещь с ID {} не найдена", itemId);
                    return new NotFoundException("Вещь не найдена");
                });
    }

    private BookingDto toDto(Booking booking) {
        log.trace("Преобразование бронирования ID {} в DTO", booking.getId());
        
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());
        dto.setStatus(booking.getStatus());

        BookingDto.ItemResponse itemResponse = new BookingDto.ItemResponse();
        itemResponse.setId(booking.getItem().getId());
        itemResponse.setName(booking.getItem().getName());
        dto.setItem(itemResponse);

        BookingDto.BookerResponse bookerResponse = new BookingDto.BookerResponse();
        bookerResponse.setId(booking.getBooker().getId());
        bookerResponse.setName(booking.getBooker().getName());
        dto.setBooker(bookerResponse);

        log.trace("Бронирование ID {} успешно преобразовано в DTO", booking.getId());
        return dto;
    }
}