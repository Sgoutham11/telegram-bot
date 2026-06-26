package com.telegram.bot.interceptor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LogManager.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        logRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        BufferingClientHttpResponseWrapper bufferedResponse = new BufferingClientHttpResponseWrapper(response);
        logResponse(bufferedResponse);
        return bufferedResponse;
    }

    private void logRequest(HttpRequest request, byte[] body) throws IOException {

        logger.info("Request URI: {}", request.getURI());
        logger.info("Request Method: {}", request.getMethod());
        logger.info("Request Headers: {}", request.getHeaders());
        logger.info("Request Body: {}", new String(body, StandardCharsets.UTF_8));
    }

    private void logResponse(ClientHttpResponse response) throws IOException {

        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        logger.info("Response Status code: {}", response.getStatusCode());
        logger.info("Response Headers: {}", response.getHeaders());
        logger.info("Response Body: {}", responseBody);
    }
}
