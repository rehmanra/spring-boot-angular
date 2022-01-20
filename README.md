# spring-boot-angular

Pre-Requisites
--------------
Docker w/ Command Line Tools
Java 16
Node 16.x
Angular CLI via NPM

Initialize Database
-------------------
docker-compose -f ./backend/docker/docker-compose.yml up

Start Spring Boot
-----------------
cd ./backend
./gradlew bootRun

Run Frontend
------------
cd ./frontend
npm install && npm run clean && npm run test && npm run watch && npm run start 
