package com.example.autoprivacyshield;

import android.graphics.Rect;

public class DetectResult {
    private final String text;
    private final boolean isSensitive;
    private final String type;
    private final Rect boundingBox;

    public DetectResult(String text, boolean isSensitive, String type, Rect boundingBox) {
        this.text = text;
        this.isSensitive = isSensitive;
        this.type = type;
        this.boundingBox = boundingBox;
    }

    public String getText() {
        return text;
    }

    public boolean isSensitive() {
        return isSensitive;
    }

    public String getType() {
        return type;
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }
}