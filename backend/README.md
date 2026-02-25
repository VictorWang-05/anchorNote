# AnchorNotes Backend

Spring Boot REST API for the AnchorNotes Android application - a location and time-based reminder note-taking system with Supabase integration.

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **PostgreSQL** (via Supabase)
- **Spring Data JPA**
- **Spring Security** with Supabase JWT authentication
- **Maven** for dependency management
- **Lombok** for reducing boilerplate code

## Architecture

```
backend/anchornotes/
├── src/main/java/com/csci310/anchornotes/
│   ├── config/          # Configuration classes (Security, CORS, Supabase)
│   ├── controller/      # REST API Controllers (29 endpoints)
│   ├── dto/             # Data Transfer Objects (22 request/response models)
│   ├── entity/          # JPA Entities (6 database models)
│   ├── exception/       # Custom exceptions and global exception handler
│   ├── repository/      # Spring Data JPA repositories (5 repositories)
│   ├── security/        # JWT verification and authentication
│   ├── service/         # Business logic layer (6 services)
│   └── util/            # Utility classes (mappers, context utils)
└── src/main/resources/
    ├── application.properties
    └── db/migration/    # Database migration scripts
```

## Key Features

### Notes Management
- **CRUD operations** for notes with title, text, and metadata
- **Multi-tag support** via many-to-many relationships
- **Pinning** notes for quick access
- **Photo and audio attachments** via Supabase Storage
- **Advanced search** with multiple criteria (tags, time, attachments, location)

### Dual Reminder System (NOT Mutually Exclusive)
- **Time-based reminders**: Notes can have a specific date/time reminder in UTC
- **Geofence reminders**: Notes can trigger based on location
- **BOTH types can coexist** on a single note simultaneously
- **Relevant Notes API**: Returns notes within time window (±1h) OR inside geofences

### Templates
- Create reusable note templates with pre-filled content, tags, and geofences
- Instantiate templates into new notes

### User Isolation
- All operations are scoped to authenticated users via Supabase JWT
- Row-level data isolation using `user_id` foreign keys

---

## Setup Instructions

### 1. Prerequisites

