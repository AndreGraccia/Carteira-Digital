package com.github.yildizmy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An endpoint that takes an id in the path reads someone's data. It must either check ownership with
 * {@code @PreAuthorize} or take the logged-in user ({@code @AuthenticationPrincipal}) and scope by it.
 * Forgetting both is how another user's statement leaks by changing the number in the URL.
 */
class ControllerAccessTest {

    @Test
    void everyEndpointWithAnIdInThePathChecksWhoIsAsking() throws Exception {
        List<Class<?>> controllers = controllers();
        assertTrue(controllers.size() >= 5, "found " + controllers.size() + " controllers");

        List<String> unguarded = new ArrayList<>();
        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isHandler(method)) continue;
                Parameter[] params = method.getParameters();
                boolean idInPath = Arrays.stream(params).anyMatch(p -> p.isAnnotationPresent(PathVariable.class));
                boolean scopedToCaller = Arrays.stream(params).anyMatch(p -> p.isAnnotationPresent(AuthenticationPrincipal.class));
                boolean checked = method.isAnnotationPresent(PreAuthorize.class) || controller.isAnnotationPresent(PreAuthorize.class);
                if (idInPath && !scopedToCaller && !checked) unguarded.add(controller.getSimpleName() + "." + method.getName());
            }
        }
        assertEquals(List.of(), unguarded);
    }

    private static boolean isHandler(Method method) {
        return Arrays.stream(method.getAnnotations())
                .anyMatch(a -> a.annotationType() == RequestMapping.class
                        || a.annotationType().isAnnotationPresent(RequestMapping.class));
    }

    private static List<Class<?>> controllers() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        List<Class<?>> found = new ArrayList<>();
        for (var resource : resolver.getResources("classpath*:com/github/yildizmy/controller/*.class")) {
            String name = "com.github.yildizmy.controller." + resource.getFilename().replace(".class", "");
            if (name.contains("$")) continue;
            Class<?> type = Class.forName(name);
            if (type.isAnnotationPresent(RestController.class)) found.add(type);
        }
        return found;
    }
}
