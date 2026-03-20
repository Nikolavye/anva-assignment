package com.anva.wordfrequency.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordFrequencyImpl implements WordFrequency {
    private String word;
    private int frequency;


}
