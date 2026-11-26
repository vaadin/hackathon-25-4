FROM eclipse-temurin:21-jdk AS build
# The Node.js that Vaadin downloads needs libatomic, which this image does not carry.
RUN apt-get update && apt-get install -y --no-install-recommends libatomic1 && rm -rf /var/lib/apt/lists/*

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
#
# Where secrets cannot be mounted, as on v-herd, a server licence key can come as a build
# argument instead. With no key at all the build still succeeds, showing Vaadin's banner.
#
#   $ docker build --build-arg offlinekey=<server licence key> .
ARG offlinekey=""

# The production bundle needs the licence; -Pai brings the model's library into the jar, and the
# model answers when the container is given OPENAI_API_KEY. A container that gets no key must be
# built without it, or it does not start: --build-arg mavenprofiles=production
ARG mavenprofiles=production,ai
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=proKey \
    --mount=type=secret,id=offlineKey \
    sh -c 'PRO_KEY=$(sed -n "s/.*\"proKey\" *: *\"\([^\"]*\)\".*/\1/p" /run/secrets/proKey 2>/dev/null || echo "") && \
    OFFLINE_KEY=$(cat /run/secrets/offlineKey 2>/dev/null || echo "${offlinekey}") && \
    BANNER=$([ -z "${PRO_KEY}${OFFLINE_KEY}" ] && echo true || echo false) && \
    ./mvnw clean package -P${mavenprofiles} -DskipTests -Dvaadin.proKey=${PRO_KEY} -Dvaadin.offlineKey=${OFFLINE_KEY} -Dproduction.commercialWithBanner=${BANNER}'

# Split the JAR into layers, so that the dependencies get their own image layer
RUN cp target/*.jar application.jar && \
    java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine
# How people sign in, baked in where the host passes no environment, as on v-herd:
# --build-arg profile=demo. Empty by default, so the container still has to say it.
ARG profile=""
ENV SPRING_PROFILES_ACTIVE=${profile}
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /application
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
EXPOSE 8080
# Let the heap use 65% of the container's memory limit, instead of the default 25%; the rest is
# metaspace and threads, which a 512 MB container like v-herd's needs.
# How people sign in is the container's to say: SPRING_PROFILES_ACTIVE=cas with
# SECURITYSTATUS_CAS_SERVICE, or demo; with neither the application does not start.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "application.jar"]
