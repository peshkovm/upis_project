package ru.eltech.mapeshkov.batch;

public class BatchMain {
  public static void main(String[] args) {
    try {
      Batch.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
