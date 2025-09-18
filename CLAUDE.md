# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BUZZLE is a Spring Boot quiz application that provides both single-player quiz functionality and real-time multiplayer quiz battles. The backend uses Java 17, Spring Boot 3.3.5, and integrates with OpenAI GPT for AI-generated quizzes.

## Development Commands

### Building and Running
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

### Database Operations
- The application uses MySQL with JPA/Hibernate
- DDL mode is set to `create` (recreates schema on startup)
- Database configuration is externalized via environment variables

## Architecture

### Package Structure
The codebase follows a domain-driven design pattern organized as:
- `shop.buzzle.buzzle.{domain}.api` - REST controllers and DTOs
- `shop.buzzle.buzzle.{domain}.application` - Business logic services
- `shop.buzzle.buzzle.{domain}.domain` - Domain entities and repositories
- `shop.buzzle.buzzle.global` - Cross-cutting concerns (config, JWT, OAuth, etc.)

### Key Domains
- **auth** - Authentication and authorization (JWT, OAuth2)
- **member** - User management and profiles
- **quiz** - Single-player quiz functionality
- **game** - General game logic
- **multi** - Multiplayer matching system (SSE-based)
- **multiroom** - Real-time multiplayer quiz rooms (WebSocket)
- **websocket** - WebSocket infrastructure for real-time features
- **notification** - User notification system
- **ai** - OpenAI integration for quiz generation

### Technology Stack
- **Framework**: Spring Boot 3.3.5 with Java 17
- **Database**: MySQL with Spring Data JPA and QueryDSL
- **Authentication**: JWT tokens with OAuth2 (Kakao)
- **Real-time**: WebSocket (Spring WebSocket) and Server-Sent Events (SSE)
- **AI Integration**: OpenAI GPT API for quiz generation
- **API Documentation**: Springdoc OpenAPI (Swagger)
- **Build**: Gradle with Lombok for boilerplate reduction

### Real-time Features
- **Multiplayer Matching**: Uses Server-Sent Events (SSE) for real-time match finding
- **Quiz Battles**: WebSocket-based real-time quiz sessions between players
- **Notifications**: Event-driven notification system

### Configuration
- Application uses Spring Profiles (active profile: `prod`)
- Database credentials and OpenAI API keys are externalized
- Swagger UI available for API documentation
- JPA shows SQL queries in development (show_sql: true)

### Authentication Flow
- OAuth2 integration with Kakao for social login
- JWT-based authentication with refresh token mechanism
- Custom authentication filters and resolvers for user context

### Development Notes
- Uses Lombok for reducing boilerplate code
- QueryDSL for type-safe database queries  
- Custom exception handling with global error responses
- JPA Auditing enabled for entity timestamps
- WebSocket configuration supports both STOMP and raw WebSocket