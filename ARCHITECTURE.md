# 🏗️ Architecture Documentation - LLM CLI Agent

## Overview

This document describes the complete architecture of the LLM CLI Agent, a terminal-based application for interacting with Large Language Models built with Kotlin following clean architecture principles.

## Architecture Layers

The application follows a **3-layer clean architecture** pattern:

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer (Kotter)                   │
│  - Terminal rendering, user input, state management    │
└─────────────────────┬───────────────────────────────────┘
                      │ depends on
                      ▼
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                         │
│  - Business logic, entities, use cases, interfaces     │
└─────────────────────┬───────────────────────────────────┘
                      │ depends on
                      ▼
┌─────────────────────────────────────────────────────────┐
│                     Data Layer                          │
│  - API clients, config, persistence, implementations   │
└─────────────────────────────────────────────────────────┘
```

### Dependency Rule

- **UI Layer** depends on **Domain Layer**
- **Domain Layer** is independent (no external dependencies)
- **Data Layer** implements **Domain Layer** interfaces

## Layer Details

### 1. Domain Layer (`domain/`)

**Purpose**: Contains pure business logic with no external dependencies.

#### Entities (`domain/model/`)

```kotlin
// Role.kt - Message role enumeration
enum class Role { USER, ASSISTANT, SYSTEM }

// ChatMessage.kt - Single message entity
data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val timestamp: Long
)

// ChatSession.kt - Chat session aggregate
data class ChatSession(
    val id: String,
    val systemPrompt: String,
    val messages: MutableList<ChatMessage>,
    val createdAt: Long
)
```

#### Repository Interfaces (`domain/repository/`)

```kotlin
// ChatRepository.kt - Repository contract
interface ChatRepository {
    suspend fun sendMessage(session: ChatSession, userMessage: String): Result<ChatMessage>
    suspend fun getHistory(sessionId: String): Result<List<ChatMessage>>
}
```

#### Use Cases (`domain/usecase/`)

```kotlin
// SendMessageUseCase.kt - Send message to LLM
class SendMessageUseCase(
    private val repository: ChatRepository,
    private val historyStore: HistoryStore
) {
    suspend operator fun invoke(session: ChatSession, userMessage: String): Result<Unit>
}

// StartSessionUseCase.kt - Create new chat session
class StartSessionUseCase {
    operator fun invoke(systemPrompt: String): ChatSession
}
```

**Key Principles**:
- No framework dependencies
- Pure Kotlin/JVM
- Testable without mocks
- Business rules encapsulation

---

### 2. Data Layer (`data/`)

**Purpose**: Handles external integrations, API calls, configuration, and persistence.

#### Configuration (`data/config/`)

```kotlin
// Config.kt - Configuration data class
data class Config(
    val baseUrl: String,
    val modelName: String,
    val apiKey: String,
    val provider: String,
    val timeout: Long,
    val persistHistory: Boolean,
    val historyPath: String
)

// ConfigLoader.kt - Loads from config.properties
object ConfigLoader {
    fun load(filePath: String = "config.properties"): Config
}
```

**Configuration File** (`resources/config.properties`):
```properties
llm.provider=OPENAI
llm.baseUrl=https://api.openai.com
llm.model=gpt-3.5-turbo
llm.apiKey=your-api-key-here
llm.timeout=30000
persist.enabled=false
persist.path=./history
```

#### API Layer (`data/api/`)

**DTOs** (`data/api/model/`):
```kotlin
// LlmRequest.kt - API request format
@Serializable
data class LlmRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val temperature: Double = 0.7
)

// LlmResponse.kt - API response format
@Serializable
data class LlmResponse(
    val choices: List<Choice>,
    val usage: Usage?
)
```

**Provider Abstraction** (`data/api/provider/`):

```kotlin
// LlmProvider.kt - Provider interface
interface LlmProvider {
    suspend fun chat(messages: List<ChatMessage>, model: String): LlmResponse
}

// OpenAiProvider.kt - OpenAI implementation
class OpenAiProvider(config: Config, client: HttpClient) : LlmProvider {
    override suspend fun chat(...): LlmResponse {
        // POST to https://api.openai.com/v1/chat/completions
    }
}

