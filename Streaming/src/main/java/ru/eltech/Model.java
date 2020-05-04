package ru.eltech;

import java.io.IOException;
import java.nio.file.Paths;
import org.apache.spark.ml.PipelineModel;

public class Model {
  private PipelineModel model = null;
  private Watcher watcher = null;
  private String path;

  public Model(String path) throws IOException {
    watcher = new Watcher(Paths.get(path));
    this.path = path;
  }

  public PipelineModel getModel() {
    if (model == null || watcher.check()) {
      model = PipelineModel.load(path);
    }

    return model;
  }
}
