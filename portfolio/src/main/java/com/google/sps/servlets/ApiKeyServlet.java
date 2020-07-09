package com.google.sps.servlets;

import java.io.IOException;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/apiKeyServlet")
public class ApiKeyServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Map<String, String> env = System.getenv();
    String apiKey = env.get("API_KEY");
    response.setContentType("text/html");
    response.getWriter().println(apiKey);
  }
}