- **Java 21** or higher ([Download OpenJDK](https://adoptium.net/))
- **Maven 3.8+** ([Download Maven](https://maven.apache.org/download.cgi))
- **Supabase Account** ([Sign up](https://supabase.com))
- **Git**

#### Install Java 21
```bash
# macOS (using Homebrew)
brew install openjdk@21

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-21-jdk

# Windows
# Download from https://adoptium.net/
```

Verify installation:
```bash
java -version  # Should show version 21
```

#### Install Maven
```bash
# macOS (using Homebrew)
brew install maven

# Ubuntu/Debian
sudo apt-get install maven

# Windows
# Download from https://maven.apache.org/download.cgi
```

Verify installation:
```bash
mvn -version  # Should show Maven 3.8+
```

### 2. Clone the Repository

```bash
git clone <repository-url>
cd anchornotes_team3/backend/anchornotes
```

### 3. Set Up Supabase

#### Create a Supabase Project

1. Go to [https://supabase.com/dashboard](https://supabase.com/dashboard)
2. Click "New Project"
3. Choose organization and create project
4. Wait for database provisioning

#### Get API Keys

1. Go to Project Settings → API
2. Copy:
   - **Project URL** (looks like: `https://xyz.supabase.co`)
   - **anon public** key
   - **service_role** key (keep this secret!)

#### Get Database Credentials

1. Go to Project Settings → Database
2. Connection String format:
   ```
   postgresql://postgres:[YOUR-PASSWORD]@db.xyz.supabase.co:5432/postgres
   ```
3. Convert to JDBC format:
   ```
   jdbc:postgresql://db.xyz.supabase.co:5432/postgres?sslmode=require
   ```

#### Disable Email Confirmation (For Development)

1. Go to Authentication → Providers → Email
2. Turn OFF "Confirm email"
3. Click Save
4. *Note: This allows instant login without email verification*

### 4. Database Setup

**IMPORTANT**: You must run the schema cleanup script before starting the backend.

**Step 1**: Go to Supabase Dashboard > SQL Editor

**Step 2**: Open and run the cleanup script:
```
database/CLEANUP_SCHEMA.sql
```

**Step 3**: Copy and paste the entire script into the Supabase SQL Editor

**Step 4**: Click "Run" to execute

This will:
- Add missing `user_id` column to `audio_attachment`
- Remove redundant `user` columns from `notes` and `templates`
- Remove old single-tag columns (migrated to junction tables)
- Add `NOT NULL` constraints to `user_id` columns
- Create performance indexes
- Migrate existing data safely

### 5. Configure Environment Variables

The `.env` file at the root contains all necessary environment variables:

```bash
# Located at: anchornotes_team3/.env
```

Update with your Supabase credentials:

```env
# Supabase Configuration
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_anon_key_here
SUPABASE_SERVICE_ROLE=your_service_role_key_here

# Database Configuration
SUPABASE_DB_URL=jdbc:postgresql://db.your-project.supabase.co:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=your_password_here

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

**Important:**
- Never commit the `.env` file (it's in `.gitignore`)
- Replace placeholder values with your actual Supabase credentials
- Keep `service_role` key secure - it has admin privileges

### 6. Build and Run

#### Using Maven Wrapper (Recommended)

```bash
# Navigate to the project directory
cd backend/anchornotes

# Make wrapper executable (Unix/macOS only)
chmod +x mvnw

# Clean and build
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

#### Using System Maven

```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 7. Verify Installation

Test the health endpoint:
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "anchornotes-api"
  }
}
```

---

## API Documentation

### Authentication

All endpoints (except `/api/auth/*` and `/api/health`) require JWT authentication via Supabase.

**Headers:**
```
Authorization: Bearer <supabase_jwt_token>
Content-Type: application/json
```

### How Authentication Works

1. **User registers** → Supabase creates user in auth.users table
2. **Backend returns** → Supabase JWT token
3. **Client stores token** → Use for authenticated requests
4. **Client sends requests** → Include `Authorization: Bearer <token>` header
5. **Backend verifies** → Token signature using Supabase service_role key

### Public Endpoints

#### Health Check
```
GET /api/health
```

#### Register New User
```
POST /api/auth/register

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe"
  }
}
```

#### Login
```
POST /api/auth/login

{
  "username": "john@example.com",
  "password": "password123"
}
```

**Note:** Use **email** as username for login.

### Protected Endpoints

#### Notes
```
POST   /api/notes                     - Create note
GET    /api/notes/{id}                - Get note by ID
PUT    /api/notes/{id}                - Update note
DELETE /api/notes/{id}                - Delete note
POST   /api/notes/{id}/pin            - Pin/unpin note
PUT    /api/notes/{id}/tags           - Set tags on note
GET    /api/notes/search              - Search notes with filters
POST   /api/relevant-notes            - Get relevant notes (time + geofence)
```

#### Reminders
```
PUT    /api/notes/{id}/reminder/time      - Set time reminder
PUT    /api/notes/{id}/reminder/geofence  - Set geofence reminder
DELETE /api/notes/{id}/reminder           - Clear all reminders
```

**IMPORTANT**: Notes can have BOTH time and geofence reminders simultaneously!

#### Tags
```
GET    /api/tags           - List all tags for user
POST   /api/tags           - Create tag
DELETE /api/tags/{id}      - Delete tag
```

**Create Tag Request:**
```json
{
  "name": "work",
  "color": "#8B5CF6"
}
```

#### Attachments (Pre-signed URL Flow)
```
POST   /api/notes/{id}/photo                     - Initiate photo upload
POST   /api/notes/{id}/photo/{aid}/complete      - Complete photo upload
DELETE /api/notes/{id}/photo/{aid}               - Delete photo
POST   /api/notes/{id}/audio                     - Initiate audio upload
POST   /api/notes/{id}/audio/{aid}/complete      - Complete audio upload
DELETE /api/notes/{id}/audio/{aid}               - Delete audio
```

**Upload Flow:**
1. Call initiate endpoint → Get pre-signed URL
2. Upload file directly to Supabase Storage using URL
3. Call complete endpoint → Link attachment to note

#### Geofences
```
GET    /api/geofences      - List geofences for Android registration
```

Returns all notes with geofences in format for Android GeofencingClient.

#### Templates
```
GET    /api/templates             - List all templates
POST   /api/templates             - Create template
PUT    /api/templates/{id}        - Update template
DELETE /api/templates/{id}        - Delete template
POST   /api/templates/{id}/instantiate - Create note from template
```

---

## Database Schema

### Core Tables

**notes**
- `id`, `user_id`, `title`, `text`, `pinned`, `created_at`, `last_edited`
- `reminder_time` (UTC timestamp)
- `geofence` (FK to geofence table)
- `image_file`, `audio_file` (FKs to photo_attachment/audio_attachment)

**tags**
- `id`, `user_id`, `name`, `color`, `created_at`

**note_tags** (many-to-many junction)
- `note_id`, `tag_id`, `created_at`

**geofence**
- `id`, `user_id`, `latitude`, `longitude`, `radius`, `created_at`

**photo_attachment**
- `id`, `user_id`, `media_url`, `media_type`, `duration_sec`, `created_at`

**audio_attachment**
- `id`, `user_id`, `media_url`, `media_type`, `created_at`

**templates**
- Same structure as notes: `id`, `user_id`, `name`, `text`, `pinned`, `geofence`, attachments

**template_tags** (many-to-many junction)
- `template_id`, `tag_id`, `created_at`

---

## Development Workflow

### Running Tests
```bash
./mvnw test
```

### Enable SQL Logging
In `application.properties`:
```properties
spring.jpa.show-sql=true
```

### Hot Reload
Spring Boot DevTools is included - code changes trigger automatic restart.

### Building for Production
```bash
./mvnw clean package -DskipTests
```

JAR file location: `target/anchornotes-0.0.1-SNAPSHOT.jar`

### Running in Production
```bash
# Set production environment variables
export SPRING_PROFILES_ACTIVE=prod
export SUPABASE_URL=https://your-project.supabase.co
export SUPABASE_ANON_KEY=your_anon_key
export SUPABASE_SERVICE_ROLE=your_service_role
export SUPABASE_DB_URL=jdbc:postgresql://...
export SUPABASE_DB_USER=postgres
export SUPABASE_DB_PASSWORD=your_password

# Run the JAR
java -jar target/anchornotes-0.0.1-SNAPSHOT.jar
```

---

## API Response Format

### Success Response
```json
{
  "id": "123",
  "title": "My Note",
  "text": "Content...",
  "pinned": false,
  "lastEdited": "2025-10-30T23:10:00Z",
  "createdAt": "2025-10-30T20:00:00Z",
  "tags": [{"id": "7", "name": "work", "color": "#8B5CF6"}],
  "geofence": {"id": "note_123", "latitude": 34.0211, "longitude": -118.2893, "radius": 150},
  "reminderTimeUtc": "2025-11-03T17:00:00Z",
  "image": {"id": "19", "url": "https://..."},
  "audio": {"id": "21", "url": "https://...", "durationSec": 42},
  "hasPhoto": true,
  "hasAudio": true
}
```

### Error Response
```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Note not found"
  }
}
```

### HTTP Status Codes
- `200 OK` - Request succeeded
- `201 Created` - Resource created successfully
- `204 No Content` - Deletion successful
- `400 Bad Request` - Invalid request data (validation errors)
- `401 Unauthorized` - Missing or invalid authentication token
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Common Issues & Troubleshooting

### Maven Wrapper Missing

**Problem:** `mvnw` or `mvnw.cmd` not found

**Solution:**
```bash
mvn wrapper:wrapper
chmod +x mvnw  # Unix/macOS only
```

### Java Version Mismatch

**Problem:** `Unsupported class file major version` error

**Solution:**
```bash
java -version  # Should show version 21.x

# If not, install Java 21 and set JAVA_HOME
export JAVA_HOME=/path/to/java21
```

### Port 8080 Already in Use

**Problem:** `Port 8080 was already in use`

**Solution:**
```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9

# Or change port in application.properties
server.port=8081
```

### Database Connection Refused

**Problem:** Cannot connect to Supabase database

**Solution:**
- Check that Supabase allows connections from your IP
- Verify database credentials in `.env`
- Ensure connection string uses port 5432 (not 6543)
- Check `sslmode=require` is in connection string

### JWT Validation Errors

**Problem:** `Unauthorized` errors with valid token

**Solution:**
- Ensure `SUPABASE_SERVICE_ROLE` key is correct
- Check token expiration
- Verify token is included in `Authorization: Bearer <token>` header

### Schema Errors

**Problem:** `relation does not exist` or column errors

**Solution:**
- Re-run the cleanup script: `database/CLEANUP_SCHEMA.sql`
- Check that all tables have `user_id` columns
- Verify indexes were created

### Environment Variables Not Loading

**Problem:** `Not enough variable values available`

**Solution:**
- Ensure `.env` file is at project root
- Check file has correct format (no quotes around values)
- Restart the application after creating/modifying `.env`

### Email Verification Required

**Problem:** Registration returns `EMAIL_VERIFICATION_REQUIRED` token

**Solution:**
Go to Supabase Dashboard → Authentication → Providers → Email → Turn OFF "Confirm email"

---

## Testing the API

### Using the HTML Test Interface

Open `backend/api-tester.html` in your browser for an interactive API testing interface.

### Using cURL

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"test123","fullName":"Test User"}'
```

**Create Note:**
```bash
curl -X POST http://localhost:8080/api/notes \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"My Note","text":"Hello World"}'
```

---

## Quick Reference Commands

### Development
```bash
# Start development server
./mvnw spring-boot:run

# Run tests
./mvnw test

# Clean build
./mvnw clean install

# Skip tests during build
./mvnw clean install -DskipTests
```

### Production
```bash
# Build JAR
./mvnw clean package

# Run JAR
java -jar target/anchornotes-0.0.1-SNAPSHOT.jar
```

---

## Project Maintainers

For questions or issues:
1. Check this README first
2. Review the troubleshooting section
3. Check Supabase Dashboard for auth-related issues
4. Review application logs in the terminal

## License

CSCI 310 - USC - Team 3
