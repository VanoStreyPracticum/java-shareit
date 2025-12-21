package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.*;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.comment.*;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.user.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto addItem(ItemRequestDto itemRequestDto, Long userId) {
        log.info("Запрос на добавление новой вещи от пользователя ID: {}, название: {}", 
                userId, itemRequestDto.getName());
        
        if (itemRequestDto.getName() == null || itemRequestDto.getName().trim().isEmpty()) {
            log.warn("Попытка добавления вещи с пустым названием, пользователь ID: {}", userId);
            throw new BadRequestException("Название не может быть пустым");
        }
        if (itemRequestDto.getDescription() == null || itemRequestDto.getDescription().trim().isEmpty()) {
            log.warn("Попытка добавления вещи с пустым описанием, пользователь ID: {}", userId);
            throw new BadRequestException("Описание не может быть пустым");
        }
        if (itemRequestDto.getAvailable() == null) {
            log.warn("Попытка добавления вещи без указания available, пользователь ID: {}", userId);
            throw new BadRequestException("Поле available обязательно");
        }

        log.debug("Поиск пользователя ID: {} для создания вещи", userId);
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден при попытке добавления вещи", userId);
                    return new NotFoundException("Пользователь с ID " + userId + " не найден");
                });

        log.debug("Создание объекта вещи: название={}, доступность={}", 
                itemRequestDto.getName(), itemRequestDto.getAvailable());
        
        Item item = Item.builder()
                .name(itemRequestDto.getName())
                .description(itemRequestDto.getDescription())
                .available(itemRequestDto.getAvailable())
                .owner(owner)
                .requestId(itemRequestDto.getRequestId())
                .build();

        Item savedItem = itemRepository.save(item);
        log.info("Вещь успешно добавлена с ID: {}, название: {}, владелец: {}", 
                savedItem.getId(), savedItem.getName(), savedItem.getOwner().getId());
        
        return toItemDtoWithBookingsAndComments(savedItem, userId);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemRequestDto itemRequestDto, Long userId) {
        log.info("Запрос на обновление вещи ID: {} от пользователя ID: {}", itemId, userId);
        
        log.debug("Поиск вещи ID: {} для обновления", itemId);
        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("Вещь с ID {} не найдена при попытке обновления", itemId);
                    return new NotFoundException("Вещь с ID " + itemId + " не найдена");
                });

        log.debug("Проверка существования пользователя ID: {}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден при попытке обновления вещи", userId);
                    return new NotFoundException("Пользователь с ID " + userId + " не найден");
                });

        if (!existingItem.getOwner().getId().equals(userId)) {
            log.warn("Попытка обновления вещи ID: {} пользователем ID: {}, который не является владельцем. Владелец: {}", 
                    itemId, userId, existingItem.getOwner().getId());
            throw new NotFoundException("Вещь с ID " + itemId + " не найдена");
        }

        boolean hasUpdates = false;
        if (itemRequestDto.getName() != null && !itemRequestDto.getName().trim().isEmpty()) {
            log.debug("Обновление названия вещи ID: {} с '{}' на '{}'", 
                    itemId, existingItem.getName(), itemRequestDto.getName());
            existingItem.setName(itemRequestDto.getName());
            hasUpdates = true;
        }
        if (itemRequestDto.getDescription() != null && !itemRequestDto.getDescription().trim().isEmpty()) {
            log.debug("Обновление описания вещи ID: {}", itemId);
            existingItem.setDescription(itemRequestDto.getDescription());
            hasUpdates = true;
        }
        if (itemRequestDto.getAvailable() != null) {
            log.debug("Обновление доступности вещи ID: {} с {} на {}", 
                    itemId, existingItem.isAvailable(), itemRequestDto.getAvailable());
            existingItem.setAvailable(itemRequestDto.getAvailable());
            hasUpdates = true;
        }

        if (!hasUpdates) {
            log.warn("Запрос на обновление вещи ID: {} не содержит изменений", itemId);
        }

        Item updatedItem = itemRepository.save(existingItem);
        log.info("Вещь ID: {} успешно обновлена пользователем ID: {}", itemId, userId);
        
        return toItemDtoWithBookingsAndComments(updatedItem, userId);
    }

    @Override
    public ItemDto getItemById(Long itemId, Long userId) {
        log.debug("Запрос на получение вещи ID: {} пользователем ID: {}", itemId, userId);
        
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("Вещь с ID {} не найдена", itemId);
                    return new NotFoundException("Вещь с ID " + itemId + " не найдена");
                });

        log.info("Вещь ID: {} успешно получена пользователем ID: {}", itemId, userId);
        return toItemDtoWithBookingsAndComments(item, userId);
    }

    @Override
    public List<ItemDto> getItemsByOwner(Long userId) {
        log.info("Запрос на получение всех вещей владельца ID: {}", userId);
        
        getUser(userId);
        
        List<Item> items = itemRepository.findByOwnerIdOrderById(userId);
        log.debug("Найдено {} вещей для владельца ID: {}", items.size(), userId);
        
        List<ItemDto> result = items.stream()
                .map(item -> toItemDtoWithBookingsAndComments(item, userId))
                .collect(Collectors.toList());
        
        log.info("Возвращено {} вещей для владельца ID: {}", result.size(), userId);
        return result;
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        log.info("Запрос на поиск вещей по тексту: '{}'", text);
        
        if (text == null || text.trim().isEmpty()) {
            log.debug("Текст для поиска пустой, возвращаем пустой список");
            return Collections.emptyList();
        }

        log.debug("Поиск доступных вещей по тексту: '{}'", text);
        List<Item> items = itemRepository.searchAvailableItems(text);
        log.info("Найдено {} вещей по запросу '{}'", items.size(), text);
        
        return items.stream()
                .map(item -> {
                    log.trace("Обработка вещи ID: {} для поиска", item.getId());
                    ItemDto dto = ItemMapper.toDto(item);
                    List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(item.getId());
                    log.trace("Найдено {} комментариев для вещи ID: {}", comments.size(), item.getId());
                    dto.setComments(comments.stream()
                            .map(CommentMapper::toCommentDto)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto addComment(Long itemId, CommentRequestDto commentRequestDto, Long userId) {
        log.info("Запрос на добавление комментария к вещи ID: {} от пользователя ID: {}", itemId, userId);
        
        log.debug("Поиск вещи ID: {} для комментария", itemId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("Вещь с ID {} не найдена при добавлении комментария", itemId);
                    return new NotFoundException("Вещь не найдена");
                });

        log.debug("Поиск пользователя ID: {} для комментария", userId);
        User author = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден при добавлении комментария", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        log.debug("Проверка истории бронирований для вещи ID: {} пользователем ID: {}", itemId, userId);
        List<Booking> pastBookings = bookingRepository.findPastApprovedBookings(itemId, userId);
        if (pastBookings.isEmpty()) {
            log.warn("Пользователь ID: {} не брал вещь ID: {} в аренду, но пытается оставить комментарий", 
                    userId, itemId);
            throw new BadRequestException("Пользователь не брал эту вещь в аренду");
        }

        log.debug("Создание комментария для вещи ID: {} пользователем ID: {}", itemId, userId);
        Comment comment = Comment.builder()
                .text(commentRequestDto.getText())
                .item(item)
                .author(author)
                .created(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Комментарий успешно добавлен с ID: {} к вещи ID: {} от пользователя ID: {}", 
                savedComment.getId(), itemId, userId);
        
        return CommentMapper.toCommentDto(savedComment);
    }

    private User getUser(Long userId) {
        log.trace("Поиск пользователя ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new NotFoundException("Пользователь не найден");
                });
    }

    private ItemDto toItemDtoWithBookingsAndComments(Item item, Long userId) {
        log.trace("Преобразование вещи ID: {} в DTO для пользователя ID: {}", item.getId(), userId);
        
        ItemDto itemDto = ItemMapper.toDto(item);

        if (item.getOwner() != null && item.getOwner().getId().equals(userId)) {
            log.debug("Пользователь ID: {} является владельцем вещи ID: {}, добавляем информацию о бронированиях", 
                    userId, item.getId());
            
            LocalDateTime now = LocalDateTime.now();

            Booking lastBooking = bookingRepository.findLastBooking(
                    item.getId(), now, BookingStatus.APPROVED);
            if (lastBooking != null) {
                log.trace("Найдено последнее бронирование ID: {} для вещи ID: {}", 
                        lastBooking.getId(), item.getId());
                itemDto.setLastBooking(new ItemDto.BookingInfo(
                        lastBooking.getId(),
                        lastBooking.getBooker().getId()));
            }

            Booking nextBooking = bookingRepository.findNextBooking(
                    item.getId(), now, BookingStatus.APPROVED);
            if (nextBooking != null) {
                log.trace("Найдено следующее бронирование ID: {} для вещи ID: {}", 
                        nextBooking.getId(), item.getId());
                itemDto.setNextBooking(new ItemDto.BookingInfo(
                        nextBooking.getId(),
                        nextBooking.getBooker().getId()));
            }
        }

        List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(item.getId());
        log.trace("Найдено {} комментариев для вещи ID: {}", comments.size(), item.getId());
        
        itemDto.setComments(comments.stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList()));

        log.trace("Вещь ID: {} успешно преобразована в DTO", item.getId());
        return itemDto;
    }
}