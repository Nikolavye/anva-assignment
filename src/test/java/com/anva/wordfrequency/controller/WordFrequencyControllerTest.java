package com.anva.wordfrequency.controller;

import com.anva.wordfrequency.domain.AnalysisRecord;
import com.anva.wordfrequency.domain.WordFrequency;
import com.anva.wordfrequency.domain.WordFrequencyImpl;
import com.anva.wordfrequency.service.AnalysisHistoryService;
import com.anva.wordfrequency.service.WordFrequencyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import tools.jackson.databind.ObjectMapper;

class WordFrequencyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WordFrequencyAnalyzer analyzer;

    @Mock
    private AnalysisHistoryService historyService;

    @InjectMocks
    private WordFrequencyController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testCalculateHighest() throws Exception {
        WordFrequencyController.AnalysisRequestPayload payload = new WordFrequencyController.AnalysisRequestPayload();
        payload.setText("hello hello world");

        when(analyzer.calculateHighestFrequency(eq("hello hello world"))).thenReturn(2);
        
        AnalysisRecord mockRecord = new AnalysisRecord();
        mockRecord.setSingleResult(2);
        when(historyService.store(any(AnalysisRecord.class))).thenReturn(mockRecord);

        mockMvc.perform(post("/api/v1/word-frequency/highest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.singleResult").value(2));
    }

    @Test
    void testCalculateFrequencyForWord() throws Exception {
        WordFrequencyController.AnalysisRequestPayload payload = new WordFrequencyController.AnalysisRequestPayload();
        payload.setText("hello hello world");
        payload.setWord("hello");

        when(analyzer.calculateFrequencyForWord(eq("hello hello world"), eq("hello"))).thenReturn(2);

        AnalysisRecord mockRecord = new AnalysisRecord();
        mockRecord.setSingleResult(2);
        when(historyService.store(any(AnalysisRecord.class))).thenReturn(mockRecord);

        mockMvc.perform(post("/api/v1/word-frequency/frequency-for-word")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.singleResult").value(2));
    }

    @Test
    void testCalculateMostFrequentNWords() throws Exception {
        WordFrequencyController.AnalysisRequestPayload payload = new WordFrequencyController.AnalysisRequestPayload();
        payload.setText("hello hello world");
        payload.setN(2);

        WordFrequency wf1 = new WordFrequencyImpl("hello", 2);
        WordFrequency wf2 = new WordFrequencyImpl("world", 1);

        when(analyzer.calculateMostFrequentNWords(eq("hello hello world"), eq(2)))
                .thenReturn(Arrays.asList(wf1, wf2));

        AnalysisRecord mockRecord = new AnalysisRecord();
        mockRecord.setResults(Arrays.asList((WordFrequencyImpl) wf1, (WordFrequencyImpl) wf2));
        
        when(historyService.store(any(AnalysisRecord.class))).thenReturn(mockRecord);

        mockMvc.perform(post("/api/v1/word-frequency/most-frequent-n")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].word").value("hello"))
                .andExpect(jsonPath("$.results[0].frequency").value(2))
                .andExpect(jsonPath("$.results[1].word").value("world"))
                .andExpect(jsonPath("$.results[1].frequency").value(1));
    }
}
