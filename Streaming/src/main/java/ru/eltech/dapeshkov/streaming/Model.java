package ru.eltech.dapeshkov.streaming;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import org.apache.spark.ml.PipelineModel;

/** class to get mlib model from file if model changes gets new model */
public class Model {
  // mlib model
  private PipelineModel model = null;
  // wathceds the directory for model changes
  private Watcher watcher = null;
  private String path;

  /**
   * creates new {@link Model} instance
   *
   * @param path path to mlib model file
   * @throws IOException
   */
  public Model(String path) throws IOException {
    watcher = new Watcher(Paths.get(path).getParent());
    this.path = path;
  }

  /**
   * gets mlib model
   *
   * @return mlib model
   */
  public PipelineModel getModel() {
    if (model == null
        || watcher.check(
            StandardWatchEventKinds.ENTRY_CREATE, Paths.get(path).getFileName().toString())) {
      model = PipelineModel.load(path);
      System.out.println("loaded model");
    }

    return model;
  }
}
