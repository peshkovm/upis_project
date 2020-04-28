package ru.eltech.gui;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BigFileTableModel extends AbstractTableModel {

    private static final int COLUMN_COUNT = 2;
    private static final String[] COLUMN_NAMES = {"Line", "Text"};
    private static final Class<?>[] COLUMN_CLASSES = {Integer.class, String.class};

    private final List<Integer> linePositions = new ArrayList<>();
    private MappedByteBuffer buffer;
    private String filePath;
    private int fileSize;
    public boolean isMousePressed = false;
    private int numOfLongestRow;

    public BigFileTableModel(String filePath) {
        try {
            setFilePath(filePath);
            readFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setFilePath(String filePath) {
        linePositions.clear();
        linePositions.add(0);
        this.filePath = filePath;
    }

    public void close() {
        buffer = null;
        System.gc();
    }

    private void readFile() throws IOException {
        try (FileChannel fileChannel = FileChannel.open(Paths.get(filePath))) {

            long fileLength = fileChannel.size();

            if (fileLength > Integer.MAX_VALUE) {
                throw new IOException("File too large, " + fileLength + " > " + Integer.MAX_VALUE);
            }

            int fileSize = (int) fileLength;

            this.fileSize = fileSize;

            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

            long lineLength = 0, maxLineLength = 0;
            for (int pos = 0; pos < fileSize; ++pos) {
                byte b = buffer.get();
                lineLength++;
                if (b == '\n') {
                    if (lineLength > maxLineLength) {
                        numOfLongestRow = linePositions.size() - 1;
                        maxLineLength = lineLength;
                    }
                    lineLength = 0;

                    linePositions.add(pos + 1);
                }
            }

            if (fileSize > linePositions.get(linePositions.size() - 1)) {
                linePositions.add(fileSize); // Last line without newline character.
            }

            System.out.println("readFile отработал");
        }
    }

    @Override
    public int getRowCount() {
        // Starts with 0 and final end position is not a row.
        return linePositions.size() - 1;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int i) {
        return COLUMN_NAMES[i];
    }

    @Override
    public Class<?> getColumnClass(int i) {
        return COLUMN_CLASSES[i];
    }

    @Override
    public boolean isCellEditable(int i, int i1) {
        return false; //ii == 1;
    }

    /*
    Вызывается EDT в момент прорисовки таблицы.
    Возвращается только часть файла, которая будет видна на экране.
    Весь файл не возвращается, экономя время получения значения из буфера.
     */
    @Override
    public Object getValueAt(int i, int i1) {
        if (!isMousePressed) {
            System.out.println("row = " + (i + 1));

            if (i1 == 0) {
                return i + 1; //начать отчет номеров строк с 1 вместо 0
            }
            if (0 <= i && i < getRowCount()) {
                int startPos = linePositions.get(i);
                int endPos = linePositions.get(i + 1) - 1;
                byte[] line = new byte[endPos - startPos];
                buffer.position(startPos);
                buffer.get(line);
                String s = new String(line, StandardCharsets.UTF_8); // UTF-8!
                if (s.endsWith("\r")) {
                    s = s.substring(0, s.length() - 1);
                }
                return s;
            } else
                return "";
        } else {
            if (i1 == 0) {
                return i + 1; //начать отчет номеров строк с 1 вместо 0
            }
            return "data is loading . . .";
        }
    }

    @Override
    public void setValueAt(Object o, int i, int i1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getNumOfLongestRow() {
        return numOfLongestRow;
    }

    public int getFileSize() {
        return fileSize;
    }
}