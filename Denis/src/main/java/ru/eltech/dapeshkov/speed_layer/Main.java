package ru.eltech.dapeshkov.speed_layer;

import java.io.FileNotFoundException;

/** This is a test class */
public class Main {

  public static void main(final String[] args) throws FileNotFoundException {
    final NewsReader reader = new NewsReader("working_files/files/", "Google");
    reader.start();
  }
}
