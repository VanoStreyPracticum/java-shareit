package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;

public class ItemMapper {
    
    public static ItemDto toDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.isAvailable())
                .ownerId(item.getOwner() != null ? item.getOwner().getId() : null)
                .requestId(item.getRequestId())
                .build();
    }
}