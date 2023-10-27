package com.taim.conduire.model;

import java.util.List;

public class ApiResponse {
    private List<CodeFrequencyStat> data;

    public List<CodeFrequencyStat> getData() {
        return data;
    }

    public void setData(List<CodeFrequencyStat> data) {
        this.data = data;
    }
}
