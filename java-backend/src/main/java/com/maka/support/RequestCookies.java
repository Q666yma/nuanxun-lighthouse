package com.maka.support;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Forward only the authenticated browser session to the local Python service. */
public final class RequestCookies {
    private RequestCookies() { }
    public static void forward(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            String cookie = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader("Cookie");
            if (cookie != null) headers.set(HttpHeaders.COOKIE, cookie);
        }
    }
}
