# 构建阶段
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn -B -pl biz-service -am clean package -DskipTests

# 运行阶段：精简镜像，非 root 运行
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/biz-service/target/biz-service.jar app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
