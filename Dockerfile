FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM tomcat:9.0-jdk17

# Tell Tomcat it is running behind HTTPS (Render proxy)
RUN sed -i 's/port="8080"/port="8080" scheme="https" proxyPort="443"/' /usr/local/tomcat/conf/server.xml

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
