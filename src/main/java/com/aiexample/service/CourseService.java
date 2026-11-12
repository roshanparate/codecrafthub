package com.aiexample.service;

import com.aiexample.exception.CourseNotFoundException;
import com.aiexample.exception.InvalidCourseException;
import com.aiexample.exception.StorageException;
import com.aiexample.exception.CourseNotFoundException;
import com.aiexample.model.Course;
import com.aiexample.model.Course;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service responsible for course business logic and JSON file operations.
 */
@Service
public class CourseService {

    /**
     * The JSON file is stored in the application's current working directory.
     */
    private static final Path COURSES_FILE = Paths.get("courses.json");

    /**
     * These are the only status values accepted by the API.
     */
    private static final List<String> VALID_STATUSES = List.of(
            "Not Started",
            "In Progress",
            "Completed"
    );

    private final ObjectMapper objectMapper;

    /**
     * Spring automatically provides the configured Jackson ObjectMapper.
     */
    public CourseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Returns all courses from courses.json.
     */
    public synchronized List<Course> getAllCourses() {
        return readCoursesFromFile();
    }

    /**
     * Finds a course by ID.
     *
     * @throws CourseNotFoundException if no course has the requested ID
     */
    public synchronized Course getCourseById(Long id) {
        return readCoursesFromFile()
                .stream()
                .filter(course -> course.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                "Course with id " + id + " was not found"
                        )
                );
    }

    /**
     * Creates a new course.
     *
     * The ID and created_at timestamp are generated here rather than
     * being accepted from the client.
     */
    public synchronized Course createCourse(Course course) {
        validateCourse(course);

        List<Course> courses = readCoursesFromFile();

        long nextId = courses.stream()
                .map(Course::getId)
                .filter(id -> id != null)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;

        course.setId(nextId);
        course.setCreatedAt(Instant.now());

        courses.add(course);
        writeCoursesToFile(courses);

        return course;
    }

    /**
     * Updates an existing course.
     *
     * The ID and created_at timestamp cannot be changed by the client.
     */
    public synchronized Course updateCourse(Long id, Course updatedCourse) {
        validateCourse(updatedCourse);

        List<Course> courses = readCoursesFromFile();

        Course existingCourse = courses.stream()
                .filter(course -> course.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                "Course with id " + id + " was not found"
                        )
                );

        // Update only editable fields.
        existingCourse.setName(updatedCourse.getName());
        existingCourse.setDescription(updatedCourse.getDescription());
        existingCourse.setTargetDate(updatedCourse.getTargetDate());
        existingCourse.setStatus(updatedCourse.getStatus());

        // Keep the original ID and createdAt values.
        writeCoursesToFile(courses);

        return existingCourse;
    }

    /**
     * Deletes a course by ID.
     */
    public synchronized void deleteCourse(Long id) {
        List<Course> courses = readCoursesFromFile();

        boolean wasRemoved = courses.removeIf(
                course -> course.getId().equals(id)
        );

        if (!wasRemoved) {
            throw new CourseNotFoundException(
                    "Course with id " + id + " was not found"
            );
        }

        writeCoursesToFile(courses);
    }

    /**
     * Validates business rules that are not fully covered by annotations.
     */
    private void validateCourse(Course course) {
        if (course == null) {
            throw new InvalidCourseException("Course request cannot be empty");
        }

        if (course.getStatus() == null ||
                !VALID_STATUSES.contains(course.getStatus())) {

            throw new InvalidCourseException(
                    "status must be exactly one of: "
                            + String.join(", ", VALID_STATUSES)
            );
        }
    }

    /**
     * Reads all courses from the JSON file.
     *
     * If the file does not exist, it is created automatically with
     * an empty JSON array.
     */
    private List<Course> readCoursesFromFile() {
        ensureFileExists();

        try {
            String json = Files.readString(COURSES_FILE);

            // An empty file is treated as an empty course list.
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }

            List<Course> courses = objectMapper.readValue(
                    json,
                    new TypeReference<List<Course>>() {
                    }
            );

            // Protect the rest of the application from a null list.
            return courses == null ? new ArrayList<>() : courses;

        } catch (JsonProcessingException exception) {
            throw new StorageException(
                    "courses.json contains invalid JSON",
                    exception
            );
        } catch (IOException exception) {
            throw new StorageException(
                    "Could not read courses.json",
                    exception
            );
        }
    }

    /**
     * Writes all courses to courses.json.
     */
    private void writeCoursesToFile(List<Course> courses) {
        try {
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(courses);

            Files.writeString(COURSES_FILE, json);

        } catch (JsonProcessingException exception) {
            throw new StorageException(
                    "Could not convert courses to JSON",
                    exception
            );
        } catch (IOException exception) {
            throw new StorageException(
                    "Could not write courses.json",
                    exception
            );
        }
    }

    /**
     * Creates courses.json automatically if it does not exist.
     */
    private void ensureFileExists() {
        try {
            if (Files.notExists(COURSES_FILE)) {
                Files.writeString(COURSES_FILE, "[]");
            }
        } catch (IOException exception) {
            throw new StorageException(
                    "Could not create courses.json",
                    exception
            );
        }
    }
}
