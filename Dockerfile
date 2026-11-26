FROM eclipse-temurin:21-jdk AS build

ENV HOME=/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY . $HOME

# If you have a Vaadin Pro key, pass it as a secret with id "proKey":
#
#   $ docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
#
# If you have a Vaadin Offline key, pass it as a secret with id "offlineKey":
#
#   $ docker build --secret id=offlineKey,src=$HOME/.vaadin/offlineKey .

# The production bundle needs the licence; -Pai brings the model's library into the jar, and the
# model answers when the container is given OPENAI_API_KEY.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=proKey \
    --mount=type=secret,id=offlineKey \
    sh -c 'PRO_KEY=$(sed -n "s/.*\"proKey\" *: *\"\([^\"]*\)\".*/\1/p" /run/secrets/proKey 2>/dev/null || echo "") && \
    OFFLINE_KEY=$(cat /run/secrets/offlineKey 2>/dev/null || echo "") && \
    ./mvnw clean package -Pproduction,ai -DskipTests -Dvaadin.proKey=${PRO_KEY} -Dvaadin.offlineKey=${OFFLINE_KEY}'

# Split the JAR into layers, so that the dependencies get their own image layer
RUN cp target/*.jar application.jar && \
    java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /application
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
EXPOSE 8080
# Let the heap use 75% of the container's memory limit, instead of the default 25%.
# How people sign in is the container's to say: SPRING_PROFILES_ACTIVE=cas with
# SECURITYSTATUS_CAS_SERVICE, or demo; with neither the application does not start.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
