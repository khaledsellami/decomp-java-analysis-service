FROM maven:3.8-adoptopenjdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app/pom.xml
RUN mvn -f /usr/src/app/pom.xml clean compile assembly:single

FROM gcr.io/distroless/java AS DEPLOY
COPY --from=build /usr/src/app/target/DecompAnalysis.jar /usr/app/DecompAnalysis.jar
#COPY data_prod/ /usr/app/data/
WORKDIR /usr/app
EXPOSE 50100
ENTRYPOINT ["java","-jar","/usr/app/DecompAnalysis.jar"]
