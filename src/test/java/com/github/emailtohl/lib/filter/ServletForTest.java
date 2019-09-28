package com.github.emailtohl.lib.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class ServletForTest extends HttpServlet {
	private static final long serialVersionUID = -3387990658424986228L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String name = req.getParameter("name");
		String content = "hello " + name;
//		resp.getOutputStream().print(content);
		PrintWriter w = resp.getWriter();
		w.print(content);
	}
}
