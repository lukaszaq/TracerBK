package pl.tracer.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(TracerProperties.class)
@ConditionalOnProperty(prefix = "tracer", name = "enabled", matchIfMissing = true)
public class TracerAutoConfiguration {

    @Bean
    EventSender tracerEventSender(TracerProperties p) { return new EventSender(p.getCollectorUrl()); }

    @Bean
    TrackingRestInterceptor trackingRestInterceptor(TracerProperties p, EventSender s, Environment env) {
        return new TrackingRestInterceptor(p, s, self(p, env));
    }

    static String self(TracerProperties p, Environment env) {
        return p.getServiceName() != null ? p.getServiceName()
                : env.getProperty("spring.application.name", "unknown-service");
    }

    // działa dla RestClient.Builder / RestTemplateBuilder wstrzykiwanych przez Springa
    @Bean
    RestClientCustomizer tracerRestClientCustomizer(TrackingRestInterceptor i) {
        return builder -> builder.requestInterceptor(i);
    }

    @Bean
    RestTemplateCustomizer tracerRestTemplateCustomizer(TrackingRestInterceptor i) {
        return rt -> rt.getInterceptors().add(i);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class WebConfig {
        @Bean
        FilterRegistrationBean<TrackingFilter> trackingFilter(TracerProperties p, EventSender s, Environment env) {
            FilterRegistrationBean<TrackingFilter> reg =
                    new FilterRegistrationBean<>(new TrackingFilter(p, s, self(p, env)));
            reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return reg;
        }
    }
}
