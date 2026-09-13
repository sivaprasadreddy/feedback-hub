package dev.sivalabs.feedbackhub.config;

import dev.sivalabs.feedbackhub.ApplicationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebMvcConfig implements WebMvcConfigurer {
    private final ApplicationProperties props;

    WebMvcConfig(ApplicationProperties props) {
        this.props = props;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/admin/messages").setViewName("admin/messages");
    }
}
