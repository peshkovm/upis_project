package ru.eltech.mapeshkov;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        final PlotHelper plotHelper = new PlotHelper("C:\\JavaLessons\\bachelor-diploma\\Maksim\\Plot\\src\\main\\resources\\plotData.txt");
        plotHelper.setMaxSeriesLength(5);

        for (; ; ) {
            TimeUnit.SECONDS.sleep(3);
            plotHelper.refresh();
            System.out.print("plot refreshed: ");
        }
    }
}