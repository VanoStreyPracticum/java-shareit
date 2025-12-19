package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.shareit.item.comment.CommentDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @NotBlank(message = "Описание не может быть пустым")
    private String description;

    @NotNull(message = "Поле available обязательно")
    private Boolean available;

    private Long ownerId;
    private Long requestId;
    private BookingInfo lastBooking;
    private BookingInfo nextBooking;
    private List<CommentDto> comments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingInfo {
        private Long id;
        private Long bookerId;
    }
}