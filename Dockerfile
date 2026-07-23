FROM node:current-alpine AS build-frontend
LABEL maintainer Ascensio System SIA <support@onlyoffice.com>
ARG BACKEND_URL
ENV BACKEND_URL=$BACKEND_URL
ARG EDITOR_ACTION_ID
ENV EDITOR_ACTION_ID=$EDITOR_ACTION_ID
WORKDIR /usr/src/app
COPY ./frontend/package*.json ./
COPY ./frontend/onlyoffice-docspace-react*.tgz ./
RUN npm install
COPY frontend .
RUN npm run build

FROM eclipse-temurin:21-jdk-jammy AS backend
WORKDIR /app
COPY ./backend/.mvn/ .mvn
COPY ./backend/mvnw ./backend/pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:resolve
COPY ./backend/src ./src
CMD ["./mvnw", "spring-boot:run"]

FROM nginx:alpine AS frontend
COPY --from=build-frontend \
    /usr/src/app/build \
    /usr/share/nginx/html
COPY --from=build-frontend \
    /usr/src/app/nginx/nginx.conf \
    /etc/nginx/conf.d/default.conf
EXPOSE 80
