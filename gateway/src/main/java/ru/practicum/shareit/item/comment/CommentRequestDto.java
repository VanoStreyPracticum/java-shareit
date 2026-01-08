package ru.practicum.shareit.item.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    @NotBlank(message = "Текст комментария не может быть пустым")
    private String text;
}