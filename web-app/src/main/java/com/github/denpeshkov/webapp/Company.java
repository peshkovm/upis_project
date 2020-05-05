package com.github.denpeshkov.webapp;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Company {
    GOOGLE,
    FACEBOOK,
    AMAZON;

    @JsonValue
    @Override
    public String toString() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
}
