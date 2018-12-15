package ru.eltech.dapeshkov.speed_layer;

import java.util.concurrent.*;

/**
 * This class reads RSS from given URLs and outputs the item contents in the files.
 * After invocation of method {@link #start() start()} it outputs all contents of all RSS to the files, then it will output only newly added items when RSS are updated.
 * This program requests RSS every 3 seconds.
 *
 * @author Peshkov Denis.
 */

public class RSSReader {
    private final URLFilePair[] array;
    private final ScheduledExecutorService ex = Executors.newScheduledThreadPool(4);

    /**
     * A pair of two {@link String} that is used to represent an file name and an URL name for the RSS.
     * This class is used to be a parameter for {@link RSSReader constructor} so the number of resources and output files are the same.
     */
    static public class URLFilePair {
        private final String file;
        private final String url;

        /**
         * Initializes the instance of {@code URLFilePair}.
         * @param file output file name
         * @param url URL of the RSS*/

        public URLFilePair(String file, String url) {
            this.file = file;
            this.url = url;
        }

        private String getFile() {
            return file;
        }

        private String getUrl() {
            return url;
        }
    }

    /**
     * Initialize the instance of {@code RSSReader}.
     * @param mas the array of {@link URLFilePair}
     */

    public RSSReader(URLFilePair... mas) {
        array = mas;
    }

    /**
     * This method requests all given RSS and outputs the contents to the given files.
     * Most of the time this method should be invoked only once.
     * Method works as a service running all the time with 3 second interval
     */

    public void start() {
        for (URLFilePair a : array) {
            RSS rss = new RSS(a.getUrl());
            StaxStreamProcessor processor = new StaxStreamProcessor();
            ex.scheduleAtFixedRate(() -> {
                try (rss) {
                    processor.parse(rss.get(), a.getFile());
                }
            }, 0, 3, TimeUnit.SECONDS);
        }
    }
}