FROM openjdk:11-jdk-slim
WORKDIR /app
COPY . .
RUN mkdir -p out && javac -d out -cp "web/WEB-INF/lib/*:src" src/*.java
CMD ["java", "-jar", "web/WEB-INF/lib/jetty-runner-9.4.53.v20231009.jar", "--port", "8080", "--classes", "out", "web"]
