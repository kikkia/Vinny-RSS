FROM maven:3.6.3-jdk-11-slim AS build
WORKDIR /app
COPY . /app/
RUN mvn clean package

FROM openjdk:latest
WORKDIR /app
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"
COPY --from=build /app/target/ /app
COPY --from=build /app/res/ /app/res
COPY --from=build /app/bootstrap.sh /app
RUN chmod -R 777 /app/bootstrap.sh
ENTRYPOINT exec java $JAVA_OPTS -jar /opt/issues.jar