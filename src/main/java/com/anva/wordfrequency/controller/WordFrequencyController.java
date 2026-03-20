package com.anva.wordfrequency.controller;

import com.anva.wordfrequency.domain.AnalysisRecord;
import com.anva.wordfrequency.domain.WordFrequency;
import com.anva.wordfrequency.service.AnalysisHistoryService;
import com.anva.wordfrequency.service.WordFrequencyAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;

import org.springframework.web.bind.annotation.RequestMethod;

@RestController
@RequestMapping("/api/v1/word-frequency")
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE,
        RequestMethod.OPTIONS })
public class WordFrequencyController {

    private static final Logger logger = LoggerFactory.getLogger(WordFrequencyController.class);

    private final WordFrequencyAnalyzer analyzer;
    private final AnalysisHistoryService historyService;

    @Autowired
    public WordFrequencyController(WordFrequencyAnalyzer analyzer, AnalysisHistoryService historyService) {
        this.analyzer = analyzer;
        this.historyService = historyService;
    }

    // A simple Data Transfer Object to receive JSON text from the frontend
    @Data
    public static class AnalysisRequestPayload {
        private String text;
        private String word;
        private int n;
    }

    @PostMapping("/highest")
    public ResponseEntity<AnalysisRecord> calculateHighest(@RequestBody AnalysisRequestPayload request) {
        logger.info("Received request for /highest with text length: {}",
                request.getText() != null ? request.getText().length() : 0);
        int highest = analyzer.calculateHighestFrequency(request.getText());

        String topWord = "";
        try {
            List<WordFrequency> topList = analyzer.calculateMostFrequentNWords(request.getText(), 1);
            if (!topList.isEmpty()) {
                topWord = topList.get(0).getWord();
            }
        } catch (Exception e) {
            // ignore if no words
        }

        // Save to MongoDB
        AnalysisRecord record = new AnalysisRecord();
        record.setText(request.getText());
        if (!topWord.isEmpty()) {
            record.setOperation("calculateHighestFrequency (Top word: " + topWord + ")");
        } else {
            record.setOperation("calculateHighestFrequency");
        }
        record.setSingleResult(highest);

        AnalysisRecord saved = historyService.store(record);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/frequency-for-word")
    public ResponseEntity<AnalysisRecord> calculateFrequencyForWord(@RequestBody AnalysisRequestPayload request) {
        logger.info("Received request for /frequency-for-word for target word: '{}'", request.getWord());
        int frequency = analyzer.calculateFrequencyForWord(request.getText(), request.getWord());

        // Save to MongoDB
        AnalysisRecord record = new AnalysisRecord();
        record.setText(request.getText());
        record.setOperation("calculateFrequencyForWord (" + request.getWord() + ")");
        record.setSingleResult(frequency);

        AnalysisRecord saved = historyService.store(record);

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/most-frequent-n")
    public ResponseEntity<AnalysisRecord> calculateMostFrequentNWords(@RequestBody AnalysisRequestPayload request) {
        logger.info("Received request for /most-frequent-n for top {} words", request.getN());
        List<WordFrequency> words = analyzer.calculateMostFrequentNWords(request.getText(), request.getN());

        // Save to MongoDB
        AnalysisRecord record = new AnalysisRecord();
        record.setText(request.getText());
        record.setOperation("calculateMostFrequentNWords (" + request.getN() + ")");

        // We cast the List<WordFrequency> safely to our implementation for MongoDB
        // storage
        List<com.anva.wordfrequency.domain.WordFrequencyImpl> concreteList = new java.util.ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            WordFrequency w = words.get(i);
            concreteList.add(new com.anva.wordfrequency.domain.WordFrequencyImpl(w.getWord(), w.getFrequency()));
        }
        record.setResults(concreteList);

        AnalysisRecord saved = historyService.store(record);

        return ResponseEntity.ok(saved);
    }

    // --- Basic CRUD operations for the frontend to manage history ---

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisRecord>> getHistory() {
        return ResponseEntity.ok(historyService.findAll());
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteHistoryItem(@PathVariable String id) {
        historyService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> deleteAllHistory() {
        historyService.deleteAll();
        return ResponseEntity.ok().build();
    }
}
