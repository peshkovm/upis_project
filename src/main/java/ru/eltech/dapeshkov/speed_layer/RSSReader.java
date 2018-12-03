package ru.eltech.dapeshkov.speed_layer;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RSSReader {
    private final RSS[] rss;
    private final OutputStream[] files;
    private final ScheduledExecutorService ex = Executors.newScheduledThreadPool(4);

    public RSSReader(String[] mas, OutputStream[] files) throws IOException, XMLStreamException {
        rss = new RSS[mas.length];
        this.files = files;
        for (int i = 0; i < mas.length; i++) {
            rss[i] = new RSS(mas[i]);
        }
    }

    public void start() {
        int bound = rss.length;
        for (int i = 0; i < bound; i++) {
            int finalI = i;
            ex.scheduleAtFixedRate(() -> {
                try {
                    StaxStreamProcessor.parse(rss[finalI].get(), files[finalI]);
                } finally {
                    rss[finalI].close();
                }
            }, 0, 3, TimeUnit.SECONDS);
        }
    }
}