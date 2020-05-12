package ru.eltech.mapeshkov.plot;

public class PlotMain {
  public static void main(String[] args) {
    try {
      PlotHelper.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
