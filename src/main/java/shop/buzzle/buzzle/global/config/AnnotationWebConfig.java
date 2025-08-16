package shop.buzzle.buzzle.global.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import shop.buzzle.buzzle.global.annotationresolver.CurrentUserEmailArgumentResolver;

@Configuration
@RequiredArgsConstructor
public class AnnotationWebConfig implements WebMvcConfigurer {

    private final CurrentUserEmailArgumentResolver currentUserEmailArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserEmailArgumentResolver);
    }
}