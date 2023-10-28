package com.taim.conduire.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormData {
    private String inputText;
    private String selectedOption;

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }
}