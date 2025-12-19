package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        validateUser(userDto);
        
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new ConflictException("Email уже зарегистрирован");
        }
        
        User user = UserMapper.toUser(userDto);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }
    
    @Override
    public UserDto getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        return UserMapper.toUserDto(user);
    }
    
    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        
        if (userDto.getEmail() != null) {
            userRepository.findByEmail(userDto.getEmail())
                    .ifPresent(userWithEmail -> {
                        if (!userWithEmail.getId().equals(userId)) {
                            throw new ConflictException("Email уже зарегистрирован");
                        }
                    });
            existingUser.setEmail(userDto.getEmail());
        }
        
        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }
        
        User updatedUser = userRepository.save(existingUser);
        return UserMapper.toUserDto(updatedUser);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(userId);
    }
    
    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }
    
    private void validateUser(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
        if (userDto.getName() == null || userDto.getName().isBlank()) {
            throw new ValidationException("Имя не может быть пустым");
        }
    }
}