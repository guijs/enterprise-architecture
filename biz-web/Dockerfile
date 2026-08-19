# 构建阶段
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn -B -pl biz-web -am clean package -DskipTests

# 运行阶段：精简镜像，非 root 运行
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/biz-web/target/biz-web.jar app.jar
# SkyWalking Agent（见 6.14）可在此 COPY 进镜像或用 sidecar/initContainer 注入
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
