package ru.eltech.utils;

import java.util.ArrayList;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

/** Enum that contains schemes used for prediction */
public enum Schemes {
  SCHEMA_NOT_LABELED(
      new StructField[] {
        new StructField("company", DataTypes.StringType, false, Metadata.empty()),
        new StructField("sentiment", DataTypes.StringType, false, Metadata.empty()),
        new StructField("date", DataTypes.TimestampType, false, Metadata.empty()),
        new StructField("today_stock", DataTypes.DoubleType, false, Metadata.empty()),
        // new StructField("tomorrow_stock", DataTypes.DoubleType, false, Metadata.empty()),
      }),
  SCHEMA_LABELED(
      new StructField[] {
        new StructField("company", DataTypes.StringType, false, Metadata.empty()),
        new StructField("sentiment", DataTypes.StringType, false, Metadata.empty()),
        new StructField("date", DataTypes.StringType, false, Metadata.empty()),
        new StructField("today_stock", DataTypes.DoubleType, false, Metadata.empty()),
        new StructField("label", DataTypes.DoubleType, true, Metadata.empty()),
      }),
  SCHEMA_WINDOWED() {
    {
      //////////// fill schema///////////////////
      int slidingWindowWidth = PropertiesClass.getSlidingWindowWidth();
      ArrayList<StructField> structFieldList = new ArrayList<>();
      StructField[] structFields = new StructField[2 * slidingWindowWidth + 1];

      for (int i = slidingWindowWidth - 1; i >= 0; i--) {
        if (i != 0) {
          structFieldList.add(
              new StructField("sentiment_" + i, DataTypes.StringType, false, Metadata.empty()));
          structFieldList.add(
              new StructField("stock_" + i, DataTypes.DoubleType, false, Metadata.empty()));
        } else {
          structFieldList.add(
              new StructField("sentiment_today", DataTypes.StringType, false, Metadata.empty()));
          structFieldList.add(
              new StructField("stock_today", DataTypes.DoubleType, false, Metadata.empty()));
          structFieldList.add(
              new StructField("label", DataTypes.DoubleType, true, Metadata.empty()));
        }
      }

      structFields = structFieldList.toArray(structFields);
      //////////////////////////////////////////

      structType = new StructType(structFields);
    }
  };

  protected StructType structType;

  Schemes() {
    this(null);
  }

  Schemes(StructField[] structFields) {
    this.structType = new StructType(structFields);
  }

  /**
   * Returns scheme's type
   *
   * @return
   */
  public StructType getScheme() {
    return structType;
  }
}
