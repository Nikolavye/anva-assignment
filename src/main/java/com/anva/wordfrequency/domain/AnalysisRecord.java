package com.anva.wordfrequency.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
@Document(collection = "analysis_records")
public class AnalysisRecord {

    @Id
    private String id;
    private String text;
    private String operation;
    private List<WordFrequencyImpl> results;
    private int singleResult;
    private Date timestamp = new Date();
}
