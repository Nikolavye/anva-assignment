package com.anva.wordfrequency.service;

import com.anva.wordfrequency.domain.WordFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WordFrequencyAnalyzerImplTest {

    private WordFrequencyAnalyzerImpl analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new WordFrequencyAnalyzerImpl();
    }

    @Test
    void testCalculateHighestFrequency() {
        String text = "The sun shines and the sun is bright.";
        // 'the' appears 2 times, 'sun' appears 2 times, everything else 1. Highest is 2.
        int highest = analyzer.calculateHighestFrequency(text);
        
        assertThat(highest).isEqualTo(2);
    }

    @Test
    void testCalculateHighestFrequencyEmpty() {
        assertThat(analyzer.calculateHighestFrequency("")).isEqualTo(0);
        assertThat(analyzer.calculateHighestFrequency(null)).isEqualTo(0);
    }

    @Test
    void testCalculateFrequencyForWord() {
        String text = "A quick brown fox jumps over a lazy dog.";
        
        assertThat(analyzer.calculateFrequencyForWord(text, "a")).isEqualTo(2);
        assertThat(analyzer.calculateFrequencyForWord(text, "fox")).isEqualTo(1);
        assertThat(analyzer.calculateFrequencyForWord(text, "cat")).isEqualTo(0);
    }

    @Test
    void testCalculateFrequencyForWordCaseInsensitive() {
        String text = "Hello HeLLo hello HELLO";
        assertThat(analyzer.calculateFrequencyForWord(text, "hello")).isEqualTo(4);
        assertThat(analyzer.calculateFrequencyForWord(text, "HELLO")).isEqualTo(4);
    }

    @Test
    void testCalculateMostFrequentNWords() {
        String text = "The cat walks over the staircase";
        
        List<WordFrequency> result = analyzer.calculateMostFrequentNWords(text, 3);
        
        assertThat(result).hasSize(3);
        
        // 1st: "the" (2)
        assertThat(result.get(0).getWord()).isEqualTo("the");
        assertThat(result.get(0).getFrequency()).isEqualTo(2);
        
        // 2nd: "cat" (1) - alphabetically before "over", "staircase", "walks"
        assertThat(result.get(1).getWord()).isEqualTo("cat");
        
        // 3rd: "over" (1)
        assertThat(result.get(2).getWord()).isEqualTo("over");
    }

    @Test
    void testCalculateMostFrequentNWordsSortingEdgeCase() {
        String text = "z y x w v u t s r q p o n m l k j i h g f e d c b a aa bb cc";
        // All frequencies are 1. Should sort alphabetically entirely.
        
        List<WordFrequency> result = analyzer.calculateMostFrequentNWords(text, 5);
        
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getWord()).isEqualTo("a");
        assertThat(result.get(1).getWord()).isEqualTo("aa");
        assertThat(result.get(2).getWord()).isEqualTo("b");
        assertThat(result.get(3).getWord()).isEqualTo("bb");
        assertThat(result.get(4).getWord()).isEqualTo("c");
    }

    @Test
    void testPerformanceWithMassiveText() {
        StringBuilder sb = new StringBuilder();
        // Generate 1,000,000 words (10,000 unique words, each repeated 100 times)
        // Numbers are non-word characters, so we must use letters to form unique words.
        String[] dictionary = new String[10000];
        for (int i = 0; i < 10000; i++) {
            dictionary[i] = "word" + (char)('a' + (i % 26)) + (char)('a' + ((i / 26) % 26)) + (char)('a' + ((i / 676) % 26));
        }

        for (int i = 0; i < 1000000; i++) {
            sb.append(dictionary[i % 10000]).append(" ");
        }
        String massiveText = sb.toString();

        // Assert that processing 1 million words completes in under 1.5 seconds
        org.junit.jupiter.api.Assertions.assertTimeout(java.time.Duration.ofMillis(1500), () -> {
            int highest = analyzer.calculateHighestFrequency(massiveText);
            assertThat(highest).isEqualTo(100);

            // Fetch frequency for a specific dictionary word
            int freq = analyzer.calculateFrequencyForWord(massiveText, dictionary[5000]);
            assertThat(freq).isEqualTo(100);

            List<WordFrequency> top10 = analyzer.calculateMostFrequentNWords(massiveText, 10);
            assertThat(top10).hasSize(10);
            assertThat(top10.get(0).getFrequency()).isEqualTo(100);
        });
    }
}
