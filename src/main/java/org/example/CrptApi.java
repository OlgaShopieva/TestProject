package org.example;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class CrptApi {

    public static void main(String[] args) throws JsonProcessingException {
        TimeUnit timeUnit = new TimeUnit(1, ChronoUnit.SECONDS);
        int requestLimit = 30;
        CrptApi crptApi = new CrptApi(timeUnit, requestLimit);

        Object document = "someDocument";
        String signature = "someSignature";
        crptApi.createDocument(document, signature);
    }

    private final Lock lock = new ReentrantLock();
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private int requestCount = 0;
    private LocalDateTime lastRequestTime = LocalDateTime.now();

    private record TimeUnit(int timeValue, ChronoUnit unit) {
        public long toTimeUnit() {
            return unit.getDuration().multipliedBy(timeValue).getSeconds();
        }
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public void createDocument(Object document, String signature) throws JsonProcessingException {
        lock.lock();
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            if (currentTime.isAfter(lastRequestTime.plusSeconds(timeUnit.toTimeUnit()))){
                requestCount = 0;
                lastRequestTime = currentTime;
            }

            if (requestCount < requestLimit) {
                sendPostRequest(document, signature);
                requestCount++;
            } else {
                try {
                    Thread.sleep(1000);
                    createDocument(document, signature);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void sendPostRequest(Object document, String signature) throws JsonProcessingException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(document)))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ошибка отправки запроса : " + response.statusCode());
            }

            String responseBody = response.body();
            Object responseObject = new ObjectMapper().readValue(responseBody, Object.class);
            // Дополнительная обработка ответа
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}