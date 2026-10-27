# AI Chat (Fabric, Minecraft 26.3)

`/ai <Frage>` schickt die Frage an eine KI; die Antwort erscheint im Chat.

## Bauen
Empfohlen: Template auf https://fabricmc.net/develop/template erzeugen (26.3, Mojang-Mappings/unobfuscated),
dann `src/` und die Gradle-Dateien dieses Ordners darüber kopieren. Danach:

    ./gradlew build        # Jar liegt in build/libs/ai-chat-1.0.0.jar

Benötigt JDK 25.

## Einrichten
Mod + Fabric API in den mods-Ordner, Spiel einmal starten, dann `config/ai-chat.json` bearbeiten:
- provider: "openai" (auch Groq, OpenRouter, Ollama: apiUrl anpassen) oder "anthropic"
- apiKey, model eintragen, Spiel neu starten.
