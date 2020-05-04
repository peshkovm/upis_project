package ru.eltech.mapeshkov.not_spark.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class StockInfoDaily {
    @JsonProperty("1. open")
    private double open;
    @JsonProperty("2. high")
    private double high;
    @JsonProperty("3. low")
    private double low;
    @JsonProperty("4. close")
    private double close;
    @JsonProperty("5. volume")
    private int volume;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockInfoDaily that = (StockInfoDaily) o;
        return Double.compare(that.getOpen(), getOpen()) == 0 &&
                Double.compare(that.getHigh(), getHigh()) == 0 &&
                Double.compare(that.getLow(), getLow()) == 0 &&
                Double.compare(that.getClose(), getClose()) == 0 &&
                getVolume() == that.getVolume();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOpen(), getHigh(), getLow(), getClose(), getVolume());
    }

    @Override
    public String toString() {
        return "StockInfoDaily{" + '\n' +
                "open: " + open + '\n' +
                "high: " + high + '\n' +
                "low: " + low + '\n' +
                "close: " + close + '\n' +
                "volume: " + volume + '\n' +
                '}';
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public int getVolume() {
        return volume;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}