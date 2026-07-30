.PHONY: run lint build

run:
	mvn compile exec:java -Dexec.mainClass=ee.fakeplastictrees.morningcoffee.App

lint:
	mvn --batch-mode --no-transfer-progress -DskipTests verify

build:
	podman build -t morning-coffee:latest .
	rm -rf morning-coffee.tar
	podman save -o morning-coffee.tar morning-coffee:latest
