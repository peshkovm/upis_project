package ru.eltech.mapeshkov.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map;
import ru.eltech.mapeshkov.stock.beans.CompanyInfo;
import ru.eltech.mapeshkov.stock.beans.StockInfo;
import ru.eltech.mapeshkov.stock.beans.StockInfoDaily;

/** This util class contains various methods for parsing api */
public class ApiUtils {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static StockInfo latestStockInfo;

  // Suppresses default constructor, ensuring non-instantiability.
  private ApiUtils() {}

  /**
   * This util inner class contains various methods for parsing api from <a
   * href="https://www.alphavantage.co/documentation/"><i>Alpha Vantage</i></a> site
   */
  public static class AlphaVantageParser {
    // Suppresses default constructor, ensuring non-instantiability.
    private AlphaVantageParser() {}

    /**
     * Get stock price at specified date of specified company
     *
     * @param date the specified date
     * @param companyName the name of specified company
     * @return
     */
    public static StockInfoDaily getStockAtSpecifiedDay(
        final LocalDate date, final String companyName) {
      final String function = "TIME_SERIES_DAILY";
      final String dataType = "json";
      StockInfoDaily stockInfo;

      CompanyInfo companyInfo = getSymbolFromCompanyName(companyName);
      String symbol = companyInfo.getSymbol();

      try {
        final URL url =
            new URL(
                "https://www.alphavantage.co/query"
                    + "?function="
                    + function
                    + "&symbol="
                    + symbol
                    + "&outputsize="
                    + "full"
                    + "&datatype="
                    + dataType
                    + "&apikey=TF0UUHCZB8SBMXDP");
        JsonNode node = getNodeFromUrl(url);
        JsonNode foundNode = null;

        for (LocalDate localDate = date; foundNode == null; localDate = localDate.minusDays(1)) {
          foundNode = node.findValue(localDate.toString());
        }

        stockInfo = getPojoData(foundNode, StockInfoDaily.class);
      } catch (IOException e) {
        stockInfo = null;
        e.printStackTrace();
      }

      return stockInfo;
    }

    /**
     * Get latest stock price of specified company
     *
     * @param companyName the name of specified company
     * @return
     */
    public static StockInfo getLatestStock(final String companyName) {
      final String function = "GLOBAL_QUOTE";
      final String datatype = "json";
      StockInfo stockInfo;

      CompanyInfo companyInfo = getSymbolFromCompanyName(companyName);
      if (companyInfo == null) {
        System.err.println("frequency excess occurred");
        return latestStockInfo;
      }

      String symbol = companyInfo.getSymbol();

      try {
        final URL url =
            new URL(
                "https://www.alphavantage.co/query"
                    + "?function="
                    + function
                    + "&symbol="
                    + symbol
                    + "&datatype="
                    + datatype
                    + "&apikey=TF0UUHCZB8SBMXDP");

        JsonNode node = getNodeFromUrl(url);

        if ((node = excessHandler(node, "Global Quote")) == null) {
          System.err.println("frequency excess occurred");
          return latestStockInfo;
        }

        stockInfo = getPojoData(node, StockInfo.class);
        latestStockInfo = stockInfo;

      } catch (IOException e) {
        stockInfo = null;
        e.printStackTrace();
      }

      return stockInfo;
    }

    /**
     * Get company symbol from company name
     *
     * @param companyName the name of company
     * @return
     */
    public static CompanyInfo getSymbolFromCompanyName(final String companyName) {
      final String function = "SYMBOL_SEARCH";
      final String datatype = "json";
      CompanyInfo companyInfo;

      try {
        final URL url =
            new URL(
                "https://www.alphavantage.co/query"
                    + "?function="
                    + function
                    + "&keywords="
                    + companyName
                    + "&datatype="
                    + datatype
                    + "&apikey=TF0UUHCZB8SBMXDP");

        JsonNode node = getNodeFromUrl(url);

        if ((node = excessHandler(node, "bestMatches")) == null) return null;

        node = node.path(0);

        companyInfo = mapper.treeToValue(node, CompanyInfo.class);

      } catch (IOException e) {
        companyInfo = null;
        e.printStackTrace();
      }

      return companyInfo;
    }

    /**
     * Method that handles frequency excess of API calls. Return JsonNode or null if frequency
     * excess happened.
     *
     * @param node node to test for frequency excess
     * @param path path to find in node
     * @return
     * @throws IOException
     */
    private static JsonNode excessHandler(final JsonNode node, final String path)
        throws IOException {
      JsonNode pathedNode = node.path(path);

      if (pathedNode.isMissingNode()) {
        pathedNode = node.path("Note");
        if (!pathedNode.isMissingNode()) {
          pathedNode = null;
        } else throw new IOException("Node doesn't contain StockInfo or Note:frequency excess");
      }
      return pathedNode;
    }
  }

  /**
   * Get JsonNode from URL
   *
   * @param url
   * @return JsonNode
   * @throws IOException
   */
  private static JsonNode getNodeFromUrl(URL url) throws IOException {
    URLConnection connection = url.openConnection();
    String redirect = connection.getHeaderField("Location");

    if (redirect != null) {
      connection = new URL(redirect).openConnection();
    }

    JsonNode node = mapper.readTree(connection.getInputStream());

    return node;
  }

  /**
   * Get POJO of class T from JsonNode
   *
   * @param node
   * @param clazz
   * @param <T>
   * @return
   * @throws IOException
   */
  private static <T> T getPojoData(JsonNode node, Class<T> clazz) throws IOException {
    return mapper.treeToValue(node, clazz);
  }

  /**
   * Prints all data from resource pointed by the specified url to the screen.
   *
   * @param url the url to print to the screen
   */
  public static void printApiData(final URL url) {
    try {
      URLConnection connection = url.openConnection();
      String redirect = connection.getHeaderField("Location");
      if (redirect != null) {
        connection = new URL(redirect).openConnection();
      }

      try (BufferedReader in =
          new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          System.out.println(inputLine);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void printField(final Iterator<Map.Entry<String, JsonNode>> fields) {
    Map.Entry<String, JsonNode> entry = fields.next();
    String name = entry.getKey();
    JsonNode value = entry.getValue();
    System.out.println(name + ":" + value);
  }

  private static Iterator<Map.Entry<String, JsonNode>> getFields(
      final JsonNode node, final String path) {
    return node.path(path).fields();
  }
}
