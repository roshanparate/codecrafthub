package com.aiexample.controller;

import com.aiexample.model.Course;
import com.aiexample.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * POST /api/courses
     *
     * Creates a new course.
     */
    @PostMapping
    public ResponseEntity<Course> createCourse(
            @Valid @RequestBody Course course) {

        Course createdCourse = courseService.createCourse(course);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCourse);
    }

    /**
     * GET /api/courses
     *
     * Returns all courses.
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /**
     * GET /api/courses/{id}
     *
     * Returns one course by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(
            @PathVariable Long id) {

        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    /**
     * PUT /api/courses/{id}
     *
     * Replaces the editable data for an existing course.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody Course course) {

        return ResponseEntity.ok(
                courseService.updateCourse(id, course)
        );
    }

    /**
     * DELETE /api/courses/{id}
     *
     * Deletes a course.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id) {

        courseService.deleteCourse(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/courses/stats
     *
     * Returns the total number of courses and the number of courses
     * grouped by status.
     *
     * This endpoint is placed before /{id} for clarity because "stats"
     * is a fixed path, while {id} is a dynamic path variable.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCourseStatistics() {
        List<Course> courses = courseService.getAllCourses();

        // Use LinkedHashMap to keep the response order predictable.
        Map<String, Integer> coursesByStatus = new LinkedHashMap<>();

        // Include every valid status, even when its count is zero.
        coursesByStatus.put("Not Started", 0);
        coursesByStatus.put("In Progress", 0);
        coursesByStatus.put("Completed", 0);

        // Count each course according to its status.
        for (Course course : courses) {
            String status = course.getStatus();

            if (coursesByStatus.containsKey(status)) {
                coursesByStatus.put(
                        status,
                        coursesByStatus.get(status) + 1
                );
            }
        }

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("total_courses", courses.size());
        statistics.put("courses_by_status", coursesByStatus);

        return ResponseEntity.ok(statistics);
    }
}