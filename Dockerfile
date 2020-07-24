FROM maven:3.6.3-jdk-11-slim AS build
WORKDIR /app
COPY . /app/
RUN mvn clean package

FROM openjdk:latest
WORKDIR /app
COPY --from=build /app/target/ /app
COPY --from=build /app/res/ /app/res
CMD java -javaagent:"/app/res/dd-java-agent.jar" -Ddd.profiling.enabled=true -Ddd.profiling.api-key-file=res/ddkey.txt -Ddd.trace.analytics.enabled=true -jar vinny-rss-1.0.jar