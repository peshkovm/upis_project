package ru.eltech.dapeshkov.speed_layer;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class RSSReader {
    private final RSS[] rss;
    private final URLFilePair[] array;
    private final ScheduledExecutorService ex = Executors.newScheduledThreadPool(4);

    static public class URLFilePair {
        private final String file;
        private final String url;

        public URLFilePair(String file, String url) {
            this.file = file;
            this.url = url;
        }

        public String getFile() {
            return file;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            URLFilePair that = (URLFilePair) o;
            return Objects.equals(getFile(), that.getFile()) &&
                    Objects.equals(getUrl(), that.getUrl());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFile(), getUrl());
        }

        @Override
        public String toString() {
            return "URLFilePair{" +
                    "file='" + file + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    public RSSReader(URLFilePair... mas) {
        array = mas;
        rss = new RSS[mas.length];
        for (int i = 0; i < mas.length; i++) {
            rss[i] = new RSS(mas[i].getUrl());
        }
    }

    public void start() {
        for (int i = 0; i < rss.length; i++) {
            int finalI = i;
            ex.scheduleAtFixedRate(() -> {
                try {
                    StaxStreamProcessor.parse(rss[finalI].get(), array[finalI].getFile());
                } finally {
                    rss[finalI].close();
                }
            }, 0, 3, TimeUnit.SECONDS);
        }
    }
}