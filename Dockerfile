# ── Stage 1: Build all modules ────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-noble AS builder

ARG MAVEN_VERSION=3.9.9
RUN apt-get update && apt-get install -y --no-install-recommends wget ca-certificates && \
    wget -q https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt && \
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    apt-get remove -y wget && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*
ENV PATH="/opt/apache-maven-${MAVEN_VERSION}/bin:${PATH}"

WORKDIR /build

# Copy poms first for better layer caching
COPY pom.xml ./
COPY hft-core/pom.xml hft-core/
COPY order-entry-service/pom.xml order-entry-service/
COPY position-service/pom.xml position-service/
COPY risk-dashboard-service/pom.xml risk-dashboard-service/
COPY notification-service/pom.xml notification-service/
COPY audit-service/pom.xml audit-service/
COPY execution-engine/pom.xml execution-engine/
RUN mvn dependency:go-offline --batch-mode || true

COPY . .
RUN mvn clean package -Pdocker -DskipTests --batch-mode

# ── Stage 2: order-entry-service ──────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble AS order-entry-service
WORKDIR /app
COPY --from=builder /build/order-entry-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# ── Stage 3: position-service ─────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble AS position-service
WORKDIR /app
COPY --from=builder /build/position-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# ── Stage 4: risk-dashboard-service ───────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble AS risk-dashboard-service
WORKDIR /app
COPY --from=builder /build/risk-dashboard-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# ── Stage 5: notification-service ─────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble AS notification-service
WORKDIR /app
COPY --from=builder /build/notification-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# ── Stage 6: audit-service ────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble AS audit-service
WORKDIR /app
COPY --from=builder /build/audit-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# ── Stage 7: execution-engine ─────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-noble AS execution-engine
WORKDIR /app
COPY --from=builder /build/execution-engine/target/execution-engine.jar app.jar
ENTRYPOINT ["java", \
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", \
  "--add-opens=java.base/java.io=ALL-UNNAMED", \
  "--add-opens=java.base/java.nio=ALL-UNNAMED", \
  "--add-opens=java.base/sun.misc=ALL-UNNAMED", \
  "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED", \
  "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang.ref=ALL-UNNAMED", \
  "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED", \
  "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED", \
  "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED", \
  "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED", \
  "-jar", "app.jar"]
