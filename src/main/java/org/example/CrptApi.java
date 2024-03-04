package org.example;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;


public class CrptApi {
    private final TimeUnit timeUnit;

    private final int requestLimit;

    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
    }

    static class DocumentDTO implements Serializable{
        private String description;
        private String docId;
        // .... необходимые поля и методы
    }

    public void createDocument(DocumentDTO document, String signature) {
        try {
            semaphore.acquire();
            sendPostRequest(document, signature);
            semaphore.release();
        } catch (InterruptedException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private void sendPostRequest(DocumentDTO document, String signature) throws JsonProcessingException {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode jsonBody = mapper.createObjectNode();
        jsonBody.set("document", mapper.valueToTree(document));
        jsonBody.put("signature", signature);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(jsonBody)))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Successfully post document!");

            } else {
                throw new RuntimeException("Error creating document: " + response.statusCode());
            }

            String responseBody = response.body();
            Object responseObject = new ObjectMapper().readValue(responseBody, Object.class);
            // Дополнительная обработка ответа
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}