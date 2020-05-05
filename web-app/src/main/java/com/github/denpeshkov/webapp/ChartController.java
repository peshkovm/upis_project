package com.github.denpeshkov.webapp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.eltech.utils.PathEventsListener;
import ru.eltech.utils.PropertiesClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ChartController {
    private final SseEmitterFactory sseEmitterFactory;

    public ChartController(SseEmitterFactory sseEmitterFactory) {
        this.sseEmitterFactory = sseEmitterFactory;
    }

    static private class ChartData {
        @JsonUnwrapped
        ChartPoint real;
        @JsonUnwrapped
        ChartPoint predict;

        public ChartPoint getReal() {
            return real;
        }

        public ChartPoint getPredict() {
            return predict;
        }

        private static class ChartPoint {
            LocalDateTime dateTime;

            public ChartPoint(LocalDateTime dateTime, double value, Type type) {
                this.dateTime = dateTime;
                this.value = value;
                this.type = type;
            }

            double value;
            Type type;

            enum Type {
                REAL, PREDICT;

                @JsonValue
                @Override
                public String toString() {
                    return name().toLowerCase();
                }
            }

            public LocalDateTime getDateTime() {
                return dateTime;
            }

            public double getValue() {
                return value;
            }

            public Type getType() {
                return type;
            }
        }

        public ChartData(LocalDateTime dateTime, double real, double predict) {
            this.real = new ChartPoint(dateTime, real, ChartPoint.Type.REAL);
            this.predict = new ChartPoint(dateTime, predict, ChartPoint.Type.PREDICT);
        }
    }

    @CrossOrigin
    @GetMapping("/chartData")
    SseEmitter getChartData() {
        SseEmitter emitter = new SseEmitter();
        sseEmitterFactory.setSseEmitter(emitter, this.getClass());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final String streamingViewDirectory = PropertiesClass.getStreamingViewDirectory();

        executor.execute(() ->
        {
            PathEventsListener.doJobOnEvent(Paths.get(streamingViewDirectory), StandardWatchEventKinds.ENTRY_CREATE, (companyPath) -> new Thread(() -> PathEventsListener.doJobOnEvent(companyPath, StandardWatchEventKinds.ENTRY_CREATE, feedPath -> {
                try {
                    Files.lines(feedPath).map(feed -> {
                        String[] split = feed.split(",");
                        double realPrice = Double.parseDouble(split[0]);
                        double predictPrice = -1;
                        if (!split[1].equals("null"))
                            predictPrice = Double.parseDouble(split[1]);

                        return new ChartData(LocalDateTime.now(), realPrice, predictPrice);
                    }).forEach(chartData -> {
                        try {
                            emitter.send(SseEmitter.event().name(companyPath.getFileName().toString()).data(chartData.real));
                            if (chartData.predict.value != -1) {
                                emitter.send(SseEmitter.event().name(companyPath.getFileName().toString()).data(chartData.predict));
                            }
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                            throw new RuntimeException(e);
                        }
                    });

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, PathEventsListener.DoJobUntilStrategies.FEEDS, false)).start(), PathEventsListener.DoJobUntilStrategies.COMPANIES, true);

            emitter.complete();
        });

        executor.shutdown();

        return emitter;
    }
}
