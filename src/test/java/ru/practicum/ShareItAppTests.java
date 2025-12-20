package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.shareit.ShareItApp;

@SpringBootTest(classes = ShareItApp.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ShareItAppTests {

    @Test
    void contextLoads() {
    }
}