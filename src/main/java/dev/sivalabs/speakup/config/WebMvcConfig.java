package dev.sivalabs.speakup.config;

import dev.sivalabs.speakup.ApplicationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebMvcConfig implements WebMvcConfigurer {
    private final ApplicationProperties props;

    WebMvcConfig(ApplicationProperties props) {
        this.props = props;
    }
}
