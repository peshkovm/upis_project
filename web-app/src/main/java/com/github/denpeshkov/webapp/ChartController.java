package com.github.denpeshkov.webapp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

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
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
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

        return emitter;
    }
}
