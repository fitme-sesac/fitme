### Build stage
FROM gradle:8.8-jdk21 AS build
WORKDIR /app

# (권장) wrapper 먼저 복사 + CRLF 정리 + 실행권한
COPY gradlew gradlew.bat /app/
COPY gradle /app/gradle

# build scripts (kts/일반 둘 다 대응)
COPY build.gradle* settings.gradle* gradle.properties* /app/

RUN sed -i 's/\r$//' /app/gradlew && chmod +x /app/gradlew

# Gradle cache (BuildKit 사용 시 캐시 효율↑)
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew --no-daemon dependencies -x test || true

# 소스 전체 복사
COPY . /app

# 빌드
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew --no-daemon clean bootJar -x test || \
    ./gradlew --no-daemon clean build -x test

# 실행 아티팩트를 app.jar로 고정 (plain/sources 제외)
RUN set -eux; \
    f="$(ls -1 /app/build/libs/*.jar 2>/dev/null | grep -vE '(plain|sources|javadoc)\.jar$' | head -n 1 || true)"; \
    if [ -z "$f" ]; then f="$(ls -1 /app/build/libs/*.war | head -n 1)"; fi; \
    cp "$f" /app/app.jar

### Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Seoul

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
