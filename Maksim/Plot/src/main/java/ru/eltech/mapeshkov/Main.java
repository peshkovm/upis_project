package ru.eltech.mapeshkov;

import java.io.BufferedReader;
import java.io.FileReader;

public class Main {
  public static void main(String[] args) throws Exception {
    String inFileName = "";

    BufferedReader reader = new BufferedReader(new FileReader(inFileName));

    for (; ; ) {
      String line;
      while ((line = reader.readLine()) != null) {}
    }
  }
}
