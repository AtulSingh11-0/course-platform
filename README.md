# Course Platform API

A backend API for a course learning platform that manages hierarchical course content and tracks user progress through educational material.

## Project Overview

This is a RESTful API built to support an online learning platform. It handles course management, user authentication, enrollment tracking, and progress monitoring at a granular level. The system organizes educational content in a three-level hierarchy (Course → Topic → Subtopic) and tracks completion at the subtopic level.

The platform is designed for learners who want to browse courses, enroll in them, and track their learning progress. It provides multiple search strategies to help users find relevant content quickly.

**Core problem solved:** Managing hierarchical course structures with detailed progress tracking while providing flexible search capabilities for content discovery.

## Key Features

### Public Features

- **Course Browsing**: View all available courses with summaries including topic and subtopic counts
- **Course Details**: Get full course structure with all topics and subtopics
- **Multi-Strategy Search**: Three different search approaches
  - PostgreSQL full-text search with fuzzy matching
  - Elasticsearch with advanced text analysis
  - Semantic search using machine learning embeddings

### Authentication Features

- **User Registration**: Email-based registration with password validation
- **JWT Authentication**: Stateless token-based authentication with 24-hour expiration
- **Password Security**: BCrypt hashing for secure password storage

### User Features

- **Course Enrollment**: Enroll in courses with duplicate enrollment prevention
- **Progress Tracking**: Mark individual subtopics as completed
- **Progress Reports**: View completion status and percentages for enrolled courses

### Documentation

- **Interactive Swagger UI**: Test all endpoints with built-in JWT authorization support
- **OpenAPI Specification**: Machine-readable API documentation

## Tech Stack

- **Framework**: Spring Boot 4.0.2, Java 21
- **Database**: PostgreSQL 15
- **Search Engines**:
  - PostgreSQL full-text search (built-in)
  - Elasticsearch 9.3.0
  - DJL (Deep Java Library) with ONNX Runtime for embeddings
- **Security**: Spring Security, JWT (jjwt 0.13.0), BCrypt password encoder
- **Documentation**: SpringDoc OpenAPI (springdoc-openapi-starter-webmvc-ui 3.0.1)
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose
- **Utilities**: Lombok, Jackson, HikariCP connection pooling

## High-Level Architecture

The project follows a clean layered architecture pattern:

**Controller Layer** (`com.courseplatform.controller`)

- REST endpoints with OpenAPI annotations
- Request validation and response formatting
- JWT authorization enforcement on protected endpoints

**Service Layer** (`com.courseplatform.service` + `impl`)

- Business logic and transaction management
- Interface-based design for flexibility
- Orchestrates between repositories and external services

**Repository Layer** (`com.courseplatform.repository`)

- JPA repositories extending `JpaRepository`
- Custom native SQL queries for complex searches
- Database access abstraction

**Security Layer** (`com.courseplatform.security`)

- JWT token generation and validation
- Authentication filter intercepting requests
- Custom user details service for email-based auth

**Search Layer** (`com.courseplatform.search`)

- Elasticsearch document mappings
- Search repositories and service implementations
- Embedding service for semantic search

**Exception Handling** (`com.courseplatform.exception`)

- Global exception handler with `@RestControllerAdvice`
- Standardized error responses across all endpoints

### Why This Structure

- **Clear separation of concerns**: Each layer has a single responsibility
- **Testability**: Layers can be tested independently with mocks
- **Maintainability**: Changes in one layer don't cascade to others
- **Spring Boot best practices**: Follows recommended patterns from Spring documentation
- **Flexibility**: Service interfaces allow swapping implementations without changing controllers

## Database Design

The schema uses a hierarchical structure for content organization and detailed tracking for user progress.

### Entity Model

**User**

- Stores user credentials and audit timestamps
- Email is unique and used as username for authentication
- Passwords are BCrypt hashed, never stored in plaintext

**Course → Topic → Subtopic (3-level hierarchy)**

- Course contains multiple Topics
- Topic contains multiple Subtopics
- Subtopic holds the actual learning content
- Each level has custom string IDs for readability

**Enrollment**

- Links users to courses they've enrolled in
- Unique constraint prevents duplicate enrollments
- Cascade relationship with progress tracking

**SubtopicProgress**

- Tracks completion status at the subtopic level
- Records timestamp when subtopic was completed
- Unique constraint per enrollment-subtopic pair

