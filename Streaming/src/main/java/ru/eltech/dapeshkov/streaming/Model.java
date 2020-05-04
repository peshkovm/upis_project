package ru.eltech.dapeshkov.streaming;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import org.apache.spark.ml.PipelineModel;

public class Model {
  private PipelineModel model = null;
  private Watcher watcher = null;
  private String path;

  public Model(String path) throws IOException {
    watcher = new Watcher(Paths.get(path).getParent());
    this.path = path;
  }

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
