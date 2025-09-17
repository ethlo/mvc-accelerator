package com.ethlo.mvc;

public class BaseMode {
    private Mode mode = Mode.ANNOTATED;

    public Mode getMode() {
        return mode;
    }

    public BaseMode setMode(Mode mode) {
        this.mode = mode;
        return this;
    }
}
