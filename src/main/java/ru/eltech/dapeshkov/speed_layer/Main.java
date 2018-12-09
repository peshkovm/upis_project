package ru.eltech.dapeshkov.speed_layer;

import ru.eltech.dapeshkov.speed_layer.RSSReader.URLFilePair;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        RSSReader rssreader = new RSSReader(new URLFilePair("out1.txt", "http://www.kommersant.ru/rss/money.xml"),
                new URLFilePair("out2.txt", "https://www.kommersant.ru/RSS/section-politics.xml"));
        rssreader.start();
    }
}