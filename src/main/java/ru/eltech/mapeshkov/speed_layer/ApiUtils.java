package ru.eltech.mapeshkov.speed_layer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * This class contains various methods for parsing api from
 * <a href="https://www.alphavantage.co/documentation/">
 * <i>Alpha Vantage</i></a> site
 */
public class ApiUtils {
    // Suppresses default constructor, ensuring non-instantiability.
    private ApiUtils() {
    }

    public static class AlphaVantageParser {
        private static String lastTimeRefreshedCrypto;
        private static String lastTimeRefreshedStock;

        // Suppresses default constructor, ensuring non-instantiability.
        private AlphaVantageParser() {
        }

        /**
         * Parse Stock Time Series with intraday temporal resolution.
         */
        public static void parseStockTimeSeries() {
            final String function = "TIME_SERIES_INTRADAY";
            final String symbol = "MSFT";
            final String interval = "1min";
            JsonNode node;

            try {
                URL url = new URL("https://www.alphavantage.co/query" +
                        "?function=" + function +
                        "&symbol=" + symbol +
                        "&interval=" + interval +
                        "&datatype=json" +
                        "&apikey=TF0UUHCZB8SBMXDP");

                do {
                    node = getNodeFromUrl(url);
                    if (frequencyExcessHandler(node))
                        return;

                    final String refreshed = node.path("Meta Data")
                            .get("3. Last Refreshed").asText();

                    System.out.println("refreshed = " + refreshed);

                    if (!refreshed.equals(lastTimeRefreshedStock)) {
                        lastTimeRefreshedStock = refreshed;
                        break;
                    }

                    System.err.println("Sleep occurred");
                    TimeUnit.SECONDS.sleep(5); //TODO Разобраться с задержкой метода
                } while (true);

                Iterator<Map.Entry<String, JsonNode>> fields = getFields(node, "Time Series (" + interval + ")");

                printFields(fields);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Parse Digital & Crypto Currencies with
         * CURRENCY_EXCHANGE_RATE temporal resolution.
         */
        public static void parseDigitalAndCryptoCurrencies() {
            final String function = "CURRENCY_EXCHANGE_RATE";
            final String fromCurrency = "USD";
            final String toCurrency = "RUB";
            JsonNode node;

            try {
                URL url = new URL("https://www.alphavantage.co/query?" +
                        "function=" + function +
                        "&from_currency=" + fromCurrency +
                        "&to_currency=" + toCurrency +
                        "&apikey=TF0UUHCZB8SBMXDP");

                do {
                    node = getNodeFromUrl(url);
                    if (frequencyExcessHandler(node))
                        return;

                    final String refreshed = node.path("Realtime Currency Exchange Rate")
                            .get("6. Last Refreshed").asText();

                    //System.out.println("lastTimeRefreshedCrypto = " + lastTimeRefreshedCrypto);

                    if (!refreshed.equals(lastTimeRefreshedCrypto)) {
                        lastTimeRefreshedCrypto = refreshed;
                        break;
                    }

                    System.err.println("Sleep occurred");
                    TimeUnit.MILLISECONDS.sleep(250); //TODO Разобраться с задержкой метода
                } while (true);

                Iterator<Map.Entry<String, JsonNode>> fields = getFields(node, "Realtime Currency Exchange Rate");

                while (fields.hasNext()) {
                    printFields(fields);
                }
                System.out.print(System.lineSeparator()); //blank line to separate data
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private static boolean frequencyExcessHandler(JsonNode node) {
            boolean isFrequencyExcessOccurred = !Objects.isNull(node.get("Note"));

            if (isFrequencyExcessOccurred)
                System.out.println("Frequency excess. " +
                        "Alpha Vantage standard API call frequency is " +
                        "5 calls per minute and 500 calls per day.");

            return isFrequencyExcessOccurred;
        }
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

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonNode getNodeFromUrl(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        String redirect = connection.getHeaderField("Location");

        if (redirect != null) {
            connection = new URL(redirect).openConnection();
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(connection.getInputStream());

        return node;
    }

    private static void printFields(final Iterator<Map.Entry<String, JsonNode>> fields) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String name = entry.getKey();
        JsonNode value = entry.getValue();
        System.out.println(name + ":" + value);
    }

    private static Iterator<Map.Entry<String, JsonNode>> getFields(final JsonNode node, final String path) {
        return node.path(path).fields();
    }
}