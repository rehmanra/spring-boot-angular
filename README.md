# spring-boot-angular
<br>
Pre-Requisites<br>
--------------<br>
Docker w/ Command Line Tools<br>
Java 17<br>
Node 18.19<br>
Angular CLI via NPM<br>
<br>
Initialize Database<br>
-------------------<br>
docker-compose -f ./backend/docker/docker-compose.yml up<br>
<br>
Start Spring Boot<br>
-----------------<br>
cd ./backend<br>
./gradlew bootRun<br>
<br>
Run Frontend<br>
------------<br>
cd ./frontend<br>
npm install && npm run clean && npm run test && npm run watch & npm run start<br>
