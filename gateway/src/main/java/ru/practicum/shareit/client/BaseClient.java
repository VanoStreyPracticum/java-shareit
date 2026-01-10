package ru.practicum.shareit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class BaseClient {
    protected final RestTemplate rest;
    private final ObjectMapper objectMapper;

    public BaseClient(RestTemplate rest, ObjectMapper objectMapper) {
        this.rest = rest;
        this.objectMapper = objectMapper;
    }

    protected ResponseEntity<Object> get(String path) {
        return get(path, null, null);
    }

    protected ResponseEntity<Object> get(String path, Long userId) {
        return get(path, userId, null);
    }

    protected ResponseEntity<Object> get(String path, Long userId, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.GET, path, userId, parameters, null);
    }

    protected ResponseEntity<Object> post(String path, Object body) {
        return post(path, null, null, body);
    }

    protected ResponseEntity<Object> post(String path, Long userId, Object body) {
        return post(path, userId, null, body);
    }

    protected ResponseEntity<Object> post(String path, Long userId, @Nullable Map<String, Object> parameters, Object body) {
        return makeAndSendRequest(HttpMethod.POST, path, userId, parameters, body);
    }

    protected ResponseEntity<Object> patch(String path, Object body) {
        return patch(path, null, null, body);
    }

    protected ResponseEntity<Object> patch(String path, Long userId, Object body) {
        return patch(path, userId, null, body);
    }

    protected ResponseEntity<Object> patch(String path, Long userId, @Nullable Map<String, Object> parameters, Object body) {
        return makeAndSendRequest(HttpMethod.PATCH, path, userId, parameters, body);
    }

    protected ResponseEntity<Object> delete(String path) {
        return delete(path, null, null);
    }

    protected ResponseEntity<Object> delete(String path, Long userId) {
        return delete(path, userId, null);
    }

    protected ResponseEntity<Object> delete(String path, Long userId, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.DELETE, path, userId, parameters, null);
    }

    private ResponseEntity<Object> makeAndSendRequest(HttpMethod method, String path, Long userId,
                                                      @Nullable Map<String, Object> parameters, @Nullable Object body) {
        HttpEntity<Object> requestEntity = new HttpEntity<>(body, createHeaders(userId));

        ResponseEntity<Object> shareitServerResponse;
        try {
            if (parameters != null && !parameters.isEmpty()) {
                shareitServerResponse = rest.exchange(path, method, requestEntity, Object.class, parameters);
            } else {
                shareitServerResponse = rest.exchange(path, method, requestEntity, Object.class);
            }
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        }

        return prepareGatewayResponse(shareitServerResponse);
    }

    private HttpHeaders createHeaders(@Nullable Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (userId != null) {
            headers.set("X-Sharer-User-Id", String.valueOf(userId));
        }
        return headers;
    }

    private ResponseEntity<Object> prepareGatewayResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            Object body = response.getBody();
            if (body != null) {
                try {
                    String json = objectMapper.writeValueAsString(body);
                    Object processedBody = objectMapper.readValue(json, Object.class);
                    return ResponseEntity.status(response.getStatusCode())
                            .headers(response.getHeaders())
                            .body(processedBody);
                } catch (Exception e) {
                    return ResponseEntity.status(response.getStatusCode())
                            .headers(response.getHeaders())
                            .body(body);
                }
            }
            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(body);
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());

        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }

        return responseBuilder.build();
    }
}