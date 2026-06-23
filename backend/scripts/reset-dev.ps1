# Reset local dev DB and re-apply Flyway V1 (dev only — deletes all data)
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$backend = Split-Path $PSScriptRoot -Parent

& $mysql -u root -p12345 -e "DROP DATABASE IF EXISTS tutorconnectsystem; CREATE DATABASE tutorconnectsystem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mvn -f "$backend/pom.xml" flyway:migrate
