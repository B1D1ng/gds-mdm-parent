# Development Workflow

## GDS-MDM-Parent Development Guide

This document outlines the development workflow for the GDS-MDM-Parent project, including build, test, and deployment procedures.

## Prerequisites

To work with this project, you'll need:

- Java JDK (version 17)
- Maven (for build and dependency management)
- Git (for version control)
- IDE with Java support (IntelliJ IDEA, VS Code with Java extensions)
- Database (MySql for production, H2 for testing)

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone [repository-url]
   cd gds-mdm-parent
   ```

2. **Build the project**:
   ```bash
   ./mvnw clean install
   ```
   
   Or using the standard Maven command:
   ```bash
   mvn clean install
   ```

## Project Structure

The project follows a multi-module Maven structure:

```
gds-mdm-parent/
├── common/                 # Common utilities
├── common-svc/             # Common service components
├── contract/               # API contracts
├── dec/                    # Data Enrichment and Consumption (DEC) component
├── hub/                    # Central hub component
├── signal/                 # Signal processing
├── signal-common/          # Common signal utilities
├── udf/                    # User-defined functions
└── udf-common/             # Common UDF utilities
```

## Build Process

### Building Individual Modules

To build a specific module:

```bash
./mvnw clean install -pl module-name
```

For example, to build only the signal module:

```bash
./mvnw clean install -pl touchpoint-signal
```

### Skipping Tests

To build without running tests:

```bash
./mvnw clean install -DskipTests
```

## Testing

### Testing Best Practices

The project follows these testing best practices:

1. **Test Naming Convention**: `method_scenario_result`
   - Example: `findById_userExists_returnsUser`
   - Format: `[method being tested]_[scenario/condition]_[expected result]`

2. **Test Structure**: Each test follows the Given/When/Then pattern with clear separation:
   ```java
   @Test
   void findById_userExists_returnsUser() {
       // Given
       var userId = 1L;
       var expectedUser = new User(userId, "John");
       when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
       
       // When
       var result = userService.findById(userId);
       
       // Then
       assertThat(result).isNotNull();
       assertThat(result.getId()).isEqualTo(userId);
       assertThat(result.getName()).isEqualTo("John");
   }
   ```

3. **Assertion Framework**: AssertJ is used exclusively with JUnit 5
   - Use fluent assertions from AssertJ (`assertThat()`)
   - Do not use JUnit assertions (`assertEquals()`, etc.)

4. **No Comments in Tests**: Tests should be self-documenting
   - Visual test parts: Use Given/When/Then structure with blank lines separator
   - Use descriptive variable and method names instead of explanatory comments
   - Use lombok var or val

### Coding Best Practices

The project follows these coding best practices that must be applied to all new code and updates:

1. **Naming Convention**: Use short, standard, boring names. Prefer shorter one-noun versions (e.g., `lookup` instead of `businessLookup`).

2. **Service DB Methods Naming**:
   - Use `getSomething()` when the method should throw `DataNotFoundException` if the entity is not found
   - Use `findSomething()` when the method should return an `Optional` if the entity might not exist

3. **Use Lombok**: Prefer `val` where possible (or Java's `var`) to reduce boilerplate and improve readability.

4. **Utility Classes**: Use `StringUtils`, `CollectionUtils`, etc. to simplify null checks and common operations.

5. **Control Flow**: Do not use `return` statements after `else` blocks.

6. **Collection Processing**: Use Java streams to manipulate collections instead of traditional loops.

7. **Error Handling**: Check negative flows first to avoid excessive nesting.

8. **Code Comments**: Avoid redundant code comments. Make code self-explanatory through clear naming and structure.

9. **Collections**: Use `Set` instead of `List` when order is not important.

10. **Validation**: Use Hibernate validation annotations on public service methods.

11. **DRY Principle**: Don't Repeat Yourself. Extract duplicate code into reusable methods or classes.

12. **Single Responsibility**: Each class and method should have only one reason to change. Keep methods focused on a single task.

13. **Immutability**: Prefer immutable objects and final fields (val) where possible to improve thread safety and reduce bugs.

14. **Specific Exceptions**: Use or create specific exception types rather than generic ones to provide clear error context.

15. **Method Length**: Keep methods short (preferably under 30 lines) and focused on a single responsibility.

16. **Constants**: Use constants for magic numbers and strings. Define them at the class or interface level.

17. **Logging**: Use appropriate log levels (DEBUG, INFO, WARN, ERROR) consistently. Include relevant context in log messages.

### Running Tests

To run all tests:

```bash
./mvnw test
```

To run tests for a specific module:

```bash
./mvnw test -pl module-name
```

### Integration Tests

Integration tests likely use the application-IT.yaml configuration:

```bash
./mvnw verify
```

## Running the Application

Each module with a Spring Boot application can be run individually:

```bash
./mvnw spring-boot:run -pl module-name
```

### Environment Profiles

The application supports multiple environment profiles:

- Development: `-Dspring.profiles.active=Dev`
- Feature: `-Dspring.profiles.active=Feature`
- Staging: `-Dspring.profiles.active=Staging`
- Pre-Production: `-Dspring.profiles.active=Pre-Production`
- Production: `-Dspring.profiles.active=Production`

Example:

```bash
./mvnw spring-boot:run -pl touchpoint-signal -Dspring.profiles.active=Dev
```

## Code Quality

The project uses several code quality tools:

### Checkstyle

To run Checkstyle:

```bash
./mvnw checkstyle:check
```

### PMD

To run PMD:

```bash
./mvnw pmd:check
```

## Deployment

Based on the project structure, deployment likely involves:

1. Building the application:
   ```bash
   ./mvnw clean package
   ```

2. Deploying to the target environment with the appropriate profile.

## Additional Notes

This development workflow is based on the observed file structure and common practices for Maven-based Java projects. The actual build, test, and deployment procedures may vary based on the project's specific requirements and infrastructure. For more detailed information about the project structure and architecture, refer to the other documentation files in this directory.