// OllamaProvider.kt - Ollama implementation
class OllamaProvider(config: Config, client: HttpClient) : LlmProvider {
    override suspend fun chat(...): LlmResponse {
        // POST to http://localhost:11434/api/chat
    }
}

// CustomProvider.kt - Generic implementation
class CustomProvider(config: Config, client: HttpClient) : LlmProvider
```

**Provider Selection Logic**:
```
Config.provider → "OPENAI" → OpenAiProvider
                → "OLLAMA" → OllamaProvider
                → "CUSTOM" → CustomProvider
```

#### Repository Implementation (`data/repository/`)

```kotlin
// ChatRepositoryImpl.kt
class ChatRepositoryImpl(
    private val provider: LlmProvider,
    private val config: Config
) : ChatRepository {
    override suspend fun sendMessage(session: ChatSession, userMessage: String): Result<ChatMessage> {
        // 1. Add user message to session
        // 2. Prepare messages (system + history)
        // 3. Call LLM provider
        // 4. Extract assistant response
        // 5. Add to session
        // 6. Return result
    }
}
```

#### Persistence Layer (`data/persistence/`)

```kotlin
// HistoryStore.kt - Persistence interface
interface HistoryStore {
    suspend fun save(session: ChatSession)
    suspend fun load(sessionId: String): ChatSession?
    suspend fun listSessions(): List<String>
}

// InMemoryStore.kt - Default (no persistence)
class InMemoryStore : HistoryStore {
    private val sessions = mutableMapOf<String, ChatSession>()
}

// JsonHistoryStore.kt - JSON file persistence
class JsonHistoryStore(historyPath: String) : HistoryStore {
    // Saves to ./history/{sessionId}.json
}
```

---

### 3. UI Layer (`ui/`)

**Purpose**: Terminal interface using Kotter framework.

#### State Management (`ui/state/`)

```kotlin
// AppState.kt - Application state machine
enum class AppState {
    WELCOME,        // Initial welcome screen
    PROMPT_INPUT,   // System prompt input
    CHATTING,       // Main chat loop
    LOADING,        // Processing request
    ERROR           // Error display
}
```

**State Transitions**:
```
WELCOME → PROMPT_INPUT → CHATTING ⇄ LOADING
                              ↓
                           ERROR → CHATTING
```

#### UI Components (`ui/component/`)

```kotlin
// Helpers.kt - Rendering utilities
object Helpers {
    fun RenderScope.renderMessage(message: ChatMessage) {
        // Colored output based on role:
        // USER: cyan
        // ASSISTANT: green
        // SYSTEM: yellow
    }
    
    fun RenderScope.renderHeader(title: String) {
        // Draws header with borders
    }
    
    fun RenderScope.renderError(error: String) {
        // Red error message
    }
    
    fun RenderScope.renderLoading() {
        // Loading indicator
    }
}
```

#### Main Application (`Main.kt`)

```kotlin
fun main() {
    val container = AppContainer()
    
    session {
        var state by liveVarOf(AppState.WELCOME)
        var chatSession by liveVarOf<ChatSession?>(null)
        var errorMsg by liveVarOf<String?>(null)
        var isProcessing by liveVarOf(false)
        
        section {
            when (state) {
                AppState.WELCOME -> { /* render welcome */ }
                AppState.PROMPT_INPUT -> { /* render prompt input */ }
                AppState.CHATTING -> { /* render chat */ }
                AppState.LOADING -> { /* render loading */ }
                AppState.ERROR -> { /* render error */ }
            }
        }.runUntilInputEntered {
            onInputEntered {
                // Handle user input based on current state
            }
        }
    }
}
```

---

## Dependency Injection

### AppContainer (`di/AppContainer.kt`)

Manual dependency injection container that wires all components:

```kotlin
class AppContainer {
    // 1. Load configuration
    val config: Config = ConfigLoader.load()
    
