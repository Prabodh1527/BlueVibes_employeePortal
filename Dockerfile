# Use the new official Java 11 image
FROM eclipse-temurin:11-jdk

WORKDIR /app
COPY . .

# Create 'out' folder and compile
RUN mkdir -p out && javac -d out -cp "web/WEB-INF/lib/*:src" src/*.java

# Start the app
CMD ["java", "-jar", "web/WEB-INF/lib/jetty-runner-9.4.53.v20231009.jar", "--port", "8080", "--classes", "out", "web"]
