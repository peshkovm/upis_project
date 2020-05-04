package ru.eltech.dapeshkov.speed_layer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;

public class JSONProcessor {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JSONProcessor() {

    }

    public static <T> T parse(final String str, final Class<T> cl) {
        T json = null;
        if (str != null) {
            try {
                json = mapper.readValue(str, cl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    public static <T> T parse(final InputStream in, final Class<T> cl) {
        T json = null;
        if (in != null) {
            try {
                json = mapper.readValue(in, cl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    public static <T> String write(final T obj) {
        String res = null;
        try {
            res = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return res;
    }

    static class Item {
        private String anons;

        @Override
        public String toString() {
            return anons;
        }

        private String authors;
        private String category;
        private String fronturl;
        private String id;
        private String opinion_authors;

        private Photo photo;

        public void setPhoto(final Photo photo) {
            this.photo = photo;
        }

        public Photo getPhoto() {
            return photo;
        }

        private String project;

        public LocalDateTime getPublish_date() {
            return publish_date;
        }

        public void setPublish_date(final LocalDateTime publish_date) {
            this.publish_date = publish_date;
        }

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss Z", locale = "en_GB")
        private LocalDateTime publish_date;
        private String title;

        public void setAnons(final String anons) {
            this.anons = anons;
        }

        public void setAuthors(final String authors) {
            this.authors = authors;
        }

        public void setCategory(final String category) {
            this.category = category;
        }

        public void setFronturl(final String fronturl) {
            this.fronturl = fronturl;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public void setOpinion_authors(final String opinion_authors) {
            this.opinion_authors = opinion_authors;
        }

        public void setProject(final String project) {
            this.project = project;
        }

        public void setTitle(final String title) {
            this.title = title;
        }

        public String getAnons() {
            return anons;
        }

        public String getAuthors() {
            return authors;
        }

        public String getCategory() {
            return category;
        }

        public String getFronturl() {
            return fronturl;
        }

        public String getId() {
            return id;
        }

        public String getOpinion_authors() {
            return opinion_authors;
        }

        public String getProject() {
            return project;
        }

        public String getTitle() {
            return title;
        }
    }

    static class Photo {
        @Override
        public String toString() {
            return "Photo{" +
                    "url='" + url + '\'' +
                    '}';
        }

        private String url;

        public void setUrl(final String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class News {
        private Item[] items;

        public void setItems(final Item... items) {
            this.items = items;
        }

        @Override
        public String toString() {
            return Arrays.toString(items);
        }

        public Item[] getItems() {
            return items;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Train {
        String text;
        String sentiment;

        @Override
        public String toString() {
            return "Train{" +
                    "text='" + text + '\'' +
                    ", sentiment='" + sentiment + '\'' +
                    '}';
        }

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }

        public String getSentiment() {
            return sentiment;
        }

        public void setSentiment(final String sentiment) {
            this.sentiment = sentiment;
        }
    }
}