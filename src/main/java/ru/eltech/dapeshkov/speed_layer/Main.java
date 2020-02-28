package ru.eltech.dapeshkov.speed_layer;

import ru.eltech.spark.Batch;
import ru.eltech.spark.Streaming;

/**
 * This is a test class
 */

public class Main {
    public static void main(final String[] args) {
        Batch.start();
        Streaming.start();
        final NewsReader reader = new NewsReader("files/", "Google", "Google");
        reader.start();
    }
}