package ru.eltech.dapeshkov.speed_layer;

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

public class MyNode {
    public static void main(String[] args) throws IOException, InterruptedException, XMLStreamException {
        RSSReader rssreader = new RSSReader(new String[]{"http://www.kommersant.ru/rss/money.xml"
                , "https://www.kommersant.ru/RSS/section-politics.xml"}
                , new OutputStream[]{new FileOutputStream("out1.txt")
                , new FileOutputStream("out2.txt")});
        rssreader.start();
    }
}