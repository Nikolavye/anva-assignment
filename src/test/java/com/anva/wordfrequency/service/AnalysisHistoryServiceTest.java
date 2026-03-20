package com.anva.wordfrequency.service;

import com.anva.wordfrequency.domain.AnalysisRecord;
import com.anva.wordfrequency.repository.AnalysisRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisHistoryServiceTest {

    @Test
    void storeReturnsOriginalRecordWhenHistoryIsDisabled() {
        AnalysisRecordRepository repository = mock(AnalysisRecordRepository.class);
        AnalysisHistoryService service = new AnalysisHistoryService(repository, false);
        AnalysisRecord record = new AnalysisRecord();
        record.setOperation("calculateHighestFrequency");

        AnalysisRecord result = service.store(record);

        assertThat(result).isSameAs(record);
        verify(repository, never()).save(record);
    }

    @Test
    void findAllReturnsEmptyListWhenHistoryLookupFails() {
        AnalysisRecordRepository repository = mock(AnalysisRecordRepository.class);
        AnalysisHistoryService service = new AnalysisHistoryService(repository, true);
        when(repository.findAll()).thenThrow(new RuntimeException("mongo unavailable"));

        List<AnalysisRecord> result = service.findAll();

        assertThat(result).isEmpty();
    }
}
