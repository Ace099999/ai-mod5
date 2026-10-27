package de.example.aichat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Schlanker HTTP-Client für OpenAI-kompatible APIs und die Anthropic-API. */
public class AiClient {
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();

	public static CompletableFuture<String> ask(AiConfig cfg, String question) {
		boolean anthropic = "anthropic".equalsIgnoreCase(cfg.provider);

		JsonObject body = new JsonObject();
		body.addProperty("model", cfg.model);
		body.addProperty("max_tokens", cfg.maxTokens);

		JsonArray messages = new JsonArray();
		if (anthropic) {
			body.addProperty("system", cfg.systemPrompt);
		} else {
			messages.add(message("system", cfg.systemPrompt));
		}
		messages.add(message("user", question));
		body.add("messages", messages);

		String url = cfg.apiUrl;
		if (anthropic && url.contains("openai.com")) {
			url = "https://api.anthropic.com/v1/messages";
		}

		HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(cfg.timeoutSeconds))
				.header("Content-Type", "application/json");
		if (anthropic) {
			req.header("x-api-key", cfg.apiKey).header("anthropic-version", "2023-06-01");
		} else {
			req.header("Authorization", "Bearer " + cfg.apiKey);
		}
		req.POST(HttpRequest.BodyPublishers.ofString(body.toString()));

		return HTTP.sendAsync(req.build(), HttpResponse.BodyHandlers.ofString())
				.thenApply(res -> {
					if (res.statusCode() / 100 != 2) {
						throw new RuntimeException("HTTP " + res.statusCode() + ": " + shorten(res.body()));
					}
					return parse(res.body(), anthropic);
				});
	}

	private static JsonObject message(String role, String content) {
		JsonObject o = new JsonObject();
		o.addProperty("role", role);
		o.addProperty("content", content);
		return o;
	}

	private static String parse(String json, boolean anthropic) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		if (anthropic) {
			return root.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().trim();
		}
		return root.getAsJsonArray("choices").get(0).getAsJsonObject()
				.getAsJsonObject("message").get("content").getAsString().trim();
	}

	private static String shorten(String s) {
		return s.length() > 200 ? s.substring(0, 200) + "..." : s;
	}
}
