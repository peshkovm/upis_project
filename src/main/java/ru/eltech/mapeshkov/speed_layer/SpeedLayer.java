package ru.eltech.mapeshkov.speed_layer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpeedLayer {
    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                ApiUtils.AlphaVantageParser::parseDigitalAndCryptoCurrencies
                , 0L
                , 2L
                , TimeUnit.SECONDS
        );
        scheduler.schedule(scheduler::shutdown, 8, TimeUnit.SECONDS);
        try {
            scheduler.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}