package com.example.autoprivacyshield;

public class MatchRegion {
    public int start;
    public int end;
    public String matchedText;

    public MatchRegion(int start, int end, String matchedText) {
        this.start = start;
        this.end = end;
        this.matchedText = matchedText;
    }
}
