package ru.eltech.dapeshkov.speed_layer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a parser of RSS.
 * It outputs the contents of item to the files, also this class saves the value of most recent post in RSS and outputs only newly added posts, when RSS is updated
 */

class StaxStreamProcessor {
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();
    private ZonedDateTime lastpubdate;

    void parse(InputStream is, String file) {
        XMLStreamReader reader = null;
        ZonedDateTime time = null;
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file,true))) {
            if (is != null) {
                reader = FACTORY.createXMLStreamReader(is);
                while (reader.hasNext()) {
                    String str = getElement(reader, "item");
                    if (str != null) {
                        ZonedDateTime pubdate = getDate(str);
                        if (lastpubdate == null || lastpubdate.isBefore(pubdate)) {
                            if (time == null || pubdate.isAfter(time)) {
                                time = pubdate;
                            }
                            bufferedWriter.write(str + "\n" + "\n");
                            bufferedWriter.flush();
                        }
                    }
                }
                if (time != null) {
                    lastpubdate = time;
                }
            }
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getElement(XMLStreamReader reader, String element) throws XMLStreamException {
        StringBuilder str = new StringBuilder();
        while (reader.hasNext()) {
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT && reader.getLocalName().equals(element))
                break;
            reader.next();
        }

        while (reader.hasNext()) {
            reader.next();
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                str.append(reader.getLocalName()).append(" ").append(reader.getElementText()).append("\n");
            }
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT && reader.getLocalName().equals(element)) {
                break;
            }
            //reader.next();
        }
        return str.length() == 0 ? null : str.deleteCharAt(str.length() - 1).toString();//TODO Optional
    }

    private ZonedDateTime getDate(String str) {
        Pattern pattern = Pattern.compile("pubDate (.*)");
        Matcher matcher = pattern.matcher(str);
        matcher.find();
        return ZonedDateTime.parse(matcher.group(1), DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}