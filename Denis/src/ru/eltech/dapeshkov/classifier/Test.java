package ru.eltech.dapeshkov.classifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.eltech.dapeshkov.speed_layer.JSONProcessor;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Stream;

public class Test {
    public static void main(String[] args) {
        int[] a = {0, 0, 0};
        JSONProcessor.Train[] arr = null;

        try (FileInputStream in = new FileInputStream("Denis/train1.json")) {
            arr = JSONProcessor.parse(in, JSONProcessor.Train[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Denis/train1.json")))) {
            JSONProcessor.Train[] res1 = Arrays.stream(arr).filter((t) -> t.getSentiment().equals("negative")).limit(1434).toArray(JSONProcessor.Train[]::new);
            JSONProcessor.Train[] res2 = Arrays.stream(arr).filter((t) -> t.getSentiment().equals("neutral")).limit(1434).toArray(JSONProcessor.Train[]::new);
            JSONProcessor.Train[] res3 = Arrays.stream(arr).filter((t) -> t.getSentiment().equals("positive")).limit(1434).toArray(JSONProcessor.Train[]::new);
            JSONProcessor.Train[] res4 = Stream.concat(Arrays.stream(res1), Stream.concat(Arrays.stream(res2), Arrays.stream(res3))).toArray(JSONProcessor.Train[]::new);
            ObjectMapper mapper = new ObjectMapper();
            String str = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(res4);
            out.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
