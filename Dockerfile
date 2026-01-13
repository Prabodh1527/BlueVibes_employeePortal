FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

FROM tomcat:9.0-jdk17

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Pass Render env variables into Java
ENV JAVA_OPTS="\
 -DDB_HOST=${DB_HOST} \
 -DDB_NAME=${DB_NAME} \
 -DDB_USER=${DB_USER} \
 -DDB_PASSWORD=${DB_PASSWORD} \
 -DDB_PORT=${DB_PORT} \
 -DCLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME} \
 -DCLOUDINARY_API_KEY=${CLOUDINARY_API_KEY} \
 -DCLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET}"

EXPOSE 8080
CMD ["sh", "-c", "catalina.sh run"]
