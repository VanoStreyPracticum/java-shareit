package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping(path = "/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemDto> addItem(
            @RequestHeader(value = "X-Sharer-User-Id") Long userId,
            @RequestBody ItemDto itemDto) {
        ItemDto savedItem = itemService.addItem(itemDto, userId);
        return ResponseEntity.status(201).body(savedItem);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemDto> updateItem(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-Sharer-User-Id", required = false) Long userId,
            @RequestBody ItemDto itemDto) {

        if (userId == null) {
            throw new RuntimeException("X-Sharer-User-Id header is missing");
        }

        ItemDto updatedItem = itemService.updateItem(itemId, itemDto, userId);
        return ResponseEntity.ok().body(updatedItem);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto> getItemById(@PathVariable Long itemId) {
        ItemDto item = itemService.getItemById(itemId);
        return ResponseEntity.ok().body(item);
    }

    @GetMapping
    public ResponseEntity<Collection<ItemDto>> getItemsByOwner(
            @RequestHeader(value = "X-Sharer-User-Id") Long userId) {
        Collection<ItemDto> items = itemService.getItemsByOwner(userId);
        return ResponseEntity.ok().body(items);
    }

    @GetMapping("/search")
    public ResponseEntity<Collection<ItemDto>> searchItems(@RequestParam String text) {
        Collection<ItemDto> items = itemService.searchItems(text);
        return ResponseEntity.ok().body(items);
    }
}
