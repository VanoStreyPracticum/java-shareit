package ru.practicum.shareit.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.client.ItemClient;
import ru.practicum.shareit.item.comment.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;

@RestController
@RequestMapping(path = "/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<Object> addItem(
            @RequestHeader(value = "X-Sharer-User-Id") @Positive Long userId,
            @Valid @RequestBody ItemRequestDto itemRequestDto) {
        return itemClient.addItem(userId, itemRequestDto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> updateItem(
            @PathVariable @Positive Long itemId,
            @RequestHeader(value = "X-Sharer-User-Id") @Positive Long userId,
            @Valid @RequestBody ItemUpdateDto itemUpdateDto) {
        ItemRequestDto itemRequestDto = ItemRequestDto.builder()
                .name(itemUpdateDto.getName())
                .description(itemUpdateDto.getDescription())
                .available(itemUpdateDto.getAvailable())
                .build();
        return itemClient.updateItem(itemId, userId, itemRequestDto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Object> getItemById(
            @PathVariable @Positive Long itemId,
            @RequestHeader(value = "X-Sharer-User-Id") @Positive Long userId) {
        return itemClient.getItem(itemId, userId);
    }

    @GetMapping
    public ResponseEntity<Object> getItemsByOwner(
            @RequestHeader(value = "X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return itemClient.getItemsByOwner(userId);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchItems(
            @RequestParam String text,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return itemClient.searchItems(text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(
            @PathVariable @Positive Long itemId,
            @RequestHeader(value = "X-Sharer-User-Id") @Positive Long userId,
            @Valid @RequestBody CommentRequestDto commentRequestDto) {
        return itemClient.addComment(itemId, userId, commentRequestDto);
    }
}