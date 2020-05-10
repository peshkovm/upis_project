package ru.eltech.dapeshkov.classifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import ru.eltech.dapeshkov.news.JSONProcessor;

/** class for sentiment analysis */
public class MultinomialNaiveBayes<T, K> extends NaiveBayes<T, K> {

  private final Map<T, Integer> counts = new HashMap<>();

  public MultinomialNaiveBayes() {}

  /**
   * trains the model
   *
   * @param n number of words in feature vector element
   */
  @Override
  public void train(T category, Collection<K> vector) {
    this.category.add(category);
    // number of documents
    countOfDocuments++;

    // trains the model
    vector.stream()
        .unordered()
        .forEach(
            i -> {
              likelihood.compute(new Pair(i, category), (k, v) -> (v == null) ? 1 : v + 1);
              vocabulary.add(i);
              counts.compute(category, (k, v) -> (v == null) ? 1 : v + 1);
            });
    prior_probability.compute(category, (k, v) -> (v == null) ? 1 : v + 1);
  }

  // method to colculate the likelihood of the text to givven sentiment
  @Override
  Double classify_cat(final T category, final Collection<K> vector) {
    // log is used to not multiply small close to 0 numbers, instead sum is used
    // laplacian smooth is used
    // multinomial
    Double s =
        Math.log(prior_probability.get(category) / (double) countOfDocuments)
            + vector.stream()
                .unordered()
                .mapToDouble(
                    value ->
                        Math.log(
                            (likelihood.getOrDefault(new Pair(value, category), 0) + 1)
                                / (double) (counts.get(category) + vocabulary.size())))
                .sum();
    return s;
  }

  public static void main(final String[] args) throws IOException {
    JSONProcessor.Train[] arr = null;
    MultinomialNaiveBayes<String, String> processing = new MultinomialNaiveBayes<>();

    try (InputStream in =
        MultinomialNaiveBayes.class.getResourceAsStream("/sberbank_lemtrain.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (JSONProcessor.Train a : arr) {
      String[] str = MultinomialNaiveBayes.parse(a.getText(), 2);
      if (str != null) {
        processing.train(a.getSentiment(), Arrays.asList(str));
      }
    }

    try (InputStream in =
        MultinomialNaiveBayes.class.getResourceAsStream("/sberbank_lemtest.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    int i = 0;

    for (JSONProcessor.Train a : arr) {
      String[] str = MultinomialNaiveBayes.parse(a.getText(), 2);
      String sentiment = null;
      if (str != null) {
        sentiment = processing.sentiment(Arrays.asList(str));
      }
      if (sentiment == null) {
        continue;
      }
      String sentiment1 = a.getSentiment();
      if (sentiment.equals(sentiment1)) {
        i++;
      }
    }
    System.out.println((i / (double) arr.length) * 100);
  }
}
