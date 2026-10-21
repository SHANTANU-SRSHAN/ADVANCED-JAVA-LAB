package com.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        HttpSession session = request.getSession(false);
        String username = (String) session.getAttribute("username");
        PrintWriter out = response.getWriter();
        out.println("<h1>Student Home</h1>");
        out.println("<h2>Welcome " + username + "</h2>");
        out.println("<a href='logout'>Logout</a>");
    }
}
