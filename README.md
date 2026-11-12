# CodeCraftHub

CodeCraftHub is a lightweight REST API for tracking personal learning goals as courses. It is built with Java 17 and Spring Boot, and stores course data in a local JSON file.

## Features

- Create, list, view, update, and delete courses
- Automatically assigns course IDs and creation timestamps
- Validates required fields, target dates, and course status
- Persists data in `courses.json` in the application's working directory
- Returns structured JSON errors for invalid requests and missing courses

## Requirements

- Java 17 or later
- Apache Maven 3.6.3 or later

## Installation

Clone the repository and change into its directory:

```bash
git clone <repository-url>
cd codecrafthub
```

Confirm Java and Maven are available:

```bash
java -version
mvn -version
```

## Run the application

Start the application from the project directory:

```bash
mvn spring-boot:run
```

The API listens at `http://localhost:9898`. The first request creates `courses.json` with an empty course list if the file does not exist. Keep the application running while making requests.

To package and run an executable JAR instead:

```bash
mvn clean package
java -jar target/codecrafthub-1.0-SNAPSHOT.jar
```

## API

All endpoints use the `/api/courses` base path and accept and return JSON unless noted otherwise.

| Method | Path | Description | Success response |
|---|---|---|---|
| `GET` | `/api/courses` | List all courses | `200 OK`, JSON array |
| `GET` | `/api/courses/{id}` | Get a course by ID | `200 OK`, course object |
| `POST` | `/api/courses` | Create a course | `201 Created`, course object |
| `PUT` | `/api/courses/{id}` | Replace editable fields of a course | `200 OK`, course object |
| `DELETE` | `/api/courses/{id}` | Delete a course | `204 No Content` |

Course request fields:

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | Yes | Must not be blank |
| `description` | string | Yes | Must not be blank |
| `target_date` | string | Yes | Date in `YYYY-MM-DD` format |
| `status` | string | Yes | Exactly `Not Started`, `In Progress`, or `Completed` |
| `id` | integer | No | Assigned by the server; ignored for create/update |
| `created_at` | timestamp | No | Assigned by the server; preserved on update |

### Create a course

```bash
curl -i -X POST http://localhost:9898/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Learn Spring Boot",
    "description": "Build a REST API with Spring Boot",
    "target_date": "2026-12-15",
    "status": "In Progress"
  }'
```

The response includes the generated `id` and `created_at`, for example:

```json
{
  "id": 1,
  "name": "Learn Spring Boot",
  "description": "Build a REST API with Spring Boot",
  "target_date": "2026-12-15",
  "status": "In Progress",
  "created_at": "2026-11-12T04:22:51.123Z"
}
```

### List and get courses

```bash
curl http://localhost:9898/api/courses
curl http://localhost:9898/api/courses/1
```

### Update a course

`PUT` replaces all editable fields. Include every required field; the course ID and creation timestamp remain unchanged.

```bash
curl -i -X PUT http://localhost:9898/api/courses/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Learn Spring Boot",
    "description": "Finish the REST API and add tests",
    "target_date": "2027-01-15",
    "status": "Completed"
  }'
```

### Delete a course

```bash
curl -i -X DELETE http://localhost:9898/api/courses/1
```

### Errors

Errors are JSON objects containing `timestamp`, `status`, `error`, and `message`. Validation errors may also include an `errors` object keyed by field name.

- `400 Bad Request`: missing or blank fields, unsupported status, malformed JSON, or invalid `target_date` format
- `404 Not Found`: the requested course ID does not exist
- `500 Internal Server Error`: unexpected server or `courses.json` storage error

Example validation error:

```json
{
  "timestamp": "2026-11-12T04:22:51.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "errors": {
    "name": "name is required"
  }
}
```

## Troubleshooting

- **Maven or Java is not recognized:** Install Java 17+ and Maven, then open a new terminal and verify `java -version` and `mvn -version`.
- **Port 9898 is already in use:** Stop the other process using the port, or start this application with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9899` and use port `9899` in requests.
- **Requests fail to connect:** Confirm the application started successfully and use the configured port (`9898` by default).
- **A request returns `400 Bad Request`:** Send valid JSON with all required fields. `target_date` must use `YYYY-MM-DD`, and `status` must match one of the three allowed values exactly.
- **A request returns `404 Not Found`:** Check that the course ID exists with `GET /api/courses`.
- **The application cannot read or write courses:** Ensure the application's working directory is writable and that `courses.json` contains a valid JSON array. The file is created automatically when absent; back up any data before repairing or replacing it.