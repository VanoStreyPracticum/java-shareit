package ru.practicum.shareit.item;

import java.util.Collection;

public interface ItemService {

    ItemDto addItem(ItemDto itemDto, Long userId);

    ItemDto updateItem(Long itemId, ItemDto itemDto, Long userId);

    ItemDto getItemById(Long itemId);

    Collection<ItemDto> getItemsByOwner(Long userId);

    Collection<ItemDto> searchItems(String text);
}
