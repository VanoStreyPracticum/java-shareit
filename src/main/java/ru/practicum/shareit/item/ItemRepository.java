package ru.practicum.shareit.item;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ItemRepository {
    private final Map<Long, Item> storage = new HashMap<>();
    private Long currentId = 1L;

    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(currentId++);
        }
        storage.put(item.getId(), item);
        return item;
    }

    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Collection<Item> findAllByOwnerId(Long ownerId) {
        return storage.values().stream()
                .filter(item -> item.getOwnerId().equals(ownerId))
                .toList();
    }

    public Collection<Item> searchAvailable(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String searchText = text.toLowerCase();

        return storage.values().stream()
                .filter(Item::isAvailable)
                .filter(item -> {
                    String name = item.getName();
                    String description = item.getDescription();
                    return (name != null && name.toLowerCase().contains(searchText)) ||
                            (description != null && description.toLowerCase().contains(searchText));
                })
                .collect(Collectors.toList());
    }


    public void deleteById(Long id) {
        storage.remove(id);
    }
}
