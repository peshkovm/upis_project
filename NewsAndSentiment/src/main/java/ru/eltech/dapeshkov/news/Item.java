package ru.eltech.dapeshkov.news;

import java.io.Serializable;
import java.sql.Timestamp;

public class Item implements Serializable {
  public static final long serialVersionUID = 0L;
  private String sentiment;
  private String company;
  private double today_stock;
  private Timestamp date;

  public Item(String company_name, String sentiment, Timestamp dateTime, double stock) {
    this.sentiment = sentiment;
    this.company = company_name;
    this.today_stock = stock;
    this.date = dateTime;
  }

  @Override
  public String toString() {
    return getCompany() + "," + getSentiment() + "," + getDate() + "," + getToday_stock();
  }

  public String getSentiment() {
    return sentiment;
  }

  public void setSentiment(String sentiment) {
    this.sentiment = sentiment;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public double getToday_stock() {
    return today_stock;
  }

  public void setToday_stock(double today_stock) {
    this.today_stock = today_stock;
  }

  public Timestamp getDate() {
    return date;
  }

  public void setDate(Timestamp date) {
    this.date = date;
  }
}
