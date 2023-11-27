package com.taim.conduire.model;

/* =========Design Smell=========
 * We know that this is a unutilized abstraction, and we're keeping this  just for visibility to the marker.
 * */

public class CodeFrequencyStat {
    private int[] codeFrequency;

    public int[] getCodeFrequency() {
        return codeFrequency;
    }

    public void setCodeFrequency(int[] codeFrequency) {
        this.codeFrequency = codeFrequency;
    }
}
