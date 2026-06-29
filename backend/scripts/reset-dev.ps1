# Reset local dev DB and re-apply Flyway V1–V4 (dev only — deletes all data)
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$backend = Split-Path $PSScriptRoot -Parent

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Users\nguye\.jdks\openjdk-24.0.1"
}

& $mysql -u root -p12345 -e "DROP DATABASE IF EXISTS tutorconnectsystem; CREATE DATABASE tutorconnectsystem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mvn -f "$backend/pom.xml" flyway:migrate
