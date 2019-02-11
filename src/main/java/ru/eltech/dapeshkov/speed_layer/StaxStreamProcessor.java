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
 * It outputs the contents of item to the files, also this class saves the value of most recent post in RSS and outputs only newly added posts, when RSS is updated.
 */

class StaxStreamProcessor {
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance(); //StAX
    private ZonedDateTime lastpubdate; // date of the last publication in given RSS

    /**
     * parses RSS contents and outputs results to the file.
     * @param is contents of the RSS.
     * @param file name of the output file.
     */

    void parse(InputStream is, String file) {
        XMLStreamReader reader = null; //StAX
        ZonedDateTime time = null; //date of the item
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file,true))) {
            if (is != null) {
                reader = FACTORY.createXMLStreamReader(is);
                while (reader.hasNext()) {
                    String str = getElement(reader, "item");
                    if (str != null) {
                        ZonedDateTime pubdate = getDate(str); //gets the date of the item
                        if (lastpubdate == null || lastpubdate.isBefore(pubdate)) { //if it is a new item (the date of the item is after the lastpubdate)
                            if (time == null || pubdate.isAfter(time)) {//time will be the max date of the items in given RSS
                                time = pubdate;
                            }
                            bufferedWriter.write(str + "\n" + "\n");
                            bufferedWriter.flush();
                        }
                    }
                }
                if (time != null) {
                    lastpubdate = time;//lastpubdate is the max date of the items in the given RSS
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

    /**
     * Returns the content of the XML element
     * @param reader the instance of the {@link XMLStreamReader}
     * @param element name of the element to get
     * @return the contents of the {@code element}
     * @throws XMLStreamException
     */

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

    /**
     * Gets the date of the item
     * @param str the content of the item
     * @return the date of the item
     */
    private ZonedDateTime getDate(String str) {
        Pattern pattern = Pattern.compile("pubDate (.*)"); //gets the content of the pubdate element in RFC-822 format
        Matcher matcher = pattern.matcher(str);
        matcher.find();
        return ZonedDateTime.parse(matcher.group(1), DateTimeFormatter.RFC_1123_DATE_TIME); //converts from the RFC-822
    }
}