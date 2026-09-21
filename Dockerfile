# ---------- Etapa 1: build ----------
# Usa o proprio Maven wrapper do projeto, entao nao depende de imagem "maven:*"
# existir para o JDK 25.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Normaliza CRLF (o mvnw foi commitado no Windows) e garante permissao de execucao.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Camada de cache das dependencias: se o pom.xml nao mudar, o Docker reaproveita.
RUN ./mvnw -B -ntp dependency:go-offline -DskipTests || true

COPY src ./src
RUN ./mvnw -B -ntp clean package -DskipTests \
    && mv target/*.jar /app/app.jar

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/app.jar ./app.jar

# Ajuste para container pequeno (512 MB no plano free do Render).
# MaxRAMPercentage evita que a JVM assuma a RAM da maquina hospedeira inteira.
# SerialGC gasta bem menos memoria que o G1 em heap pequeno.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -Djava.security.egd=file:/dev/./urandom"

# Informativo: a porta real vem da variavel PORT injetada pela plataforma.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
