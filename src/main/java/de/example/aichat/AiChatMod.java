package de.example.aichat;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AiChatMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("ai-chat");

	private static AiConfig config;
	private static final Map<String, Long> LAST_USE = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		config = AiConfig.load();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("ai")
						.then(Commands.argument("frage", StringArgumentType.greedyString())
								.executes(ctx -> {
									handle(ctx.getSource(), StringArgumentType.getString(ctx, "frage"));
									return 1;
								}))));

		LOGGER.info("AI Chat geladen. Trage deinen API-Key in config/ai-chat.json ein.");
	}

	private static void handle(CommandSourceStack source, String question) {
		if (config.apiKey == null || config.apiKey.isBlank() || config.apiKey.equals("DEIN_API_KEY")) {
			source.sendFailure(Component.literal("Kein API-Key gesetzt! Trage ihn in config/ai-chat.json ein und starte neu."));
			return;
		}

		// Cooldown pro Spieler
		String key = source.getTextName();
		long now = System.currentTimeMillis();
		Long last = LAST_USE.get(key);
		if (last != null && now - last < config.cooldownSeconds * 1000L) {
			source.sendFailure(Component.literal("Bitte warte kurz, bevor du die KI erneut fragst."));
			return;
		}
		LAST_USE.put(key, now);

		source.sendSuccess(() -> Component.literal("[KI] denkt nach...").withStyle(ChatFormatting.GRAY), false);

		AiClient.ask(config, question).whenComplete((answer, error) ->
				// Zurück auf den Server-Thread, bevor Chat-Nachrichten gesendet werden
				source.getServer().execute(() -> {
					if (error != null) {
						Throwable cause = error.getCause() != null ? error.getCause() : error;
						LOGGER.error("KI-Anfrage fehlgeschlagen", cause);
						source.sendFailure(Component.literal("[KI] Fehler: " + cause.getMessage()));
						return;
					}
					String clean = answer.replace("**", "").replace("`", "");
					for (String line : clean.split("\\R")) {
						if (line.isBlank()) continue;
						source.sendSuccess(() -> Component.literal("[KI] ").withStyle(ChatFormatting.AQUA)
								.append(Component.literal(line).withStyle(ChatFormatting.WHITE)), false);
					}
				}));
	}
}
