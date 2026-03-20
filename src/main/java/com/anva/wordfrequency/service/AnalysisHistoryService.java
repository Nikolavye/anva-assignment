package com.anva.wordfrequency.service;

import com.anva.wordfrequency.domain.AnalysisRecord;
import com.anva.wordfrequency.repository.AnalysisRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AnalysisHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisHistoryService.class);

    private final AnalysisRecordRepository repository;
    private final boolean historyEnabled;

    public AnalysisHistoryService(
            AnalysisRecordRepository repository,
            @Value("${app.history.enabled:false}") boolean historyEnabled) {
        this.repository = repository;
        this.historyEnabled = historyEnabled;
    }

    public AnalysisRecord store(AnalysisRecord record) {
        if (!historyEnabled) {
            return record;
        }

        try {
            return repository.save(record);
        } catch (RuntimeException exception) {
            logger.warn("History persistence failed; returning analysis result without saved history.", exception);
            return record;
        }
    }

    public List<AnalysisRecord> findAll() {
        if (!historyEnabled) {
            return Collections.emptyList();
        }

        try {
            return repository.findAll();
        } catch (RuntimeException exception) {
            logger.warn("History lookup failed; returning an empty history list.", exception);
            return Collections.emptyList();
        }
    }

    public void deleteById(String id) {
        if (!historyEnabled) {
            return;
        }

        try {
            repository.deleteById(id);
        } catch (RuntimeException exception) {
            logger.warn("History deletion failed for id '{}'.", id, exception);
        }
    }

    public void deleteAll() {
        if (!historyEnabled) {
            return;
        }

        try {
            repository.deleteAll();
        } catch (RuntimeException exception) {
            logger.warn("History deletion failed for all records.", exception);
        }
    }
}
