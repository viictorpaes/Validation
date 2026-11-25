# Stage 1: Build Spring Boot Application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Production Runtime with OpenCV Native Dependencies & Tesseract OCR
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-por \
    tesseract-ocr-eng \
    libtesseract-dev \
    libleptonica-dev \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    libgomp1 \
    fontconfig \
    fonts-dejavu-core \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && TESS_DIR=$(dirname $(find /usr/share -name "por.traineddata" | head -n 1)) \
    && ln -sfn "$TESS_DIR" /usr/share/tessdata \
    && groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app/storage/uploads \
    && chown -R spring:spring /app

ENV TESSDATA_PREFIX=/usr/share/tessdata
ENV TESSERACT_DATAPATH=/usr/share/tessdata
ENV UPLOAD_STORAGE_DIR=/app/storage/uploads

COPY --from=build /workspace/target/*.jar /app/app.jar

USER spring:spring
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
