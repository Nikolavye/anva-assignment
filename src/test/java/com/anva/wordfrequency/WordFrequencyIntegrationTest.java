package com.anva.wordfrequency;

import com.anva.wordfrequency.domain.AnalysisRecord;
import com.anva.wordfrequency.repository.AnalysisRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class WordFrequencyIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.2");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("app.history.enabled", () -> "true");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AnalysisRecordRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testCalculateHighestFrequency() {
        Map<String, Object> payload = Map.of("text", "The sun shines and the sun is bright");

        ResponseEntity<AnalysisRecord> response = restTemplate.postForEntity(
                "/api/v1/word-frequency/highest", payload, AnalysisRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSingleResult()).isEqualTo(2);
        // Verify it was persisted to MongoDB
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void testCalculateFrequencyForWord() {
        Map<String, Object> payload = Map.of(
                "text", "Hello hello HELLO world",
                "word", "hello"
        );

        ResponseEntity<AnalysisRecord> response = restTemplate.postForEntity(
                "/api/v1/word-frequency/frequency-for-word", payload, AnalysisRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSingleResult()).isEqualTo(3);
    }

    @Test
    void testCalculateFrequencyForWordNotFound() {
        Map<String, Object> payload = Map.of(
                "text", "Hello world",
                "word", "missing"
        );

        ResponseEntity<AnalysisRecord> response = restTemplate.postForEntity(
                "/api/v1/word-frequency/frequency-for-word", payload, AnalysisRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSingleResult()).isEqualTo(0);
    }

    @Test
    void testCalculateMostFrequentNWords() {
        Map<String, Object> payload = Map.of(
                "text", "The cat walks over the staircase",
                "n", 3
        );

        ResponseEntity<AnalysisRecord> response = restTemplate.postForEntity(
                "/api/v1/word-frequency/most-frequent-n", payload, AnalysisRecord.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResults()).hasSize(3);
        // "the" has highest frequency (2)
        assertThat(response.getBody().getResults().get(0).getWord()).isEqualTo("the");
        assertThat(response.getBody().getResults().get(0).getFrequency()).isEqualTo(2);
        // Remaining words (frequency 1) sorted alphabetically: "cat" before "over"
        assertThat(response.getBody().getResults().get(1).getWord()).isEqualTo("cat");
        assertThat(response.getBody().getResults().get(2).getWord()).isEqualTo("over");
    }

    @Test
    void testHistoryEndpoint() {
        // Create two analysis records
        restTemplate.postForEntity("/api/v1/word-frequency/highest",
                Map.of("text", "hello world"), AnalysisRecord.class);
        restTemplate.postForEntity("/api/v1/word-frequency/highest",
                Map.of("text", "foo bar baz"), AnalysisRecord.class);

        // Verify history returns both
        ResponseEntity<List> historyResponse = restTemplate.getForEntity(
                "/api/v1/word-frequency/history", List.class);

        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).hasSize(2);
    }

    @Test
    void testDeleteHistoryItem() {
        // Create a record
        ResponseEntity<AnalysisRecord> createResponse = restTemplate.postForEntity(
                "/api/v1/word-frequency/highest",
                Map.of("text", "hello hello world"), AnalysisRecord.class);

        String id = createResponse.getBody().getId();
        assertThat(repository.findAll()).hasSize(1);

        // Delete it
        restTemplate.delete("/api/v1/word-frequency/history/" + id);
        assertThat(repository.findAll()).isEmpty();
    }
}
