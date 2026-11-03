# 갈곳 백엔드(Spring Boot) 이미지
# 1단계: Gradle로 실행 가능한 jar를 만든다(테스트는 PC에서 돌리므로 여기서는 뺀다)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
# 메모리 2GB 서버에서 빌드하므로 Gradle 메모리를 작게 잡는다
ENV GRADLE_OPTS="-Xmx768m -Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx768m"

# 의존성만 먼저 받아 두면, 소스만 바뀐 다음 빌드에서는 이 단계를 다시 쓰지 않는다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies > /dev/null

COPY src src
RUN ./gradlew bootJar -x test && cp build/libs/*.jar app.jar

# 2단계: 실행에는 JRE만 둔다
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 galgot
COPY --from=build /workspace/app.jar app.jar
USER galgot
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=192m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
