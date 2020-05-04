package ru.eltech.mapeshkov.not_spark.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Objects;

public class StockInfo {
    @JsonProperty("01. symbol")
    private String companyName;
    @JsonProperty("02. open")
    private double open;
    @JsonProperty("03. high")
    private double high;
    @JsonProperty("04. low")
    private double low;
    @JsonProperty("05. price")
    private double price;
    @JsonProperty("06. volume")
    private int volume;
    @JsonProperty("07. latest trading day")
    private LocalDate latestTradingDay;
    @JsonProperty("08. previous close")
    private double previousClose;
    @JsonProperty("09. change")
    private double change;
    @JsonProperty("10. change percent")
    private String changePercent;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockInfo stockInfo = (StockInfo) o;
        return Double.compare(stockInfo.getOpen(), getOpen()) == 0 &&
                Double.compare(stockInfo.getHigh(), getHigh()) == 0 &&
                Double.compare(stockInfo.getLow(), getLow()) == 0 &&
                Double.compare(stockInfo.getPrice(), getPrice()) == 0 &&
                getVolume() == stockInfo.getVolume() &&
                Double.compare(stockInfo.getPreviousClose(), getPreviousClose()) == 0 &&
                Double.compare(stockInfo.getChange(), getChange()) == 0 &&
                getCompanyName().equals(stockInfo.getCompanyName()) &&
                getLatestTradingDay().equals(stockInfo.getLatestTradingDay()) &&
                getChangePercent().equals(stockInfo.getChangePercent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCompanyName(), getOpen(), getHigh(), getLow(), getPrice(), getVolume(), getLatestTradingDay(), getPreviousClose(), getChange(), getChangePercent());
    }

    @Override
    public String toString() {
        return "StockInfo{" + '\n' +
                "companyName: '" + companyName + '\n' +
                "open: " + open + '\n' +
                "high: " + high + '\n' +
                "low: " + low + '\n' +
                "price: " + price + '\n' +
                "volume: " + volume + '\n' +
                "latestTradingDay: " + latestTradingDay + '\n' +
                "previousClose: " + previousClose + '\n' +
                "change: " + change + '\n' +
                "changePercent: " + changePercent + '\n' +
                '}';
    }

    public String getCompanyName() {
        return companyName;
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

    public double getPrice() {
        return price;
    }

    public int getVolume() {
        return volume;
    }

    public LocalDate getLatestTradingDay() {
        return latestTradingDay;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public double getChange() {
        return change;
    }

    public String getChangePercent() {
        return changePercent;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public void setPrice(double price) {
        this.price = price;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    //@JsonSetter("07. latest trading day")
    public void setLatestTradingDay(String latestTradingDay) {
        this.latestTradingDay = LocalDate.parse(latestTradingDay);
    }

    public void setPreviousClose(double previousClose) {
        this.previousClose = previousClose;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public void setChangePercent(String changePercent) {
        this.changePercent = changePercent;
    }
}