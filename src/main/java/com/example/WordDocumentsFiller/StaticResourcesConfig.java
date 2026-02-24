package com.example.WordDocumentsFiller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.VersionResourceResolver;

@Configuration
public class StaticResourcesConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"))
                .addResolver(new PathResourceResolver());

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"))
                .addResolver(new PathResourceResolver());

        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/")
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"))
                .addResolver(new PathResourceResolver());
    }
}
