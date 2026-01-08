package ru.practicum.shareit.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.gateway.user.dto.UserDto;

@FeignClient(name = "user-service", url = "${shareit-server.url}")
public interface UserClient {
    @GetMapping("/{userId}")
    ResponseEntity<Object> getUser(@PathVariable long userId);

    @PostMapping
    ResponseEntity<Object> createUser(@RequestBody UserDto userDto);

    @PatchMapping("/{userId}")
    ResponseEntity<Object> updateUser(@PathVariable long userId, @RequestBody UserDto userDto);

    @DeleteMapping("/{userId}")
    ResponseEntity<Object> deleteUser(@PathVariable long userId);
}