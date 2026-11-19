package com.nutriscan.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Controller for application root and OAuth redirect routing.
 */
@Controller
public class HomeController {

    /**
     * Root endpoint providing API status message.
     */
    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "NutriScan API is running. Access the student frontend at http://localhost:5173";
    }

    /**
     * Fallback redirect endpoint to route authenticated users to the React frontend dashboard.
     */
    @GetMapping("/oauth2/redirect")
    public void oauth2Redirect(HttpServletResponse response) throws IOException {
        response.sendRedirect("http://localhost:5173/dashboard");
    }
}
