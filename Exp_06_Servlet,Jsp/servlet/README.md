# Experiment 06: Java Servlets and JSP

## Aim
To develop and run a Java web application using Servlets and JavaServer Pages (JSP).

## Overview
Servlets handle requests on the server side, while JSP is used to create dynamic web pages. Together, they can process user input and display generated responses in a browser.

## Technologies Used
- Java
- Java Servlets
- JavaServer Pages (JSP)
- Apache Maven
- Apache Tomcat

## Requirements
- JDK compatible with the project's configuration
- Apache Maven
- Apache Tomcat compatible with the Servlet/JSP API used by the project
- A browser

## How to Run
1. Open a terminal in the folder containing this experiment's `pom.xml`.
2. Build the project:

   ```bash
   mvn clean package
   ```

3. After a successful build, locate the generated WAR file in the `target` folder.
4. Deploy the WAR file to the Tomcat `webapps` directory, or use the deployment method configured for your project.
5. Start Tomcat and open the application URL in your browser.

## Important Notes
- Run Maven from the directory that contains `pom.xml`; otherwise Maven may report that no POM is present.
- The exact URL and deployment steps depend on the application's context path and Tomcat configuration.
- Do not commit passwords or other private configuration values.
