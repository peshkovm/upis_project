package ru.eltech.dapeshkov.speed_layer;

import ru.eltech.spark.Batch;
import ru.eltech.spark.Streaming;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is a test class
 */

public class Main {
    public static void main(final String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(Batch::start);
        service.submit(Streaming::start);
        final NewsReader reader = new NewsReader("files/", "Google", "Google");
        reader.start();
    }
}