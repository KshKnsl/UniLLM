# Frontend build stage
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
RUN npm install -g pnpm@latest
COPY frontend/package*.json ./
COPY frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/src/lib ./src/lib
COPY frontend/src/components ./src/components
COPY frontend/src/*.jsx ./src/
COPY frontend/src/*.js ./src/
COPY frontend/src/*.css ./src/
COPY frontend/*.config.js ./
COPY frontend/index.html ./
COPY frontend/components.json ./
RUN pnpm run build

# Backend build stage
FROM maven:3.9-eclipse-temurin-11-alpine AS backend-builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package dependency:copy-dependencies

# Runtime stage
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/classes ./classes
COPY --from=backend-builder /app/target/dependency ./lib
COPY --from=frontend-builder /app/frontend/dist ./frontend/dist

EXPOSE 8080
ENV PORT=8080
CMD ["java", "-cp", "classes:lib/*", "unillm.api.UniLLMApiServer"]
