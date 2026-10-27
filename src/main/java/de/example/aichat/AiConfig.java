package de.example.aichat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Konfiguration: config/ai-chat.json */
public class AiConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ai-chat.json");

	/** "openai" (auch für Groq, OpenRouter, Ollama, LM Studio ...) oder "anthropic" */
	public String provider = "openai";
	public String apiUrl = "https://api.openai.com/v1/chat/completions";
	public String apiKey = "DEIN_API_KEY";
	public String model = "gpt-4o-mini";
	public int maxTokens = 400;
	public int timeoutSeconds = 60;
	public int cooldownSeconds = 5;
	public String systemPrompt = "Du bist ein hilfreicher Assistent in Minecraft. "
			+ "Antworte kurz (maximal 3-4 Sätze), ohne Markdown und in der Sprache der Frage.";

	public static AiConfig load() {
		try {
			if (Files.exists(PATH)) {
				AiConfig cfg = GSON.fromJson(Files.readString(PATH), AiConfig.class);
				if (cfg != null) return cfg;
			}
			AiConfig cfg = new AiConfig();
			Files.writeString(PATH, GSON.toJson(cfg));
			return cfg;
		} catch (Exception e) {
			AiChatMod.LOGGER.error("Konfiguration konnte nicht geladen werden", e);
			return new AiConfig();
		}
	}
}
