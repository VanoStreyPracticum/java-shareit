package ru.practicum.shareit.item;

import ru.practicum.shareit.item.comment.dto.*;
import ru.practicum.shareit.item.dto.*;

import java.util.List;

public interface ItemService {
    ItemDto addItem(ItemRequestDto itemRequestDto, Long userId);

    ItemDto updateItem(Long itemId, ItemRequestDto itemRequestDto, Long userId);

    ItemDto getItemById(Long itemId, Long userId);

    List<ItemDto> getItemsByOwner(Long userId);

    List<ItemDto> searchItems(String text);

    CommentDto addComment(Long itemId, CommentRequestDto commentRequestDto, Long userId);
}