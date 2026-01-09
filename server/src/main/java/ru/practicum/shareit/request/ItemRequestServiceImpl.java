package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.Item;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestResponseDto createRequest(ItemRequestDto itemRequestDto, Long userId) {
        log.info("Запрос на создание запроса вещи от пользователя ID: {}", userId);

        if (itemRequestDto.getDescription() == null || itemRequestDto.getDescription().trim().isEmpty()) {
            log.warn("Попытка создания запроса с пустым описанием, пользователь ID: {}", userId);
            throw new IllegalArgumentException("Описание не может быть пустым");
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден при создании запроса", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        ItemRequest itemRequest = ItemRequest.builder()
                .description(itemRequestDto.getDescription())
                .requester(requester)
                .created(LocalDateTime.now())
                .build();

        ItemRequest savedRequest = itemRequestRepository.save(itemRequest);
        log.info("Запрос успешно создан с ID: {}, пользователь ID: {}", savedRequest.getId(), userId);

        return toResponseDtoWithItems(savedRequest);
    }

    @Override
    public List<ItemRequestResponseDto> getUserRequests(Long userId) {
        log.info("Запрос на получение запросов пользователя ID: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        List<ItemRequest> requests = itemRequestRepository.findByRequesterIdOrderByCreatedDesc(userId);
        log.debug("Найдено {} запросов для пользователя ID: {}", requests.size(), userId);

        return requests.stream()
                .map(this::toResponseDtoWithItems)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemRequestResponseDto> getAllRequests(Long userId, int from, int size) {
        log.info("Запрос на получение всех запросов, пользователь ID: {}, from: {}, size: {}", userId, from, size);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        if (from < 0 || size <= 0) {
            throw new IllegalArgumentException("Неверные параметры пагинации");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));
        List<ItemRequest> requests = itemRequestRepository.findAllByRequesterIdNot(userId, pageable);
        log.debug("Найдено {} запросов других пользователей", requests.size());

        return requests.stream()
                .map(this::toResponseDtoWithItems)
                .collect(Collectors.toList());
    }

    @Override
    public ItemRequestResponseDto getRequestById(Long requestId, Long userId) {
        log.info("Запрос на получение запроса ID: {} пользователем ID: {}", requestId, userId);

        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Запрос с ID {} не найден", requestId);
                    return new NotFoundException("Запрос не найден");
                });

        return toResponseDtoWithItems(itemRequest);
    }

    private ItemRequestResponseDto toResponseDtoWithItems(ItemRequest itemRequest) {
        ItemRequestResponseDto dto = ItemRequestMapper.toResponseDto(itemRequest);
        List<Item> items = itemRepository.findByRequestId(itemRequest.getId());
        dto.setItems(items.stream()
                .map(ItemMapper::toDto)
                .collect(Collectors.toList()));
        return dto;
    }
}