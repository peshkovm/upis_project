package ru.eltech.dapeshkov.classifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import ru.eltech.dapeshkov.news.JSONProcessor;

/** class for sentiment analysis */
public class BernoulliNaiveBayes<T, K> extends NaiveBayes<T, K> {

  public BernoulliNaiveBayes() {}

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
        .distinct()
        .forEach(
            i -> {
              likelihood.compute(new Pair(i, category), (k, v) -> (v == null) ? 1 : v + 1);
              vocabulary.add(i);
            });
    prior_probability.compute(category, (k, v) -> (v == null) ? 1 : v + 1);
  }

  // method to colculate the likelihood of the text to givven sentiment
  @Override
  Double classify_cat(final T category, final Collection<K> vector) {
    // log is used to not multiply small close to 0 numbers, instead sum is used
    // laplacian smooth is used
    // multinomial
    /*Double s = Math.log(prior_probability.get(category) / (double) countOfDocuments) +
    vocabulary.stream().mapToDouble(a -> {
        if (vector.contains(a))
            return Math.log((double) (likelihood.getOrDefault(new Pair(a, category), 0) + 1) / (prior_probability.get(category) + 2));
        return Math.log(1 - ((double) (likelihood.getOrDefault(new Pair(a, category), 0) + 1) / (prior_probability.get(category) + 2)));
    }).sum();*/
    Double s =
        prior_probability.get(category)
            / (double) countOfDocuments
            * vocabulary.stream()
                .mapToDouble(
                    a -> {
                      if (vector.contains(a))
                        return (double) (likelihood.getOrDefault(new Pair(a, category), 0) + 1)
                            / (prior_probability.get(category) + 2);
                      return 1
                          - ((double) (likelihood.getOrDefault(new Pair(a, category), 0) + 1)
                              / (prior_probability.get(category) + 2));
                    })
                .reduce((a, b) -> a * b)
                .getAsDouble();
    return s;
  }

  public static void main(final String[] args) throws IOException {
    JSONProcessor.Train[] arr = null;
    BernoulliNaiveBayes<String, String> bernoulliNaiveBayes = new BernoulliNaiveBayes<>();

    try (InputStream in =
        BernoulliNaiveBayes.class.getResourceAsStream("/sberbank_lemtrain.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (JSONProcessor.Train a : arr) {
      String[] str = BernoulliNaiveBayes.parse(a.getText(), 2);
      if (str != null) {
        bernoulliNaiveBayes.train(a.getSentiment(), Arrays.asList(str));
      }
    }

    try (InputStream in = BernoulliNaiveBayes.class.getResourceAsStream("/sberbank_lemtest.json")) {
      arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    int i = 0;

    for (JSONProcessor.Train a : arr) {
      if (a.getText() == null) continue;
      String[] str = BernoulliNaiveBayes.parse(a.getText(), 2);
      String sentiment = null;
      if (str != null) {
        sentiment = bernoulliNaiveBayes.sentiment(Arrays.asList(str));
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
