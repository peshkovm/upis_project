package ru.eltech.dapeshkov.speed_layer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class represents the connection to RSS.
 * The {@link RSS#close() } should be called after reading all necessary information from RSS to close the connection and {@link InputStream}.
 */

class RSS implements AutoCloseable {
    private final URL url;
    private long lastModified = -1; //HTTP Last-Modified
    private InputStream in = null; //Stream for reading from RSS
    private HttpURLConnection connection = null;

    /**
     * Initializes the instance of the {@code RSS} with given URL.
     * @param url URL of the RSS
     */

    RSS(String url) {
        URL url1;
        try {
            url1 = new URL(url);
        } catch (MalformedURLException e) {
            url1 = null;
            e.printStackTrace();
        }
        this.url = url1;
    }

    /**
     * Connects to the RSS (If-Modified-Since is used) and returns {@link InputStream  InputStream} of the given RSS.
     * @return the data of the RSS.
     */

    InputStream get() {
        in = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            if (lastModified != -1) {
                connection.setIfModifiedSince(lastModified);
            }
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                lastModified = connection.getLastModified();
                in = connection.getInputStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    @Override
    public void close() {
        try {
            if (in != null) {
                in.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}