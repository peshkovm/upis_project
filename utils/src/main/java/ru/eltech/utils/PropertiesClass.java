package ru.eltech.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class PropertiesClass {
  private static Properties appProperties = new Properties();
  private static final String propertiesFilePath = ".\\main\\src\\main\\resources\\app.properties";

  static {
    try {
      appProperties.setProperty(
          "liveFeedDirectory", ".\\main\\src\\main\\resources\\working_files\\live_feed\\");
      appProperties.setProperty(
          "liveFeedLogDirectory", ".\\main\\src\\main\\resources\\logs\\live_feed\\");
      appProperties.setProperty("amountOfFeedsInMinute", "15");
      appProperties.setProperty("totalAmountOfFeeds", "600");
      appProperties.setProperty("availableCompanies", "Apple, Google");
      appProperties.setProperty(
          "totalAmountOfCompanies", String.valueOf(PropertiesClass.getAvailableCompanies().length));
      appProperties.setProperty("amountOfFeedsToRestartBatchLayer", "15");
      appProperties.setProperty(
          "batchLogDirectory", ".\\main\\src\\main\\resources\\logs\\batch\\");
      appProperties.setProperty(
          "batchMlModelDirectory", ".\\main\\src\\main\\resources\\working_files\\ml_model\\");
      appProperties.setProperty("slidingWindowWidth", "3");
      appProperties.setProperty(
          "streamingLogDirectory", ".\\main\\src\\main\\resources\\logs\\streaming\\");
      appProperties.setProperty(
          "streamingViewDirectory",
          ".\\main\\src\\main\\resources\\working_files\\streaming_view\\");
      appProperties.setProperty("plotDirectory", ".\\main\\src\\main\\resources\\logs\\plots\\");
      appProperties.setProperty(
          "realStockPricesDirectory", ".\\main\\src\\main\\resources\\real_stock_prices\\");

      appProperties.store(new FileWriter(propertiesFilePath), null);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private PropertiesClass() {}

  public static String getLiveFeedDirectory() {
    return appProperties.getProperty("liveFeedDirectory");
  }

  public static String getLiveFeedLogDirectory() {
    return appProperties.getProperty("liveFeedLogDirectory");
  }

  public static int getAmountOfFeedsInMinute() {
    return Integer.parseInt(appProperties.getProperty("amountOfFeedsInMinute"));
  }

  public static int getTotalAmountOfFeeds() {
    return Integer.parseInt(appProperties.getProperty("totalAmountOfFeeds"));
  }

  public static int getTotalAmountOfCompanies() {
    return Integer.parseInt(appProperties.getProperty("totalAmountOfCompanies"));
  }

  public static int getAmountOfFeedsToRestartBatchLayer() {
    return Integer.parseInt(appProperties.getProperty("amountOfFeedsToRestartBatchLayer"));
  }

  public static int getSlidingWindowWidth() {
    return Integer.parseInt(appProperties.getProperty("slidingWindowWidth"));
  }

  public static String getBatchLogDirectory() {
    return appProperties.getProperty("batchLogDirectory");
  }

  public static String getbatchMlModelDirectory() {
    return appProperties.getProperty("batchMlModelDirectory");
  }

  public static String getStreamingLogDirectory() {
    return appProperties.getProperty("streamingLogDirectory");
  }

  public static String getStreamingViewDirectory() {
    return appProperties.getProperty("streamingViewDirectory");
  }

  public static String getPlotDirectory() {
    return appProperties.getProperty("plotDirectory");
  }

  public static String getRealStockPricesDirectory() {
    return appProperties.getProperty("realStockPricesDirectory");
  }

  public static String[] getAvailableCompanies() {
    String availableCompanies = appProperties.getProperty("availableCompanies");
    return availableCompanies.split(",");
  }
}
