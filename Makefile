.PHONY: build clean data run

build:
	@./gradlew build

clean:
	@./gradlew clean

data:
	@./gradlew runData

run:
	@./gradlew runClient
