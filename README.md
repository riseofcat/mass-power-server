mass-power-server  
  
begins from: https://github.com/kenichi-odo/spark-kotlin-gradle-heroku  
  
./gradlew stage  
heroku local  
and look at localhost:5000    
  
  
kotlin gradle stamples: https://github.com/gradle/kotlin-dsl/tree/master/samples  
  
  
./gradlew clean heroku-jvm:shadowJar  #создаст файл heroku-jvm/build/libs/heroku-jvm-1.0-all.jar
java -jar heroku-jvm/build/libs/heroku-jvm-1.0-all.jar 
