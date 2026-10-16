package com.wizlit.file.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PrivateAccessFilterTest {

    @Test
    @DisplayName("JWT 페이로드에 name, picture 클레임이 누락(null)되어도 ConcurrentHashMap NPE 없이 정상 통과 검증")
    void filter_withMissingNameAndPictureClaims_shouldNotThrowNpe() throws NoSuchMethodException {
        // Given
        RequestMappingHandlerMapping handlerMapping = Mockito.mock(RequestMappingHandlerMapping.class);
        WebFilterChain chain = Mockito.mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        Method sampleMethod = this.getClass().getDeclaredMethod("sampleHandler");
        HandlerMethod handlerMethod = new HandlerMethod(this, sampleMethod);
        when(handlerMapping.getHandler(any())).thenReturn(Mono.just(handlerMethod));

        PrivateAccessFilter filter = new PrivateAccessFilter(
                handlerMapping,
                "admin@wizlit.com,*",
                false,
                null
        );

        // JWT token with email and exp, but without name and picture
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"email\":\"admin@wizlit.com\",\"exp\":9999999999}".getBytes()
        );
        String mockJwt = header + "." + payload + ".dummySig";

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/file?uploader=2")
                .header("Authorization", "Bearer " + mockJwt)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getAttributes().get("email")).isEqualTo("admin@wizlit.com");
        assertThat(exchange.getAttributes().get("name")).isNull();
        assertThat(exchange.getAttributes().get("avatar")).isNull();
        assertThat(exchange.getAttributes().get("token")).isEqualTo(mockJwt);
    }

    @Test
    @DisplayName("핸들러 매핑이 비어있는 비매핑/정적 경로 요청 시 체인에 위임되어 통과 검증")
    void filter_withUnmappedHandler_shouldDelegateToChain() {
        // Given
        RequestMappingHandlerMapping handlerMapping = Mockito.mock(RequestMappingHandlerMapping.class);
        WebFilterChain chain = Mockito.mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        when(handlerMapping.getHandler(any())).thenReturn(Mono.empty());

        PrivateAccessFilter filter = new PrivateAccessFilter(
                handlerMapping,
                "admin@wizlit.com,*",
                false,
                null
        );

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/files/testId").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        Mockito.verify(chain).filter(exchange);
    }

    @PrivateAccess
    public void sampleHandler() {
    }
}
