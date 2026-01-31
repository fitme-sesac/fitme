package com.example.pproject.Config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    /**
     * IMPORTANT:
     * Vite dev proxy(changeOrigin=true) 사용 시, 백엔드가 보는 Host가 "backend:8080"가 되어
     * 상대경로 redirect("/Login")가 브라우저에서 "http://backend:8080/Login"으로 변환될 수 있다.
     * 따라서 실패 redirect는 항상 브라우저가 접근 가능한 FRONT_BASE_URL로 보낸다.
     */
    @Value("${app.front-base-url:http://localhost:5173}")
    private String frontBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String errorMessage;

        if (exception instanceof UsernameNotFoundException) {
            errorMessage = "해당 아이디는 없는 계정입니다.";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "비밀번호를 잘못 입력하셨습니다.";
        } else {
            errorMessage = "알 수 없는 이유로 로그인에 실패했습니다.";
        }

        String encoded = java.net.URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect(frontBaseUrl + "/Login?errorMessage=" + encoded);
    }
}
