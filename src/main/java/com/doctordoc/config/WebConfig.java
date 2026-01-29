package com.doctordoc.config;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Web MVC Configuration for Doctor-Doc.
 * Configures locale resolution, resource handling, and Thymeleaf dialects.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final DoctorDocProperties properties;

    public WebConfig(DoctorDocProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        // Set default locale based on configuration
        String defaultLocale = properties.getDefaultLocale();
        if ("de".equalsIgnoreCase(defaultLocale)) {
            resolver.setDefaultLocale(Locale.GERMAN);
        } else if ("fr".equalsIgnoreCase(defaultLocale)) {
            resolver.setDefaultLocale(Locale.FRENCH);
        } else {
            resolver.setDefaultLocale(Locale.ENGLISH);
        }
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
