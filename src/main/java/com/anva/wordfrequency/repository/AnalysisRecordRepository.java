package com.anva.wordfrequency.repository;

import com.anva.wordfrequency.domain.AnalysisRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisRecordRepository extends MongoRepository<AnalysisRecord, String> {
}
