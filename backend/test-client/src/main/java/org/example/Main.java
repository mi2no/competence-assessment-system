package org.example;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        String content;
        try (var in = Main.class.getResourceAsStream("/server.c")) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        Jsonb jsonb = JsonbBuilder.create();

        String json = jsonb.toJson(
                Map.of("content", content)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello/api/competence/extract"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("Status: " + response.statusCode());
        System.out.println("{" + response.body() + "}");
    }
}