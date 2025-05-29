package com.wizlit.file.model;

import java.time.Instant;

public record LastChange<T>(Instant lastChangeTime, T data) {
}
