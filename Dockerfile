FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM tomcat:9.0-jdk17

# Tell Tomcat it is behind HTTPS (Render proxy)
RUN sed -i 's/port="8080"/port="8080" scheme="https" proxyPort="443"/' /usr/local/tomcat/conf/server.xml

# Inject Render environment variables into Java
ENV JAVA_OPTS="-DDB_HOST=${DB_HOST} -DDB_NAME=${DB_NAME} -DDB_USER=${DB_USER} -DDB_PASSWORD=${DB_PASSWORD} -DDB_PORT=${DB_PORT}"

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["sh", "-c", "catalina.sh run"]
