package ru.eltech.dapeshkov.streaming;

import org.apache.spark.ml.PipelineModel;
import ru.eltech.utils.PathEventsListener;

/** class to get mlib model from FEEDS if model changes gets new model */
public class Model {
  // mlib model
  private PipelineModel model = null;
  // wathceds the directory for model changes
  private PathEventsListener pathEventsListener = null;
  private String path;

  /**
   * creates new {@link Model} instance
   *
   * @param path path to mlib model FEEDS
   */
  public Model(String path) {
    this.path = path;
  }

  /**
   * gets mlib model
   *
   * @return mlib model
   */
  public PipelineModel getModel() {
    model = PipelineModel.load(path);

    return model;
  }
}
