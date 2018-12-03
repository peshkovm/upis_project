package ru.eltech.dapeshkov.speed_layer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StaxStreamProcessor {
    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    public static void parse(InputStream is, OutputStream out) {
        try {
            System.out.println(Thread.currentThread().getId());
            if (is != null) {
                XMLStreamReader reader = FACTORY.createXMLStreamReader(is);
                boolean flag = false;
                while (reader.hasNext()) {       // while not end of XML
                    int event = reader.next();   // read next event
                    if (event == XMLStreamReader.START_ELEMENT && "item".equals(reader.getLocalName())) {
                        flag = true;
                    }
                    if (event == XMLStreamReader.START_ELEMENT && flag && ("title".equals(reader.getLocalName()) || "link".equals(reader.getLocalName())
                            || "description".equals(reader.getLocalName()) || "category".equals(reader.getLocalName()))) {
                        out.write((reader.getLocalName() + " " + reader.getElementText() + System.lineSeparator()).getBytes());
                    }
                    if (event == XMLStreamReader.END_ELEMENT && "item".equals(reader.getLocalName())) {
                        out.write(System.lineSeparator().getBytes());
                        flag = false;
                    }
                }
                reader.close();
            }
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
        }
    }
}