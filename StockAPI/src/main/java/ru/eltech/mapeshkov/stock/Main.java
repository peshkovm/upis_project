package ru.eltech.mapeshkov.stock;

import ru.eltech.mapeshkov.stock.beans.StockInfo;

public class Main {
  public static void main(String[] args) {
    for (int i = 0; i < 100; i++) {
      StockInfo stockInfo = ApiUtils.AlphaVantageParser.getLatestStock("apple");
      System.out.println(stockInfo);
    }
  }
}
