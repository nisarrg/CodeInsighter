package com.taim.conduire.config;

import com.taim.conduire.config.CustomFilter.CustomContentTypeFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/");
    }

    @Bean
    public FilterRegistrationBean<CustomContentTypeFilter> registerCustomContentTypeFilter() {
        FilterRegistrationBean<CustomContentTypeFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CustomContentTypeFilter());
        registrationBean.addUrlPatterns("/assets/js/*"); // Specify the URL pattern to filter
        return registrationBean;
    }
}