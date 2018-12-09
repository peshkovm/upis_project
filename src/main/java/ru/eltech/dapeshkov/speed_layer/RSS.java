package ru.eltech.dapeshkov.speed_layer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

class RSS implements AutoCloseable {
    private final URL url;
    private long lastModified = -1;
    private InputStream in = null;
    private HttpURLConnection connection = null;

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