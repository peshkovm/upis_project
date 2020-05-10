package ru.eltech.dapeshkov.classifier;

import static java.nio.file.Files.newBufferedWriter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import ru.eltech.dapeshkov.news.Connection;
import ru.eltech.dapeshkov.news.JSONProcessor;

public class Main {
  static void write(final String str, final OutputStream out) {
    try (final PrintWriter writer =
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)), true)) {
      writer.println(str);
    }
  }

  public static InputStream req(String str, String str1) throws IOException {
    URL url = new URL("http://eurekaengine.ru/ru/apiobject/lingvo/");
    URLConnection con = url.openConnection();
    HttpURLConnection http = (HttpURLConnection) con;
    http.setRequestMethod("POST"); // PUT is another valid option
    http.setDoOutput(true);
    Map<String, String> arguments = new HashMap<>();
    arguments.put("text", str);
    arguments.put("ot", str1); // This is a fake password obviously
    arguments.put("otSearchMode", "1");
    StringJoiner sj = new StringJoiner("&");
    for (Map.Entry<String, String> entry : arguments.entrySet())
      sj.add(
          URLEncoder.encode(entry.getKey(), "UTF-8")
              + "="
              + URLEncoder.encode(entry.getValue(), "UTF-8"));
    byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
    int length = out.length;
    http.setFixedLengthStreamingMode(length);
    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    http.connect();
    try (OutputStream os = http.getOutputStream()) {
      os.write(out);
    }
    InputStream inputStream = http.getInputStream();
    // http.disconnect();
    return inputStream;
  }

  public static void main(String[] args) throws IOException, InterruptedException {

    String a = "Газпром";

    JSONProcessor.News news = new JSONProcessor.News();
    Set<JSONProcessor.Item> list = new HashSet<>();
    int j = 0;
    while (j < 5000) {
      final Connection connection =
          new Connection(
              "https://www.rbc.ru/v10/search/ajax/?project=rbcnews&limit=1000"
                  + "&offset="
                  + j
                  + "&query=",
              a);
      j += 1000;
      JSONProcessor.News parse = JSONProcessor.parse(connection.get(), JSONProcessor.News.class);
      list.addAll(Arrays.stream(parse.getItems()).collect(Collectors.toList()));
      connection.close();
    }

    news.setItems(list.stream().distinct().toArray(JSONProcessor.Item[]::new));
    JSONProcessor.Train[] train = new JSONProcessor.Train[news.getItems().length];
    for (int b = 0; b < train.length; b++) {
      train[b] = new JSONProcessor.Train();
    }

    int l = 0;
    for (JSONProcessor.Item i : news.getItems()) {
      String str =
          new BufferedReader(new InputStreamReader(req(i.getTitle(), a)))
              .lines()
              .collect(Collectors.joining("\n"));
      if (str.indexOf("\"frt\": [") == -1 && !str.equals("[\n" + "\n" + "]")) {
        A parse = JSONProcessor.parse(str, A.class);
        String s;
        if (parse.tonality.frt.pos > parse.tonality.frt.neg) {
          s = "positive";
        } else if (parse.tonality.frt.neg > parse.tonality.frt.pos) {
          s = "negative";
        } else {
          s = "neutral";
        }
        train[l].setText(i.getTitle());
        train[l].setSentiment(s);
        l++;
      }
    }
    String write = JSONProcessor.write(train);
    BufferedWriter bufferedWriter =
        newBufferedWriter(Paths.get("gazprom.json"), StandardOpenOption.CREATE);
    bufferedWriter.write(write);
    bufferedWriter.close();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class A {
    Tonality tonality;

    public Tonality getTonality() {
      return tonality;
    }

    public void setTonality(Tonality tonality) {
      this.tonality = tonality;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Tonality {
      String text;
      Frt frt;

      public String getText() {
        return text;
      }

      public void setText(String text) {
        this.text = text;
      }

      public Frt getFrt() {
        return frt;
      }

      public void setFrt(Frt frt) {
        this.frt = frt;
      }

      @JsonIgnoreProperties(ignoreUnknown = true)
      static class Frt {
        public double getPos() {
          return pos;
        }

        public void setPos(double pos) {
          this.pos = pos;
        }

        public double getNeg() {
          return neg;
        }

        public void setNeg(double neg) {
          this.neg = neg;
        }

        double pos;
        double neg;
      }
    }
  }
}
