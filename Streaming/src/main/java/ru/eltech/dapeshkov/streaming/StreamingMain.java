package ru.eltech.dapeshkov.streaming;

public class StreamingMain {
  public static void main(String[] args) {
    try {
      Streaming.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
