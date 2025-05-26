package com.wizlit.path.utils;

import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.utils.Validator;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
//@RequiredArgsConstructor
public class GoogleService {

    private final WebClient driveClient;
    private final WebClient oauthClient;

    public GoogleService(WebClient.Builder oauthBuilder, WebClient.Builder driveBuilder) {
        this.oauthClient = oauthBuilder
                .baseUrl("https://www.googleapis.com/oauth2/v3")
                .build();
        this.driveClient = driveBuilder
                .baseUrl("https://www.googleapis.com/drive/v3")
                .build();
    }

    public Mono<String> getUserInfo(String accessToken) {
        return oauthClient.get()
                .uri("/userinfo")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorMap(error -> Validator.from(error)
                        .containsAllElseError(new ApiException(ErrorCode.INVALID_TOKEN), "Unauthorized")
                        .toException());
    }
}