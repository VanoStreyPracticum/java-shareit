package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
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
        if (itemRequestDto.getName() == null || itemRequestDto.getName().trim().isEmpty()) {
            throw new BadRequestException("Название не может быть пустым");
        }
        if (itemRequestDto.getDescription() == null || itemRequestDto.getDescription().trim().isEmpty()) {
            throw new BadRequestException("Описание не может быть пустым");
        }
        if (itemRequestDto.getAvailable() == null) {
            throw new BadRequestException("Поле available обязательно");
        }
        
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
        
        Item item = Item.builder()
                .name(itemRequestDto.getName())
                .description(itemRequestDto.getDescription())
                .available(itemRequestDto.getAvailable())
                .owner(owner)
                .requestId(itemRequestDto.getRequestId())
                .build();
        
        Item savedItem = itemRepository.save(item);
        return toItemDtoWithBookingsAndComments(savedItem, userId);
    }
    
    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemRequestDto itemRequestDto, Long userId) {
        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с ID " + itemId + " не найдена"));
        
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
        
        if (!existingItem.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Вещь с ID " + itemId + " не найдена");
        }
        
        if (itemRequestDto.getName() != null && !itemRequestDto.getName().trim().isEmpty()) {
            existingItem.setName(itemRequestDto.getName());
        }
        if (itemRequestDto.getDescription() != null && !itemRequestDto.getDescription().trim().isEmpty()) {
            existingItem.setDescription(itemRequestDto.getDescription());
        }
        if (itemRequestDto.getAvailable() != null) {
            existingItem.setAvailable(itemRequestDto.getAvailable());
        }
        
        Item updatedItem = itemRepository.save(existingItem);
        return toItemDtoWithBookingsAndComments(updatedItem, userId);
    }
    
    @Override
    public ItemDto getItemById(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с ID " + itemId + " не найдена"));
        return toItemDtoWithBookingsAndComments(item, userId);
    }
    
    @Override
    public List<ItemDto> getItemsByOwner(Long userId) {
        getUser(userId);
        List<Item> items = itemRepository.findByOwnerIdOrderById(userId);
        return items.stream()
                .map(item -> toItemDtoWithBookingsAndComments(item, userId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Item> items = itemRepository.searchAvailableItems(text);
        return items.stream()
                .map(item -> {
                    ItemDto dto = ItemMapper.toDto(item);
                    // Получаем комментарии для каждого item
                    List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(item.getId());
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
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<Booking> pastBookings = bookingRepository.findPastApprovedBookings(itemId, userId);
        if (pastBookings.isEmpty()) {
            throw new BadRequestException("Пользователь не брал эту вещь в аренду");
        }
        
        Comment comment = Comment.builder()
                .text(commentRequestDto.getText())
                .item(item)
                .author(author)
                .created(LocalDateTime.now())
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toCommentDto(savedComment);
    }
    
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
    
    private ItemDto toItemDtoWithBookingsAndComments(Item item, Long userId) {
        ItemDto itemDto = ItemMapper.toDto(item);

        if (item.getOwner() != null && item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();

            Booking lastBooking = bookingRepository.findLastBooking(
                    item.getId(), now, BookingStatus.APPROVED);
            if (lastBooking != null) {
                itemDto.setLastBooking(new ItemDto.BookingInfo(
                        lastBooking.getId(),
                        lastBooking.getBooker().getId()));
            }

            Booking nextBooking = bookingRepository.findNextBooking(
                    item.getId(), now, BookingStatus.APPROVED);
            if (nextBooking != null) {
                itemDto.setNextBooking(new ItemDto.BookingInfo(
                        nextBooking.getId(),
                        nextBooking.getBooker().getId()));
            }
        }

        List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(item.getId());
        itemDto.setComments(comments.stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList()));
        
        return itemDto;
    }
}