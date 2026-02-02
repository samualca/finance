.PHONY: run build clean

MAIN=finance.Main

run: build
	java -cp target/classes $(MAIN)

build:
	./mvnw -q clean compile

clean:
	./mvnw -q clean
