# Word Frequency Microservice

Spring Boot service for word-frequency analysis with MongoDB-backed history storage and a small browser UI.

## Technical Stack
- Java 25 LTS
- Spring Boot 4.0.3
- Spring Data MongoDB
- Vanilla HTML/CSS/JS
- Docker and Docker Compose v2
- JUnit 5, Mockito, AssertJ, Testcontainers

## How to Run

### Docker
Fastest standalone path for a new user. This builds the app image and runs the API immediately, with no local Java setup required.

```bash
docker build -t word-frequency-service .
docker run --rm -p 8080:8080 word-frequency-service
```

Then open [http://localhost:8080](http://localhost:8080).

In this standalone mode, the analysis APIs work normally and history storage is skipped.

If you also want MongoDB-backed history, start the full stack with Docker Compose:

```bash
docker compose up --build
```

Then open [http://localhost:7429](http://localhost:7429).

### Local
1. Install Java 25 LTS.
2. Start MongoDB on `localhost:27017`.
3. Run:

```bash
APP_HISTORY_ENABLED=true ./mvnw spring-boot:run
```

If you only want to run the app without MongoDB-backed history:

```bash
./mvnw spring-boot:run
```

## Core Design

### Algorithm Notes
- Text is normalized with `toLowerCase()` and tokenized by `[^a-z]+`.
- Word counts are built with a `HashMap` via `groupingBy(..., summingInt(...))`.
- `calculateHighestFrequency` scans the frequency map once: `O(K)`.
- `calculateFrequencyForWord` is an `O(1)` lookup after map construction.
- `calculateMostFrequentNWords` uses a min-heap of size `N`, giving `O(K log N)` instead of sorting all unique words with `O(K log K)`.

### Service Behavior
- `WordFrequencyAnalyzerImpl` is stateless, so the singleton Spring service is thread-safe.
- When `APP_HISTORY_ENABLED=true`, each API call stores an `AnalysisRecord` in MongoDB.
- Without MongoDB, the API still runs and returns analysis results; history endpoints fall back to an empty list/no-op deletes.
- The current implementation optimizes for clarity and correctness over large-input streaming or request-level caching.

## Testing

Run all tests with:

```bash
./mvnw clean test
```

Test layers:
- Unit tests: verify algorithm correctness in isolation.
- Controller tests: verify request mapping and response payloads with mocked dependencies.
- Integration tests: verify the full HTTP-to-MongoDB flow with Testcontainers and a real `mongo:8.2` instance.
- **Performance tests**: verify Big-O scaling requirements. The suite includes a rigorous load test validating that processing 1,000,000 words completes structurally within milliseconds ($O(n)$ map building and $O(k \log n)$ priority queue).
