package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Запрос на создание пользователя с email: {}, имя: {}",
                userDto.getEmail(), userDto.getName());

        validateUser(userDto);

        log.debug("Проверка уникальности email: {}", userDto.getEmail());
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            log.warn("Попытка создания пользователя с уже существующим email: {}", userDto.getEmail());
            throw new ConflictException("Email уже зарегистрирован");
        }

        log.debug("Создание объекта пользователя из DTO");
        User user = UserMapper.toUser(userDto);

        log.debug("Сохранение пользователя в базе данных");
        User savedUser = userRepository.save(user);

        log.info("Пользователь успешно создан с ID: {}, email: {}",
                savedUser.getId(), savedUser.getEmail());

        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public UserDto getUser(Long userId) {
        log.debug("Запрос на получение пользователя ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        log.info("Пользователь ID: {} успешно получен", userId);
        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserDto userDto) {
        log.info("Запрос на обновление пользователя ID: {}", userId);

        log.debug("Поиск существующего пользователя ID: {}", userId);
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с ID {} не найден при попытке обновления", userId);
                    return new NotFoundException("Пользователь не найден");
                });

        boolean hasUpdates = false;

        if (userDto.getEmail() != null) {
            log.debug("Проверка нового email: {}", userDto.getEmail());
            userRepository.findByEmail(userDto.getEmail())
                    .ifPresent(userWithEmail -> {
                        if (!userWithEmail.getId().equals(userId)) {
                            log.warn("Попытка обновления email на уже существующий: {}", userDto.getEmail());
                            throw new ConflictException("Email уже зарегистрирован");
                        }
                    });

            if (!existingUser.getEmail().equals(userDto.getEmail())) {
                log.debug("Обновление email пользователя ID: {} с '{}' на '{}'",
                        userId, existingUser.getEmail(), userDto.getEmail());
                existingUser.setEmail(userDto.getEmail());
                hasUpdates = true;
            }
        }

        if (userDto.getName() != null && !userDto.getName().equals(existingUser.getName())) {
            log.debug("Обновление имени пользователя ID: {} с '{}' на '{}'",
                    userId, existingUser.getName(), userDto.getName());
            existingUser.setName(userDto.getName());
            hasUpdates = true;
        }

        if (!hasUpdates) {
            log.warn("Запрос на обновление пользователя ID: {} не содержит изменений", userId);
        }

        log.debug("Сохранение обновленного пользователя ID: {}", userId);
        User updatedUser = userRepository.save(existingUser);

        log.info("Пользователь ID: {} успешно обновлен", userId);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Запрос на удаление пользователя ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("Попытка удаления несуществующего пользователя ID: {}", userId);
            throw new NotFoundException("Пользователь не найден");
        }

        userRepository.deleteById(userId);
        log.info("Пользователь ID: {} успешно удален", userId);
    }

    @Override
    public List<UserDto> getAllUsers() {
        log.info("Запрос на получение всех пользователей");

        List<User> users = userRepository.findAll();
        log.debug("Найдено {} пользователей в базе данных", users.size());

        List<UserDto> result = users.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());

        log.info("Возвращено {} пользователей", result.size());
        return result;
    }

    private void validateUser(UserDto userDto) {
        log.trace("Валидация данных пользователя");

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            log.warn("Попытка создания пользователя с пустым email");
            throw new ValidationException("Email не может быть пустым");
        }

        if (userDto.getName() == null || userDto.getName().isBlank()) {
            log.warn("Попытка создания пользователя с пустым именем");
            throw new ValidationException("Имя не может быть пустым");
        }

        log.trace("Данные пользователя прошли валидацию");
    }
}