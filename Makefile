.PHONY: build clean run

build:
	@./gradlew build

clean:
	@./gradlew clean

run:
	@./gradlew runClient
