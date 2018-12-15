package ru.eltech.dapeshkov.speed_layer;

import ru.eltech.dapeshkov.speed_layer.RSSReader.URLFilePair;

/**
 * This is a test class
 */

public class Main {
    public static void main(String[] args) {
        RSSReader rssreader = new RSSReader(
                new URLFilePair("out1.txt", "https://news.yandex.ru/politics.rss"),
                new URLFilePair("out2.txt", "https://news.yandex.ru/energy.rss"),
                new URLFilePair("out3.txt", "https://news.yandex.ru/business.rss"),
                new URLFilePair("out4.txt", "https://news.yandex.ru/index.rss"),
                new URLFilePair("out5.txt", "https://news.yandex.ru/finances.rss")
        );
        rssreader.start();
    }
}