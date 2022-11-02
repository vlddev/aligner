Aligner
================
Parallel texts aligner

Features:

Installation
============


Installation (on Ubuntu Linux)
------------

### Build jar

1. Run `mvn clean install` 

### Exec.

java -Xmx4024M -Djdbc.driver=com.mysql.jdbc.Driver -Djdbc.url="jdbc:mysql://localhost:3306/aligner?useUnicode=true&characterEncoding=UTF-8" -Djdbc.user=<user> -Djdbc.password=<pwd> \
-jar vlad-aligner-1.0-SNAPSHOT-jar-with-dependencies.jar \
"../c/text_en.txt" en \
"../c/translation_uk.txt" uk


