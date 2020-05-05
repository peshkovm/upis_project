package com.github.denpeshkov.webapp;

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
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class NewsController {
    private final SseEmitterFactory sseEmitterFactory;

    private static class News {
        String title;
        String description;
        Timestamp dateTime;
        Sentiment sentiment;
        Company company;

        public News(String title, String description, Timestamp dateTime, Sentiment sentiment, Company company) {
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

        public String getDateTime() {
            return dateTime.toString();
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
        final String liveFeedDirectory = PropertiesClass.getLiveFeedDirectory();

        executor.execute(() ->
        {
            PathEventsListener.doJobOnEvent(Paths.get(liveFeedDirectory), StandardWatchEventKinds.ENTRY_CREATE, (companyPath) -> new Thread(() -> PathEventsListener.doJobOnEvent(companyPath, StandardWatchEventKinds.ENTRY_CREATE, feedPath -> {
                try {
                    Files.lines(feedPath).map(feed -> {
                        String[] split = feed.split(",");
                        String companyName = split[0];
                        String sentiment = split[1];
                        String dateTime = split[2];

                        return new News("title", "description", Timestamp.valueOf(dateTime), News.Sentiment.valueOf(sentiment.toUpperCase()), Company.valueOf(companyName.toUpperCase()));
                    }).forEach(news -> {
                        try {
                            emitter.send(SseEmitter.event().name("news").data(news));
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
