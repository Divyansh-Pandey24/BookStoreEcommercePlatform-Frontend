# API Gateway Service

This service acts as the central entry point for the BookNest microservices ecosystem.

## Responsibilities
- **Request Routing**: Routes incoming requests to the appropriate backend microservices.
- **Security**: Validates JWT tokens and handles cross-cutting security concerns.
- **Filtering**: Implements global filters for logging and request/response manipulation.
- **Documentation**: Aggregates Swagger/OpenAPI documentation for all services.
