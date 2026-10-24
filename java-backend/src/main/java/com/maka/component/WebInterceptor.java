package com.maka.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;

/** Session enforcement for application controllers; static assets remain public. */
@Component
public class WebInterceptor implements HandlerInterceptor {
    @org.springframework.beans.factory.annotation.Autowired private com.maka.support.RoleAccess roleAccess;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") instanceof String) {
            try {
                roleAccess.current(session);
                HandlerMethod route=(HandlerMethod)handler;
                String controller=route.getMethod().getDeclaringClass().getName();
                if (java.util.Arrays.asList("com.maka.controller.RescueOldManController","com.maka.controller.Four0FourController").contains(controller)
                    || ("com.maka.controller.RescueOldManController$PageController".equals(controller) && "systemSettings".equals(route.getMethod().getName())))
                    roleAccess.requireRole(session,"admin");
                return true;
            } catch (org.springframework.web.server.ResponseStatusException error) {
                response.setStatus(error.getStatus().value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"code\":"+error.getStatus().value()+",\"message\":\"账号不可用或没有访问权限，请重新登录或联系管理员\"}");
                return false;
            }
        }
        HandlerMethod method = (HandlerMethod) handler;
        boolean json = method.hasMethodAnnotation(ResponseBody.class)
                || method.getBeanType().isAnnotationPresent(RestController.class)
                || request.getRequestURI().startsWith("/api/");
        if (json) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"success\":false,\"msg\":\"请先登录\",\"message\":\"请先登录\"}");
        } else {
            response.sendRedirect("/login");
        }
        return false;
    }
}