    // 2. Create HTTP client
    val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) { requestTimeoutMillis = config.timeout }
    }
    
    // 3. Select LLM provider
    val llmProvider: LlmProvider = when (config.provider) {
        "OPENAI" -> OpenAiProvider(config, httpClient)
        "OLLAMA" -> OllamaProvider(config, httpClient)
        "CUSTOM" -> CustomProvider(config, httpClient)
    }
    
    // 4. Create persistence store
    val historyStore: HistoryStore = if (config.persistHistory) {
        JsonHistoryStore(config.historyPath)
    } else {
        InMemoryStore()
    }
    
    // 5. Create repository
    val chatRepository: ChatRepository = ChatRepositoryImpl(llmProvider, config)
    
    // 6. Create use cases
    val sendMessageUseCase = SendMessageUseCase(chatRepository, historyStore)
    val startSessionUseCase = StartSessionUseCase()
    
    fun shutdown() {
        httpClient.close()
    }
}
```

**Dependency Graph**:
```
AppContainer
├── Config (from config.properties)
├── HttpClient (Ktor CIO)
├── LlmProvider (OpenAI/Ollama/Custom)
│   └── depends on: Config, HttpClient
├── HistoryStore (InMemory/Json)
│   └── depends on: Config
├── ChatRepository
│   └── depends on: LlmProvider, Config
└── UseCases
    └── depend on: ChatRepository, HistoryStore
```

---

## Data Flow

### Complete Request Flow

```
1. User Input (Terminal)
   ↓
2. Kotter Input Handler (Main.kt)
   ↓
3. SendMessageUseCase
   ↓
4. ChatRepository.sendMessage()
   ↓
5. LlmProvider.chat()
   ↓
6. Ktor HttpClient → LLM API
   ↓
7. LlmResponse (JSON)
   ↓
8. ChatMessage (Domain Entity)
   ↓
9. HistoryStore.save() (if enabled)
   ↓
10. UI Re-render (Kotter)
```

### Example: Sending "Hello" Message

```kotlin
// 1. User types "Hello" in terminal
userInput = "Hello"

// 2. Main.kt handles input
onInputEntered {
    runBlocking {
        // 3. Call use case
        val result = sendMessageUseCase(chatSession, "Hello")
        
        // Inside SendMessageUseCase:
        // 4. Call repository
        repository.sendMessage(session, "Hello")
        
        // Inside ChatRepositoryImpl:
        // 5. Add user message
        session.messages.add(ChatMessage(USER, "Hello"))
        
        // 6. Prepare API request
        val allMessages = [
            ChatMessage(SYSTEM, "You are helpful"),
            ChatMessage(USER, "Hello")
        ]
        
        // 7. Call provider
        val response = provider.chat(allMessages, "gpt-3.5-turbo")
        
        // Inside OpenAiProvider:
        // 8. HTTP POST
        client.post("https://api.openai.com/v1/chat/completions") {
            setBody(LlmRequest(
                model = "gpt-3.5-turbo",
                messages = [
                    LlmMessage("system", "You are helpful"),
                    LlmMessage("user", "Hello")
                ]
            ))
        }
        
        // 9. Parse response
        LlmResponse(choices = [Choice(message = LlmMessage("assistant", "Hi there!"))])
        
        // 10. Create domain entity
        ChatMessage(ASSISTANT, "Hi there!")
        
        // 11. Add to session
        session.messages.add(assistantMessage)
        
        // 12. Persist (if enabled)
        historyStore.save(session)
        
        // 13. Return success
        Result.success(assistantMessage)
    }
}

