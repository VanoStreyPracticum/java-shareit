#!/bin/bash

# Собираем JAR файл
echo "Building JAR file..."
mvn clean package -DskipTests

# Проверяем, что JAR создался
if [ ! -f "target/shareit-0.0.1-SNAPSHOT.jar" ]; then
    echo "ERROR: JAR file not created!"
    ls -la target/
    exit 1
fi

echo "JAR file created successfully:"
ls -lh target/shareit-0.0.1-SNAPSHOT.jar

# Запускаем сервер
echo "Starting server..."
java -jar target/shareit-0.0.1-SNAPSHOT.jar &
SERVER_PID=$!

echo "Server PID: $SERVER_PID"

# Ждем запуска
echo "Waiting for server to start..."
sleep 30

# Проверяем запуск
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "Server started successfully!"
else
    echo "ERROR: Server failed to start"
    kill $SERVER_PID 2>/dev/null || true
    exit 1
fi

# Запускаем тесты Newman
echo "Running Newman tests..."
cd tests
newman run ./postman/sprint.json \
  --delay-request 50 \
  -r cli,htmlextra \
  --verbose \
  --color on \
  --reporter-htmlextra-darkTheme \
  --reporter-htmlextra-export reports/shareIt.html \
  --reporter-htmlextra-title "Отчет по тестам" \
  --reporter-htmlextra-logs true \
  --reporter-htmlextra-template ./.github/workflows/dashboard-template.hbs

EXIT_CODE=$?

# Останавливаем сервер
echo "Stopping server..."
kill $SERVER_PID 2>/dev/null || true

exit $EXIT_CODE