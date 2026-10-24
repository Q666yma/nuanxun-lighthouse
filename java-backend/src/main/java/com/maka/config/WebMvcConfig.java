package com.maka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 把磁盘 uploads 目录映射到  http://<host>/uploads/**
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 运行目录下的 /uploads/ 绝对路径
        String absPath = System.getProperty("user.dir")
                        + File.separator + "uploads" + File.separator;

        registry.addResourceHandler("/uploads/**")          // URL 前缀
                .addResourceLocations("file:" + absPath);   // 对应磁盘路径
    }
}
