package com.example.businessLogic.controller;
import com.example.businessLogic.entity.Task;
import com.example.businessLogic.entity.User;
import com.example.businessLogic.service.TaskService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
     private final TaskService taskService;
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }


    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        return ResponseEntity.ok(taskService.createTask(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable String id, @RequestBody Task taskDetails) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Task>> searchTasksByTitle(@RequestParam String title) {
        return ResponseEntity.ok(taskService.searchTasksByTitle(title));
    }

    @GetMapping("/creator/{creator}")
    public ResponseEntity<List<Task>> getTasksByCreator(@PathVariable User creator) {
        return ResponseEntity.ok(taskService.getTasksByCreator(creator));
    }

    @GetMapping("/status/{statusId}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable Integer statusId) {
        return ResponseEntity.ok(taskService.getTasksByStatus(statusId));
    }

    @GetMapping("/priority/{priorityId}")
    public ResponseEntity<List<Task>> getTasksByPriority(@PathVariable Integer priorityId) {
        return ResponseEntity.ok(taskService.getTasksByPriority(priorityId));
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<List<Task>> getTasksByAssignee(@PathVariable String userId) {
        return ResponseEntity.ok(taskService.getTasksByAssignee(userId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.getOverdueTasks());
    }

    @GetMapping("/upcoming-deadlines")
    public ResponseEntity<List<Task>> getTasksWithUpcomingDeadlines(
            @RequestParam(defaultValue = "7") int daysAhead) {
        return ResponseEntity.ok(taskService.getTasksWithUpcomingDeadlines(daysAhead));
    }

    @GetMapping("/personal/{userId}")
    public ResponseEntity<List<Task>> getTasksByPersonalId(@PathVariable String userId) {
        return ResponseEntity.ok(taskService.getPersonalTasks(userId));
    }

}