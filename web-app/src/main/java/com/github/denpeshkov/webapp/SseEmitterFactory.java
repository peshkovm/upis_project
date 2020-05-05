package com.github.denpeshkov.webapp;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Component
public class SseEmitterFactory {
    private Map<Class<?>, SseEmitter> map = new HashMap<>();

    public void setSseEmitter(SseEmitter sseEmitter, Class<?> clazz) {
        map.put(clazz, sseEmitter);
    }

    public SseEmitter getSseEmitter(Class<?> clazz) {
        return map.get(clazz);
    }
}
