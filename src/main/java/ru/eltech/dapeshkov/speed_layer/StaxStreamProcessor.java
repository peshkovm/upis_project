package ru.eltech.dapeshkov.speed_layer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;

class StaxStreamProcessor {
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    static void parse(InputStream is, String file) {
        XMLStreamReader reader = null;
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            System.out.println(Thread.currentThread().getId());
            if (is != null) {
                reader = FACTORY.createXMLStreamReader(is);
                while (reader.hasNext()) {
                    String str = getElement(reader, "item");
                    if (str != null) {
                        bufferedWriter.write(str + "\n" + "\n");
                        bufferedWriter.flush();
                    }
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

    private static String getElement(XMLStreamReader reader, String element) throws XMLStreamException {
        StringBuilder str = new StringBuilder();
        while (reader.hasNext() && (reader.next() != XMLStreamReader.START_ELEMENT || !reader.getLocalName().equals(element))) {
        }

        while (reader.hasNext() && (reader.next() != XMLStreamReader.END_ELEMENT || !reader.getLocalName().equals(element))) {
            if (reader.getEventType() == XMLStreamReader.START_ELEMENT) {
                str.append(reader.getLocalName()).append(" ").append(reader.getElementText()).append("\n");
            }
        }
        return str.length() == 0 ? null : str.deleteCharAt(str.length() - 1).toString();
    }
}