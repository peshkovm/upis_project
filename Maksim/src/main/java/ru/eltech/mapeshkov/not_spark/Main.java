package ru.eltech.mapeshkov.not_spark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.eltech.mapeshkov.not_spark.beans.StockInfo;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            StockInfo stockInfo = ApiUtils.AlphaVantageParser.getLatestStock("apple");
            System.out.println(stockInfo);
        }
    }

    static void testApiUtilsOld() {
        /*        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = sdf.parse("2002-05-02");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                () -> ApiUtilsOld.AlphaVantageParser.getStockAtSpecifiedDay(date, "aapl")
                , 0L
                , 1L
                , TimeUnit.MINUTES
        );
        scheduler.schedule(scheduler::shutdown, 0, TimeUnit.MINUTES);
        try {
            scheduler.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

/*        try {
            foo();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
    }

    static void foo() throws FileNotFoundException {
        final String function = "TIME_SERIES_DAILY";
        final String symbol = "aapl";
        JsonNode node;
        PrintWriter writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream("E:/outFile.txt")));

        try {
            final URL url = new URL("https://www.alphavantage.co/query" +
                    "?function=" + function +
                    "&symbol=" + symbol +
                    "&outputsize=" + "full" +
                    "&datatype=json" +
                    "&apikey=TF0UUHCZB8SBMXDP");

            //getAllStockData(url);

            try {
                node = getNodeFromUrl(url);

                Iterator<Map.Entry<String, JsonNode>> fields = getFields(node, "Time Series (Daily)");

                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String name = entry.getKey();
                    JsonNode value = entry.getValue();
                    StringBuilder stringBuilder = new StringBuilder();


                    Iterator<Map.Entry<String, JsonNode>> fieldsValue = value.fields();
                    if (fieldsValue.hasNext()) {
                        Map.Entry<String, JsonNode> entry1 = fieldsValue.next();
                        JsonNode value1 = entry1.getValue();
                        StringBuilder val1 = new StringBuilder(value1.toString());
                        val1.deleteCharAt(0);
                        val1.deleteCharAt(val1.length() - 1);

                        double val = Double.parseDouble(val1.toString());
                        stringBuilder.append(val);
                    }

                    //stringBuilder.deleteCharAt(stringBuilder.length());

                    String sentiment = randomSentiment();
                    writer.println("goggle" + ";" + sentiment + ";" + name + ";" + stringBuilder);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (MalformedURLException e) {
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

    private static Iterator<Map.Entry<String, JsonNode>> getFields(final JsonNode node, final String path) {
        return node.path(path).fields();
    }

    private static String randomSentiment() {
        String[] mas = {"negative", "neutral", "positive"};

        Random random = new Random();
        int i = random.nextInt(mas.length);
        int randNum = 1 + i;
        return mas[randNum - 1];
    }
}