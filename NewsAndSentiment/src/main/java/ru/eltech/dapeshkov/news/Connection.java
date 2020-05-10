package ru.eltech.dapeshkov.news;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * This class represents the connection to URL. The {@link Connection#close() } should be called
 * after reading all necessary information from Connection to close the connection and {@link
 * InputStream}.
 */
public class Connection implements AutoCloseable {
  private final URL url;
  private long lastModified = -1; // HTTP Last-Modified
  private InputStream in = null; // Stream for reading from Connection
  private HttpURLConnection connection = null;

  /**
   * Initializes the instance of the {@code Connection} with given URL.
   *
   * @param url URL of the Connection
   */
  public Connection(final String url, final String company) {
    URL url1 = null;
    try {
      url1 = new URL(url + URLEncoder.encode(company, "UTF-8"));
    } catch (MalformedURLException | UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    this.url = url1;
  }

  /**
   * Connects to the site (If-Modified-Since is used) and returns {@link InputStream InputStream} of
   * the given site content.
   *
   * @return the data of the site.
   */
  public InputStream get() {
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
        connection.getInputStream();
      }
    } catch (IOException e) {
    }
    return in;
  }

  /** closes connection */
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
