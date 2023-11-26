package com.taim.conduire.model;

import java.util.List;

/* =========Design Smell=========
* We know that this is a unutilized abstraction, and we're keeping this  just for visibility to the marker.
* */
public class ApiResponse {
    private List<CodeFrequencyStat> data;

    public List<CodeFrequencyStat> getData() {
        return data;
    }

    public void setData(List<CodeFrequencyStat> data) {
        this.data = data;
    }
}
