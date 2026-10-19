FROM openjdk:11

EXPOSE 8080

RUN echo 'Asia/Shanghai' > /etc/timezone
ENV TZ Asia/Shanghai

ADD ./aopa-db-1.0.0.jar /app.jar
ENTRYPOINT ["java", "-Xms30m", "-Xmx512m", "-jar", "/app.jar", "--server.port=8080",  "--spring.profiles.active=dev"]

