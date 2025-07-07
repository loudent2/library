# Makefile for Loudent Library Service

# Build and test
build:
	./gradlew clean build

test:
	./gradlew test

# OpenAPI code generation
openapi:
	./gradlew generateLibraryApi

clean-openapi:
	./gradlew cleanOpenApiGenerated

# Docker image
image:
	./gradlew jibDockerBuild

# Docker Compose: Local DynamoDB only
dynamodb-up:
	docker compose up

# Docker Compose: Library + DynamoDB (bridge network)
compose-up:
	docker compose --profile library up

compose-down:
	docker compose down

docs:
	./gradlew generateLibraryDocs	
