package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.comment.dto.*;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;

import java.util.List;

@RestController
@RequestMapping(path = "/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemDto> addItem(
            @RequestHeader(value = "X-Sharer-User-Id") Long userId,
            @RequestBody ItemRequestDto itemRequestDto) {
        ItemDto savedItem = itemService.addItem(itemRequestDto, userId);
        return ResponseEntity.status(201).body(savedItem);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-Sharer-User-Id") Long userId,
            @RequestBody ItemRequestDto itemRequestDto) {
        ItemDto updatedItem = itemService.updateItem(itemId, itemRequestDto, userId);
        return ResponseEntity.ok(updatedItem);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto> getItemById(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-Sharer-User-Id") Long userId) {
        ItemDto item = itemService.getItemById(itemId, userId);
        return ResponseEntity.ok(item);
    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> getItemsByOwner(
            @RequestHeader(value = "X-Sharer-User-Id") Long userId) {
        List<ItemDto> items = itemService.getItemsByOwner(userId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemDto>> searchItems(@RequestParam String text) {
        List<ItemDto> items = itemService.searchItems(text);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-Sharer-User-Id") Long userId,
            @RequestBody CommentRequestDto commentRequestDto) {
        CommentDto comment = itemService.addComment(itemId, commentRequestDto, userId);
        return ResponseEntity.ok(comment);
    }
}