**Auditing** (via `Auditable` mapped superclass)

- Automatic `createdAt` and `updatedAt` timestamps
- Uses Spring Data JPA auditing with `@EnableJpaAuditing`

### Design Rationale

- **Hierarchical structure**: Matches how educational content is naturally organized
- **Subtopic-level tracking**: Provides granular progress insights without overwhelming complexity
- **Normalized schema**: Reduces redundancy and maintains data integrity
- **Unique constraints**: Prevent logical errors like duplicate enrollments
- **Auditable timestamps**: Essential for debugging and compliance

For the complete schema definition, see `src/main/resources/schema.sql`.

## Search with PostgreSQL, Elasticsearch, and Semantic Embeddings

This project implements three distinct search strategies, each with different strengths.

### PostgreSQL Full-Text Search

**Endpoint**: `GET /api/search?q={query}`

**How it works:**

- Uses PostgreSQL's `word_similarity()` function with trigram matching
- Native SQL query searches across all three levels (courses, topics, subtopics)
- Weighted scoring system:
  - Course titles: 10x weight
  - Topic/Subtopic titles: 5x weight
  - Subtopic content: 1x weight
- Falls back to `ILIKE` pattern matching for broader results
- Returns top 20 results ordered by relevance score

**When to use:**

- Default search for most use cases
- Fast keyword matching with typo tolerance
- No additional infrastructure required
- Good for exact matches and similar words

### Elasticsearch Search

**Endpoint**: `GET /api/search/es?q={query}`

**How it works:**

- Documents indexed with nested structure (Course → Topics → Subtopics)
- English text analyzer breaks down text for better matching
- Multi-level boolean query with SHOULD clauses:
  - Root query: Course title and description (boosted 3x and 2x)
  - Nested query: Topic titles (boosted 2x)
  - Doubly-nested query: Subtopic titles and content
- Fuzzy matching with AUTO fuzziness (1-2 edit distance)
- Relevance-based scoring

**When to use:**

- Need advanced text analysis features
- Scaling to large datasets (millions of documents)
- Want to implement faceted search or aggregations
- Complex query requirements

**Scaling potential:**

- Can distribute across multiple nodes
- Supports real-time indexing
- Built for horizontal scaling

### Semantic Search

**Endpoint**: `GET /api/search/semantic?q={query}`

**How it works:**

- Uses DJL (Deep Java Library) with ONNX Runtime
- SentenceTransformer model generates embeddings (768-dimensional vectors)
- Converts query and subtopic content into vector representations
- Calculates cosine similarity between query and content vectors
- Returns top 20 most semantically similar subtopics

**When to use:**

- Natural language queries (e.g., "how fast things move" finds "velocity")
- Conceptual matching beyond exact keywords
- Understanding meaning and context
- Queries where synonym matching isn't enough

**Example:** Searching for "rate of change" will find content about "velocity" and "acceleration" even if those exact words aren't in the query.

### When to Use Each

| Search Type | Best For | Trade-off |
|------------|----------|-----------|
| **PostgreSQL** | Default searches, exact keywords | Limited to word-level matching |
| **Elasticsearch** | Large-scale deployments, complex queries | Additional infrastructure |
| **Semantic** | Natural language, conceptual queries | Slower, requires ML model |

### Extending Search

Future improvements can include:

- Autocomplete/typeahead suggestions
- Faceted search (filter by topics, difficulty level)
- Search result caching with Redis
- Custom relevance tuning based on user behavior
- Multi-language support with language detection

## Authentication & Security

### JWT Authentication Flow

1. **Registration** (`POST /api/auth/register`)
   - User provides email and password
   - Password hashed with BCrypt (10 rounds)
   - User record created in database
   - Returns success message with user ID

2. **Login** (`POST /api/auth/login`)
   - User provides email and password
   - Credentials validated against database
   - Server generates JWT with 24-hour expiration
   - Returns JWT token and user email

3. **Accessing Protected Endpoints**
   - Client includes token in Authorization header: `Bearer <token>`
   - `JwtAuthenticationFilter` intercepts request
   - Filter extracts and validates token
   - If valid, request proceeds; if invalid, returns 401

4. **Token Validation**
   - Checks token signature using secret key
   - Verifies token hasn't expired
   - Extracts username from token subject

