package com.anva.wordfrequency.service;

import com.anva.wordfrequency.domain.WordFrequency;
import com.anva.wordfrequency.domain.WordFrequencyImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WordFrequencyAnalyzerImpl implements WordFrequencyAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(WordFrequencyAnalyzerImpl.class);

    // Build the histogram in a single pass to avoid lowercasing and splitting the whole text first.
    private Map<String, Integer> buildFrequencyMap(String text) {
        if (text == null || text.isBlank()) {
            logger.debug("buildFrequencyMap received empty text, returning empty map.");
            return Collections.emptyMap();
        }

        Map<String, Integer> frequencies = new HashMap<>();
        StringBuilder token = new StringBuilder(32);

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);

            if (current >= 'A' && current <= 'Z') {
                current = (char) (current + ('a' - 'A'));
            }

            if (current >= 'a' && current <= 'z') {
                token.append(current);
            } else if (token.length() > 0) {
                frequencies.merge(token.toString(), 1, Integer::sum);
                token.setLength(0);
            }
        }

        if (token.length() > 0) {
            frequencies.merge(token.toString(), 1, Integer::sum);
        }

        return frequencies;
    }

    @Override
    public int calculateHighestFrequency(String text) {
        logger.info("Calculating highest frequency in text of length: {}", text != null ? text.length() : 0);
        
        Map<String, Integer> map = buildFrequencyMap(text);
        if (map.isEmpty()) {
            return 0;
        }
        
        // O(K) complexity, no temporary lists created
        int highest = Collections.max(map.values());
        logger.debug("Highest frequency found: {}", highest);
        return highest;
    }

    @Override
    public int calculateFrequencyForWord(String text, String word) {
        if (word == null || text == null) {
            logger.warn("Handling null text or word input in calculateFrequencyForWord. text={}, word={}", text, word);
            return 0;
        }
        
        logger.info("Calculating frequency for specific word: '{}'", word);
        
        // O(1) Instant lookup from Hash Map
        Map<String, Integer> map = buildFrequencyMap(text);
        int frequency = map.getOrDefault(word.toLowerCase(), 0);
        
        logger.debug("Frequency for word '{}' is {}", word.toLowerCase(), frequency);
        return frequency;
    }

    @Override
    public List<WordFrequency> calculateMostFrequentNWords(String text, int n) {
        logger.info("Calculating most frequent top {} words", n);
        
        Map<String, Integer> map = buildFrequencyMap(text);
        if (map.isEmpty() || n <= 0) {
            return Collections.emptyList();
        }

        // PriorityQueue (Min-Heap) logic for O(K * log N) top-N ranking 
        // Rules: Ascending frequency, then Descending alphabet
        PriorityQueue<Map.Entry<String, Integer>> heap = new PriorityQueue<>(
            (a, b) -> {
                if (!a.getValue().equals(b.getValue())) {
                    return a.getValue() - b.getValue(); // Ascending by frequency
                }
                // If frequencies match, descending alphabetically
                return b.getKey().compareTo(a.getKey()); 
            }
        );

        // Maintain a heap of size N
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            heap.offer(entry);
            if (heap.size() > n) {
                heap.poll(); // Kick out the weakest element (lowest freq, trailing alphabet)
            }
        }

        // Extract from heap into resulting list (must be reversed because heap head is the weakest)
        List<WordFrequency> result = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            Map.Entry<String, Integer> e = heap.poll();
            result.add(0, new WordFrequencyImpl(e.getKey(), e.getValue())); // Insert at front to reverse
        }
        
        return result;
    }
}
