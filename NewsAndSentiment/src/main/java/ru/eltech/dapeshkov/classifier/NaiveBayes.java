package ru.eltech.dapeshkov.classifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class NaiveBayes<T, K> {
  // mapping (word,sentiment) to count, each word (or several words) gets mapped for later use in
  // sentiment method
  final HashMap<NaiveBayes.Pair, Integer> likelihood =
      new HashMap<>(); // concurency not needed final field safe published
  // mapping sentiment to count with given sentiment
  final HashMap<T, Integer> prior_probability =
      new HashMap<>(); // concurency not needed final field safe published
  // stopwords
  static final Set<String> hash = new HashSet<>();
  // sentiment
  final Set<T> category = new HashSet<>();
  int countOfDocuments = 0;
  final Set<K> vocabulary = new HashSet<>();

  class Pair {
    final K word;
    final T category;

    public Pair(K word, T category) {
      this.word = word;
      this.category = category;
    }

    public K getWord() {
      return word;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      NaiveBayes.Pair pair = (NaiveBayes.Pair) o;
      return word.equals(pair.word) && category.equals(pair.category);
    }

    @Override
    public int hashCode() {
      return Objects.hash(word, category);
    }

    public T getCategory() {
      return category;
    }

    @Override
    public String toString() {
      return word + " " + category;
    }
  }

  public static String[] parse(final String str, final int n) {
    String[] res = str.toLowerCase().split("[^\\p{L}]+");
    if (res.length < n) return null;
    res = Arrays.stream(res).filter(t -> !hash.contains(t)).toArray(String[]::new);
    res = ngram(res, n);

    return res;
  }

  private static String[] ngram(final String[] arr, final int n) {
    String[] res = new String[arr.length - n + 1];
    for (int i = 0; i < arr.length - n + 1; i++) {
      final StringBuilder str = new StringBuilder();
      for (int j = 0; j < n; j++) {
        str.append(arr[i + j]).append(" ");
      }
      res[i] = str.toString();
    }
    return res;
  }

  // stopwords into hash
  static {
    try (Stream<String> lines =
        new BufferedReader(
                new InputStreamReader(
                    BernoulliNaiveBayes.class.getResourceAsStream("/stopwatch.txt")))
            .lines()) {
      lines.forEach(hash::add);
    }
  }

  /**
   * computes teh sentiment for text
   *
   * @param str text
   * @return sentiment
   */
  public T sentiment(Collection<K> vector) {
    Map<T, Double> collect =
        this.category.stream()
            .unordered()
            .collect(Collectors.toMap((T s) -> s, (T o) -> classify_cat(o, vector)));
    System.out.println(collect);
    return collect.entrySet().stream()
        .max(Comparator.comparing(Map.Entry::getValue))
        .get()
        .getKey();
  }

  // method to colculate the likelihood of the text to givven sentiment
  abstract Double classify_cat(final T category, final Collection<K> vector);

  public abstract void train(T category, Collection<K> vector);
}