// 14. UI re-renders with new message
```

---

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **UI** | Kotter 1.1.2 | Terminal UI with state management |
| **HTTP** | Ktor Client 2.3.12 | Async HTTP requests |
| **Serialization** | Kotlinx Serialization | JSON parsing |
| **Concurrency** | Kotlinx Coroutines | Async/await operations |
| **Logging** | kotlin-logging + SLF4J | Structured logging |
| **Persistence** | Gson | JSON file storage |
| **Build** | Gradle 8.14 | Build automation |
| **Language** | Kotlin 1.9.22 | JVM language |

---

## Design Patterns

### 1. **Repository Pattern**
- `ChatRepository` interface in domain
- `ChatRepositoryImpl` in data layer
- Abstracts data source from business logic

### 2. **Strategy Pattern**
- `LlmProvider` interface
- Multiple implementations (OpenAI, Ollama, Custom)
- Runtime selection based on config

### 3. **Use Case Pattern**
- Single responsibility per use case
- Orchestrates business logic
- Testable without UI

### 4. **Dependency Injection**
- Manual DI via `AppContainer`
- Constructor injection
- No framework magic

### 5. **State Machine**
- `AppState` enum
- Clear state transitions
- Predictable UI behavior

---

## Testing Strategy

### Unit Tests
```kotlin
// Domain layer (pure functions)
class SendMessageUseCaseTest {
    @Test
    fun `should add user message to session`() {
        val mockRepo = mock<ChatRepository>()
        val useCase = SendMessageUseCase(mockRepo, InMemoryStore())
        // Test business logic
    }
}
```

### Integration Tests
```kotlin
// Data layer (with test doubles)
class ChatRepositoryImplTest {
    @Test
    fun `should call LLM provider with correct messages`() {
        val mockProvider = mock<LlmProvider>()
        val repo = ChatRepositoryImpl(mockProvider, config)
        // Test integration
    }
}
```

### UI Tests
```kotlin
// UI layer (state transitions)
class AppStateTest {
    @Test
    fun `should transition from WELCOME to PROMPT_INPUT`() {
        // Test state machine
    }
}
```

---

## Error Handling

### Strategy
- Use Kotlin's `Result<T>` type
- Catch exceptions at boundaries
- Propagate errors up the stack
- Display user-friendly messages

### Example
```kotlin
// Repository level
override suspend fun sendMessage(...): Result<ChatMessage> = runCatching {
    // API call
}.onFailure { e ->
    logger.error(e) { "Failed to send message" }
}

// Use case level
suspend operator fun invoke(...): Result<Unit> = runCatching {
    repository.sendMessage(...).getOrThrow()
}

// UI level
if (result.isFailure) {
    errorMsg = result.exceptionOrNull()?.message
    state = AppState.ERROR
}
```

---

## Configuration Management

### Hierarchy
1. **Default values** in `Config` data class
2. **config.properties** file (resources or filesystem)
3. **Environment variables** (future enhancement)

### Example
```properties
# Required
llm.provider=OPENAI
llm.baseUrl=https://api.openai.com
llm.model=gpt-3.5-turbo
llm.apiKey=sk-...

# Optional (with defaults)
llm.timeout=30000
persist.enabled=false
persist.path=./history
```

---

## Extensibility Points

### Adding New LLM Provider
1. Create `NewProvider.kt` implementing `LlmProvider`
2. Add case in `AppContainer` provider selection
3. Update `config.properties` with new provider name

### Adding New Persistence Store
1. Create `NewStore.kt` implementing `HistoryStore`
2. Add case in `AppContainer` store selection
3. Update config with new persistence type

### Adding New UI Screen
1. Add state to `AppState` enum
2. Add rendering logic in `Main.kt` section
3. Add transition logic in `onInputEntered`

---

## Performance Considerations

### HTTP Client
- Connection pooling (Ktor CIO)
- Timeout configuration
- Keep-alive connections

### Memory Management
- In-memory store for default (no disk I/O)
- Lazy loading of history
- Session cleanup on exit

### Concurrency
- Suspend functions for async operations
- `runBlocking` for UI integration
- Single-threaded Kotter rendering

---

## Security Considerations

### API Keys
- Stored in `config.properties`
- Not committed to version control
- Loaded at runtime

### Input Validation
- Trim user input
- Handle empty messages
- Validate config values

### Error Messages
- Don't expose sensitive data
- Log detailed errors
- Show user-friendly messages

---

## Future Enhancements

1. **Streaming Responses**: Real-time token streaming
2. **Multi-session Management**: Switch between sessions
3. **Export/Import**: Save conversations to markdown
4. **Plugins**: Custom LLM providers via plugins
5. **Configuration UI**: Interactive config editor
6. **Retry Logic**: Automatic retry with exponential backoff
7. **Rate Limiting**: Respect API rate limits
8. **Token Counting**: Track usage and costs

---

## Conclusion

This architecture provides:
- ✅ **Separation of Concerns**: Clear layer boundaries
- ✅ **Testability**: Pure domain logic, mockable dependencies
- ✅ **Extensibility**: Easy to add providers, stores, features
- ✅ **Maintainability**: Clean code, documented patterns
- ✅ **Scalability**: Async operations, efficient resource usage

The clean architecture approach ensures the codebase remains maintainable and testable as it grows.
