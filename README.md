# LLM CLI Agent

A command-line interface application for interacting with Large Language Models (LLMs) built with Kotlin, following clean architecture principles.

## 🏗️ Architecture

The project follows a **3-layer clean architecture**:

```
src/main/kotlin/
├── domain/          # Business logic (entities, use cases, repository interfaces)
├── data/            # Data layer (API clients, config, repository implementations)
└── ui/              # Presentation layer (Kotter terminal UI)
```

### Key Components

- **Domain Layer**: Pure business logic with no external dependencies
  - `ChatMessage`, `ChatSession` - Core entities
  - `ChatRepository` - Repository interface
  - `SendMessageUseCase`, `StartSessionUseCase` - Business operations

- **Data Layer**: External integrations and data management
  - Multiple LLM provider support (OpenAI, Ollama, Custom)
  - Configuration management via `config.properties`
  - Optional history persistence (JSON or in-memory)

- **UI Layer**: Terminal interface using Kotter
  - State machine-based navigation
  - Real-time chat rendering with colors
  - Loading states and error handling

## 🚀 Features

- ✅ **Multi-provider support**: OpenAI, Ollama, or custom LLM APIs
- ✅ **System prompts**: Configure assistant behavior at session start
- ✅ **Chat history**: Optional persistence to JSON files
- ✅ **Beautiful terminal UI**: Powered by Kotter with colors and formatting
- ✅ **Clean architecture**: Testable, maintainable, scalable
- ✅ **Async operations**: Non-blocking HTTP requests with Kotlin coroutines
- ✅ **Graceful shutdown**: Proper resource cleanup

## 📦 Installation

### Prerequisites

- JDK 11 or higher
- Gradle 7.0+

### Build

```bash
./gradlew build
```

### Run

```bash
./gradlew run
```

Or build a fat JAR:

```bash
./gradlew shadowJar
java -jar build/libs/llm-cli-agent.jar
```

## ⚙️ Configuration

Edit `src/main/resources/config.properties`:

```properties
# Provider: OPENAI, OLLAMA, or CUSTOM
llm.provider=OPENAI
llm.baseUrl=https://api.openai.com
llm.model=gpt-3.5-turbo
llm.apiKey=your-api-key-here
llm.timeout=30000

# History persistence
persist.enabled=false
persist.path=./history
```

### Provider Examples

**OpenAI:**
```properties
llm.provider=OPENAI
llm.baseUrl=https://api.openai.com
llm.model=gpt-4
llm.apiKey=sk-...
```

**Ollama (local):**
```properties
llm.provider=OLLAMA
llm.baseUrl=http://localhost:11434
llm.model=llama2
llm.apiKey=
```

**Custom API:**
```properties
llm.provider=CUSTOM
llm.baseUrl=https://your-api.com/chat
llm.model=your-model
llm.apiKey=your-key
```

## 🎮 Usage

1. **Start the application**
   ```bash
   ./gradlew run
   ```

2. **Welcome screen** - Press ENTER to continue

3. **System prompt** - Enter a system prompt or leave blank for default

4. **Chat** - Start chatting with the LLM
   - Type your message and press ENTER
   - Type `exit`, `quit`, or `bye` to quit

## 📁 Project Structure

```
src/main/kotlin/org/example/
├── domain/
│   ├── model/
│   │   ├── Role.kt
│   │   ├── ChatMessage.kt
│   │   └── ChatSession.kt
│   ├── repository/
│   │   └── ChatRepository.kt
│   └── usecase/
│       ├── SendMessageUseCase.kt
│       └── StartSessionUseCase.kt
├── data/
│   ├── config/
│   │   ├── Config.kt
│   │   └── ConfigLoader.kt
│   ├── api/
│   │   ├── model/
│   │   │   ├── LlmRequest.kt
│   │   │   └── LlmResponse.kt
│   │   └── provider/
│   │       ├── LlmProvider.kt
│   │       ├── OpenAiProvider.kt
│   │       ├── OllamaProvider.kt
│   │       └── CustomProvider.kt
│   ├── repository/
│   │   └── ChatRepositoryImpl.kt
│   └── persistence/
│       ├── HistoryStore.kt
│       ├── InMemoryStore.kt
│       └── JsonHistoryStore.kt
├── ui/
│   ├── state/
│   │   └── AppState.kt
│   └── component/
│       └── Helpers.kt
├── di/
│   └── AppContainer.kt
└── Main.kt
```

## 🧪 Testing

```bash
./gradlew test
```

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Kotter 1.1.2 (terminal UI)
- **HTTP Client**: Ktor Client 2.3.12
- **Serialization**: Kotlinx Serialization
- **Coroutines**: Kotlinx Coroutines 1.7.3
- **Logging**: kotlin-logging + SLF4J
- **JSON**: Gson 2.10.1

## 📝 License

MIT License

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Contact

For questions or support, please open an issue on GitHub.