### Public vs Protected APIs

**Public Endpoints** (no authentication required):

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/courses` - List all courses
- `GET /api/courses/{courseId}` - Course details
- `GET /api/search` - PostgreSQL search
- `GET /api/search/semantic` - Semantic search
- `GET /api/search/es` - Elasticsearch search
- `/swagger-ui/**` - API documentation
- `/v3/api-docs/**` - OpenAPI spec

**Protected Endpoints** (JWT required):

- `POST /api/courses/{courseId}/enroll` - Enroll in course
- `POST /api/subtopics/{subtopicId}/complete` - Mark subtopic complete
- `GET /api/enrollments/{enrollmentId}/progress` - View progress report

### Using JWT with Swagger UI

1. Open Swagger UI: `http://localhost:8080/swagger-ui/index.html`
2. Register a new user using the Auth controller
3. Login to receive a JWT token
4. Click the "Authorize" button (top right, lock icon)
5. Enter: `Bearer <your-jwt-token>` (include the word "Bearer" and a space)
6. Click "Authorize" then "Close"
7. All requests will now include the JWT token automatically

### Security Considerations

- **Stateless authentication**: Server doesn't store session state
- **Token expiration**: Tokens automatically expire after 24 hours
- **HTTPS in production**: Always use HTTPS to encrypt token transmission
- **Secret key management**: JWT secret stored as environment variable
- **No token revocation**: Stateless design means tokens can't be revoked until expiration (acceptable for MVP)
- **Password requirements**: Minimum 6 characters (8+ recommended)

## API Documentation (Swagger)

### Accessing Swagger UI

**Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
**OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Features

- **Interactive Testing**: Execute API calls directly from the browser
- **JWT Authorization**: Built-in authorization dialog for testing protected endpoints
- **Request/Response Schemas**: See exact payload structure with types and validation rules
- **Example Values**: Pre-filled examples for quick testing
- **Controller Grouping**: Endpoints organized by functional area (Auth, Courses, Progress, Search)
- **Error Response Documentation**: See possible error responses for each endpoint

### Why Swagger is Essential

During development, Swagger served as:

- **Testing tool**: No need for standalone HTTP clients like Postman
- **Documentation**: API contract is always up-to-date with code
- **Frontend collaboration**: Frontend developers can see exact API structure
- **Validation**: Immediate feedback on request/response format

All OpenAPI annotations are maintained in the controller classes, making the documentation a living part of the codebase.

## Setup & Installation

### Prerequisites

- **JDK 21** - Required for Spring Boot 4.0.2
- **Maven 3.8+** - Build tool
- **Docker & Docker Compose** - For running PostgreSQL and Elasticsearch
- **Git** - Version control

### Environment Variables

The application expects these environment variables (defaults shown):

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/course_platform_dev
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=12345

# Elasticsearch Configuration
ELASTICSEARCH_URIS=http://localhost:9201
ELASTICSEARCH_USERNAME=
ELASTICSEARCH_PASSWORD=

# JWT Configuration
JWT_SECRET=<base64-encoded-secret-minimum-32-chars>
JWT_EXPIRATION_MS=86400000

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### Local Development Setup

**Step 1: Clone the repository**

```bash
git clone <repository-url>
cd course-platform
```

**Step 2: Start PostgreSQL and Elasticsearch**

```bash
docker-compose up -d postgres elasticsearch
```

This starts:

- PostgreSQL on port 5432
- Elasticsearch on port 9201 (mapped from container's 9200)

**Step 3: Verify services are running**

```bash
docker ps
```

You should see `course-postgres` and `course-elastic` containers running.

**Step 4: Run the Spring Boot application**

```bash
mvn spring-boot:run
```

The application will:

- Connect to PostgreSQL and validate schema
- Connect to Elasticsearch
- Load seed data from `src/main/resources/data/courses.json`
- Generate embeddings for semantic search (takes a minute on first run)
- Start on port 8080

**Step 5: Access Swagger UI**

```
http://localhost:8080/swagger-ui/index.html
```

**Step 6: Test the API**

1. Register a user via `POST /api/auth/register`
2. Login via `POST /api/auth/login` to get JWT token
3. Authorize in Swagger with the token
4. Test protected endpoints like enrollment and progress tracking

### Running with Docker (All Services)

To run the entire stack (app + PostgreSQL + Elasticsearch) in Docker:

```bash
docker-compose up --build
```

This is useful for testing the production-like deployment.

### Stopping Services

```bash
# Stop specific services
docker-compose stop postgres elasticsearch

# Stop and remove all containers
docker-compose down

# Stop and remove with volumes (deletes data)
docker-compose down -v
```

## Configuration

### Application Profiles

The application supports three profiles:

**dev** (`application-dev.yaml`)

- Uses Docker service names (`postgres:5432`, `elasticsearch:9200`)
- Optimized for running app locally with Docker dependencies
- Moderate logging level
- Schema validation enabled

**local** (`application-local.yaml`)

- Uses localhost URLs
- For running all services on host machine (not Docker)
- Useful for IDE debugging

**prod** (`application-prod.yaml`)

- Production-ready settings
- Environment-driven configuration
- Minimal logging
- Connection pool tuning

### Key Configuration Properties

**Server**

- Port: 8080 (configurable via `SERVER_PORT`)
- Response compression enabled for JSON/XML (min 1KB)

**Database**

- HikariCP connection pool: max 10, min idle 5
- JPA: `ddl-auto=validate` (expects schema.sql to run)
- SQL logging: disabled in dev, off in prod

**Elasticsearch**

- Socket timeout: 30 seconds
- Connection timeout: 5 seconds
- Optional basic auth support

**JWT**

- Secret: Base64-encoded key (minimum 32 characters)
- Expiration: 86400000ms (24 hours)
- HMAC-SHA signing algorithm

### Switching Profiles

**Via environment variable:**

```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

**Via Maven:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**In Docker Compose:**

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev
```

## Error Handling

All errors return a consistent JSON structure with appropriate HTTP status codes.

### Error Response Format

```json
{
  "error": "Error Type",
  "message": "Human-readable explanation",
  "timestamp": "2026-02-08T10:30:00Z"
}
```

### HTTP Status Codes

**400 Bad Request**

- Validation failures (invalid email format, password too short)
- Type mismatches (e.g., passing string where number expected)
- Malformed JSON in request body

**401 Unauthorized**

- Missing JWT token
- Expired or invalid JWT token
- Incorrect email/password during login
- User not found

**403 Forbidden**

- Access denied (not used in current implementation, but handled)

**404 Not Found**

- Course ID doesn't exist
- Subtopic ID doesn't exist
- Enrollment ID doesn't exist
- Invalid URL path

**405 Method Not Allowed**

- Using wrong HTTP method (e.g., POST instead of GET)

**500 Internal Server Error**

- Unexpected server errors
- Database connection failures
- Elasticsearch connection issues

### Validation Rules

**User Registration:**

- Email: Must match email format regex
- Password: Minimum 6 characters (8+ recommended in documentation)

**Search:**

- Query parameter: Optional (empty query returns empty results)
- No maximum length enforced

### Edge Cases Handled

- **Duplicate enrollments**: Database unique constraint prevents duplicates
- **Invalid IDs**: Returns 404 with clear message
- **Empty search queries**: Returns empty array instead of error
- **Expired JWT**: Returns 401 with explanation
- **Missing Authorization header**: Proceeds without authentication for public endpoints
- **Null/blank inputs**: Validation rejects with 400 status

### Implementation

Global exception handling is centralized in `GlobalExceptionHandler` using `@RestControllerAdvice`. All exceptions are caught, logged, and converted to standardized error responses.

## Testing

### Manual Testing with Swagger

Swagger UI provides a complete testing environment without requiring external tools.

**Basic Testing Flow:**

1. **Open Swagger UI**

   ```
   http://localhost:8080/swagger-ui/index.html
   ```

2. **Test Public Endpoints** (no auth required)
   - `GET /api/courses` - Verify course list returns
   - `GET /api/courses/{courseId}` - Check course details with topics/subtopics
   - `GET /api/search?q=physics` - Test PostgreSQL search
   - `GET /api/search/semantic?q=motion` - Test semantic search
   - `GET /api/search/es?q=calculus` - Test Elasticsearch

3. **Test Authentication**
   - `POST /api/auth/register` - Create a test account

     ```json
     {
       "email": "test@example.com",
       "password": "password123"
     }
     ```

   - `POST /api/auth/login` - Get JWT token
   - Copy the returned token value

4. **Authorize in Swagger**
   - Click "Authorize" button (lock icon, top right)
   - Enter: `Bearer <paste-your-token>`
   - Click "Authorize"

5. **Test Protected Endpoints**
   - `POST /api/courses/{courseId}/enroll` - Enroll in a course
   - `POST /api/subtopics/{subtopicId}/complete` - Mark subtopic complete
   - `GET /api/enrollments/{enrollmentId}/progress` - View progress report

6. **Test Error Scenarios**
   - Try enrolling in the same course twice (should fail)
   - Use an invalid course ID (should return 404)
   - Remove authorization and try protected endpoint (should return 401)
   - Use an expired token (should return 401)

### Testing Philosophy

This project prioritizes rapid manual testing during development:

- Swagger UI provides immediate feedback
- All endpoints have example payloads
- Error messages are clear and actionable
- Testing doesn't require external tools or scripts

### Future Testing Improvements

While Swagger testing is effective for development, a production system would benefit from:

- **Unit tests**: Test service layer business logic in isolation
- **Integration tests**: Test repository queries against test database
- **API tests**: Automated endpoint testing with test fixtures
- **Performance tests**: Load testing to identify bottlenecks
- **Security tests**: Penetration testing for vulnerabilities

## Design Decisions & Trade-offs

### What Was Prioritized

**Clean Architecture**

- Layered structure makes the codebase maintainable and understandable
- Each layer has clear responsibilities
- Easy to onboard new developers

**Search Flexibility**

- Three search implementations demonstrate different approaches
- Allows comparing performance characteristics
- Shows understanding of various search technologies

**Developer Experience**

- Comprehensive Swagger documentation reduces friction
- Clear error messages speed up debugging
- Seed data provides instant testing environment

**Security**

- JWT stateless authentication is industry-standard
- BCrypt hashing protects passwords
- Environment variables keep secrets out of code

**Progress Granularity**

- Subtopic-level tracking provides useful insights
- Not too coarse (course-level) or too fine (paragraph-level)
- Balances detail with simplicity

**Database Design**

- Normalized schema prevents data anomalies
- Unique constraints enforce business rules at DB level
- Auditing timestamps aid debugging

### What Was Kept Simple

**No Role-Based Access Control**

- Current implementation has only one user type (learners)
- Adding roles (admin, instructor) would complicate the MVP
- Can be added later when requirements are clear

**No Pagination**

- Course listings return all courses
- Acceptable for MVP with limited data
- Easy to add when needed (Spring Data supports it)

**No HTTPS Enforcement**

- Assumes reverse proxy (nginx) handles SSL termination
- Keeps application code focused on business logic
- Standard practice in containerized deployments

**No Rate Limiting**

- Trusts deployment environment (API gateway) to handle
- Adds complexity without clear benefit at MVP stage
- Can monitor and add if abuse becomes an issue

**No Caching**

- Database queries are fast enough for current scale
- Adding Redis would complicate local development
- Can be added strategically based on profiling data

### What Was Not Implemented

**Course Creation APIs**

- Focused on learner experience first
- Course management is a separate admin workflow
- Would require additional content validation logic

**User Profiles**

- Registration captures minimum required data
- Extended profiles (avatar, bio) are beyond MVP scope

**Course Reviews and Ratings**

- Social features add significant complexity
- Requires moderation and UI components
- Better as separate feature later

**Real-time Notifications**

- Would require WebSocket infrastructure
- Email notifications sufficient for MVP
- Consider when user engagement becomes priority

**Social Features**

- Comments, discussions, study groups
- Outside core learning platform goals
- Significant moderation requirements

**Multi-language Support**

- Content and API responses in English only
- Internationalization adds complexity to content storage
- Can be designed in later with proper planning

**File Uploads**

- Content stored as text in database
- Video, PDF support requires file storage and CDN
- Keeps MVP focused on core functionality

## Approach & Thought Process

When I first read through the assignment, my immediate goal was to strip away the complexity and focus on the core value: delivering course content and tracking user progress. It's easy to get lost in the weeds with a project like this—imagining complex admin panels or fancy UI features—but I knew I had to stick to the requirements to deliver something solid within the timeframe.

### How I Started

I didn't write a single line of code at first. Instead, I spent some time just sketching out the domain on paper. I needed to understand the relationships. I knew I had "Content" (Courses, Topics, Subtopics) which is basically static/read-only, and "User Data" (Users, Enrollments, Progress) which is dynamic.

Once I had that mental map, I broke the build process down into three distinct phases:

1. **The Skeleton:** Get a Spring Boot app running with a Postgres database and a basic schema.
2. **The Core Logic:** Implement the read-only course browsing and the write-heavy progress tracking.
3. **The "Wow" Factor:** Integrate Elasticsearch for the search functionality, because doing simple SQL `LIKE` queries felt like a cop-out for a backend assignment.

This approach kept me from getting overwhelmed. I could celebrate small wins—like just getting the seed data to load correctly—before worrying about how to handle complex search queries.

### Data Modeling

I tried to keep the database schema as normalized as possible without over-engineering it.

**Course Structure**

- I used a standard hierarchical model: `Course` -> `Topic` -> `Subtopic`. This felt natural. A course has many topics, a topic has many subtopics. I made sure to use proper foreign keys so the database handles the integrity for me.

**Users & Enrollments**

- The `User` table is pretty standard. The `Enrollment` table is the bridge between a User and a Course. I decided to make the `(user_id, course_id)` pair unique. This was a crucial database-level constraint to prevent the "double enrollment" bug before it even hits the application logic.

**Progress**

- This was the interesting part. I created a `SubtopicProgress` table that links an Enrollment to a specific Subtopic. I tracked a `completed_at` timestamp. This allows us to calculate the "percentage complete" dynamically by just counting how many subtopics a user has finished versus the total count in the course.

### The Search Implementation

Search is often the feature that makes or breaks a backend experience. I could have just used Postgres full-text search, and honestly, for a small dataset, that would have been fine. But I wanted to demonstrate a more scalable approach.

I chose to spin up an **Elasticsearch** container alongside the app. The logic is simple: when the application starts and seeds the database, it also indexes that content into Elasticsearch. This means we aren't hitting the primary database for heavy text searching. We query Elasticsearch, which is optimized for things like fuzzy matching (handling typos) and ranking relevance. It adds a bit of infrastructure complexity (hello, Docker memory limits!), but the result is a search experience that feels instant and forgiving.

### Authentication & APIs

For authentication, I stuck with **JWT (JSON Web Tokens)**. It's the industry standard for a reason. Since this is a REST API, I didn't want to mess around with server-side sessions. A stateless token that the client sends with every request is clean and scalable.

I clearly separated the API into two zones:

1. **Public Endpoints:** Things like viewing the course catalog, reading content, and searching. I wanted these to be frictionless. You shouldn't need to log in just to see what the platform offers.
2. **Protected Endpoints:** Enrollment and progress tracking. These are behind the `Authorization` guard.

I also spent time implementing a custom `UserDetailsService` to bridge my Postgres `users` table with Spring Security. It was a bit of boilerplate, but it gives me full control over how users are authenticated.

### Swagger & Testing

I am a huge believer that if you can't test it easily, you won't use it. Since there's no frontend provided for this assignment, **Swagger UI** became my frontend.

I configured OpenAPI so that you can actually log in right there in the browser. You hit the `/login` endpoint, copy the token, paste it into the "Authorize" button, and suddenly you can test all the protected endpoints like `enroll` or `complete`. This wasn't just for the reviewer's benefit; it saved me tons of time during development. I didn't have to keep switching to Postman or writing `curl` commands. If it worked in Swagger, I knew it was working.

### Handling Edge Cases

Backend development is mostly about handling the things that shouldn't happen but eventually will.

**Idempotency**

- I made sure that the "mark as complete" endpoint is idempotent. If a user clicks the "Complete" button ten times, it doesn't crash or create ten duplicate records. It just says "Okay, marked as complete" and moves on.

**Duplicate Enrollment**

- If you try to enroll in a course you are already taking, I don't just throw a generic 500 error. I catch that specific conflict and return a clear 409 status code with a message saying, "You are already enrolled."

**Missing Data**

- If you try to access a course ID that doesn't exist, I throw a proper 404 exception.

These small details matter. They turn a fragile API into a robust one.

### Scope & Trade-offs

There were a million things I wanted to build but didn't, simply because of time and scope constraints.

**No CMS**

- I didn't build APIs to create or edit courses. The requirements said content is read-only, so I respected that. Building a CMS is a whole other project.

**No Complex Roles**

- I didn't implement Admin or Teacher roles. Everyone is just a "Student." Adding RBAC (Role-Based Access Control) adds significant complexity to the security config, and it wasn't requested.

**Infrastructure**

- I stuck to a monolithic deployment (App + DB + Search on one VPS). In a real startup scenario with 100k users, I'd split these onto managed services (AWS RDS, Elastic Cloud), but for this assignment, a `docker-compose` setup was the most pragmatic choice.

### Overall Mindset

My guiding principle for this code was **readability**. I tried to write code that explains itself. Variable names are descriptive, methods do one thing, and the architecture follows standard Spring Boot patterns (Controller -> Service -> Repository).

I wanted to build something that another developer could clone, run `docker-compose up`, and understand within 15 minutes. If I achieved that, I consider the assignment a success.

## Project Structure

Quick reference to key directories and files:

```text
course-platform/
├── src/main/java/com/courseplatform/
│   ├── config/                          # Configuration classes
│   │   ├── AppConfig.java              # Application-wide beans
│   │   ├── SecurityConfig.java         # Spring Security, JWT filter chain
│   │   ├── ElasticsearchConfig.java    # Elasticsearch client setup
│   │   ├── OpenApiConfig.java          # Swagger/OpenAPI configuration
│   │   └── DataSeeder.java             # Loads seed data on startup
│   │
│   ├── controller/                      # REST endpoints
│   │   ├── AuthController.java         # Registration, login
│   │   ├── CourseController.java       # Course browsing, enrollment
│   │   ├── ProgressController.java     # Progress tracking
│   │   └── SearchController.java       # All search endpoints
│   │
│   ├── dto/                            # Data Transfer Objects
│   │   ├── request/                    # API request payloads
│   │   └── response/                   # API response payloads
│   │
│   ├── exception/                       # Error handling
│   │   └── GlobalExceptionHandler.java # Centralized exception handling
│   │
│   ├── model/                           # JPA entities
│   │   ├── User.java
│   │   ├── Course.java
│   │   ├── Topic.java
│   │   ├── Subtopic.java
│   │   ├── Enrollment.java
│   │   ├── SubtopicProgress.java
│   │   └── Auditable.java              # Base class for timestamps
│   │
│   ├── repository/                      # Data access layer
│   │   ├── CourseRepository.java       # Includes custom search query
│   │   ├── UserRepository.java
│   │   ├── EnrollmentRepository.java
│   │   ├── SubtopicRepository.java
│   │   └── SubtopicProgressRepository.java
│   │
│   ├── search/                          # Elasticsearch components
│   │   ├── document/
│   │   │   └── CourseDocument.java     # Elasticsearch index mapping
│   │   ├── repository/
│   │   │   └── CourseSearchRepository.java
│   │   └── service/
│   │       ├── SearchService.java      # Interface
│   │       └── impl/
│   │           └── SearchServiceImpl.java
│   │
│   ├── security/                        # Authentication & authorization
│   │   ├── JwtUtil.java                # Token generation/validation
│   │   ├── CustomUserDetailsService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtAuthenticationEntryPoint.java
│   │
│   └── service/                         # Business logic
│       ├── AuthService.java            # Interfaces
│       ├── CourseService.java
│       ├── EnrollmentService.java
│       ├── ProgressService.java
│       ├── EmbeddingService.java
│       └── impl/                        # Implementations
│           ├── AuthServiceImpl.java
│           ├── CourseServiceImpl.java
│           ├── EnrollmentServiceImpl.java
│           ├── ProgressServiceImpl.java
│           └── EmbeddingServiceImpl.java
│
├── src/main/resources/
│   ├── application.yaml                 # Main configuration
│   ├── application-dev.yaml            # Development profile
│   ├── application-local.yaml          # Local development profile
│   ├── application-prod.yaml           # Production profile
│   ├── schema.sql                      # Database schema
│   └── data/
│       └── courses.json                # Seed data
│
├── docker-compose.yml                   # Local development services
├── docker-compose.prod.yml              # Production deployment config
├── Dockerfile                          # App containerization
├── pom.xml                             # Maven dependencies
└── README.md                           # This file
```

---

Thanks for reading this far! It was a fun project to build, and I hope this documentation gives you a clear picture of the design decisions and implementation details. I really loved working on this.
