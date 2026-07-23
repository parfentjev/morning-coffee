.PHONY: run

run:
	mvn package -DskipTests && java -jar target/morning-coffee-1.0-SNAPSHOT.jar
