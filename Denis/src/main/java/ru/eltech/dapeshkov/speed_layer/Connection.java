package ru.eltech.dapeshkov.speed_layer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class represents the connection to Connection.
 * The {@link Connection#close() } should be called after reading all necessary information from Connection to close the connection and {@link InputStream}.
 */

class Connection implements AutoCloseable {
    private final URL url;
    private long lastModified = -1; //HTTP Last-Modified
    private InputStream in = null; //Stream for reading from Connection
    private HttpURLConnection connection = null;

    /**
     * Initializes the instance of the {@code Connection} with given URL.
     *
     * @param url URL of the Connection
     */

    Connection(final String url) {
        URL url1 = null;
        try {
            url1 = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        this.url = url1;
    }

    /**
     * Connects to the site (If-Modified-Since is used) and returns {@link InputStream  InputStream} of the given site content.
     *
     * @return the data of the site.
     */

    InputStream get() {
        in = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
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