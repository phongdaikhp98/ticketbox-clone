# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM gradle:8.7-jdk17-alpine AS builder
WORKDIR /app

# Cache dependencies trước (chỉ re-download khi build.gradle thay đổi)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon --quiet 2>/dev/null || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 -S appgroup && adduser -S appuser -u 1001 -G appgroup

COPY --from=builder --chown=appuser:appgroup /app/build/libs/ticketbox-*.jar app.jar

USER appuser
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
