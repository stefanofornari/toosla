package ste.toosla.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.logging.Logger;

@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = Logger.getLogger(AccessLogInterceptor.class.getName());

    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true; // Continue processing the request
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = (startTime != null) ? (System.currentTimeMillis() - startTime) : -1;

        String status = String.valueOf(response.getStatus());
        if (ex != null) {
            status = "ERROR (" + ex.getClass().getSimpleName() + ")";
        }

        LOGGER.info(String.format("Request: %s %s - Status: %s - Duration: %dms",
                request.getMethod(),
                request.getRequestURI(),
                status,
                duration));
    }
}