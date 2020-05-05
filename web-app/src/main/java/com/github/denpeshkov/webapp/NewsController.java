package com.github.denpeshkov.webapp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class NewsController {
    private final SseEmitterFactory sseEmitterFactory;
    private boolean complete = false;

    private static class News {
        String title;
        String description;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        LocalDateTime dateTime;
        Sentiment sentiment;
        Company company;

        public News(String title, String description, LocalDateTime dateTime, Sentiment sentiment, Company company) {
            this.title = title;
            this.description = description;
            this.dateTime = dateTime;
            this.sentiment = sentiment;
            this.company = company;
        }

        private enum Sentiment {
            POSITIVE,
            NEUTRAL,
            NEGATIVE;

            @JsonValue
            @Override
            public String toString() {
                return name().toLowerCase();
            }
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public Sentiment getSentiment() {
            return sentiment;
        }

        public Company getCompany() {
            return company;
        }
    }

    public NewsController(SseEmitterFactory sseEmitterFactory) {
        this.sseEmitterFactory = sseEmitterFactory;
    }

    @GetMapping("/news")
    @CrossOrigin
    public SseEmitter getNews() {
        SseEmitter emitter = new SseEmitter();
        sseEmitterFactory.setSseEmitter(emitter, this.getClass());

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() ->
        {
            try {
                if (!complete) {
                    for (int i = 0; i < 10; i++) {
                        emitter.send(SseEmitter.event().name("news").data(new News(i + "", "description", LocalDateTime.now(), News.Sentiment.POSITIVE, Company.GOOGLE)));
                        Thread.sleep(1000);
                    }
                    emitter.complete();
                    complete = true;
                }
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        executor.shutdown();

        return emitter;
    }
}
