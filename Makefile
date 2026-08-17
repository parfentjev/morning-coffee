-include .env
export

.PHONY: run lint test

run:
	mvn compile exec:java -Dexec.mainClass=ee.fakeplastictrees.morningcoffee.App

lint:
	mvn --batch-mode --no-transfer-progress -DskipTests verify

test:
	mvn test
