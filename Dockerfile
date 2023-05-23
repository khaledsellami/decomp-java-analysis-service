FROM maven:3.8-adoptopenjdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app/pom.xml
RUN mkdir /usr/src/app/data
RUN mkdir /usr/src/app/data/repositories
RUN mvn -f /usr/src/app/pom.xml clean compile assembly:single

FROM gcr.io/distroless/java
#--platform=linux/amd64
COPY --from=build /usr/src/app/target/java-analysis-service-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/app/java-analysis-service-1.0-SNAPSHOT.jar
#RUN mkdir /usr/app/data
#RUN mkdir /usr/app/data/repositories
COPY --from=build /usr/src/app/data /usr/app
EXPOSE 50100
ENTRYPOINT ["java","-jar","/usr/app/java-analysis-service-1.0-SNAPSHOT.jar"]
