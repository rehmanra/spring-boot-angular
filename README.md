# spring-boot-angular
<br>
Pre-Requisites
--------------
Docker w/ Command Line Tools<br>
Java 16<br>
Node 16.x<br>
Angular CLI via NPM<br>
<br>
Initialize Database
-------------------
docker-compose -f ./backend/docker/docker-compose.yml up<br>
<br>
Start Spring Boot
-----------------
cd ./backend<br>
./gradlew bootRun<br>
<br>
Run Frontend
------------
cd ./frontend<br>
npm install && npm run clean && npm run test && npm run watch && npm run start<br>
