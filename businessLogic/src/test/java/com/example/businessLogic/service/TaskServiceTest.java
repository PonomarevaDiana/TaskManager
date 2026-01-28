package com.example.businessLogic.service;

import com.example.businessLogic.entity.*;
import com.example.businessLogic.repository.PriorityRepository;
import com.example.businessLogic.repository.ProjectRepository;
import com.example.businessLogic.repository.StatusRepository;
import com.example.businessLogic.repository.TaskRepository;
import com.example.businessLogic.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PriorityRepository priorityRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getAllTasks_ShouldReturnAllTasks() {
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findAll()).thenReturn(expectedTasks);

        List<Task> result = taskService.getAllTasks();

        assertEquals(expectedTasks, result);
        verify(taskRepository).findAll();
    }

    @Test
    void getTaskById_WhenTaskExists_ShouldReturnTask() {
        String taskId = "task-123";
        Task expectedTask = new Task();
        expectedTask.setId(taskId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(expectedTask));

        Task result = taskService.getTaskById(taskId);

        assertEquals(expectedTask, result);
        verify(taskRepository).findById(taskId);
    }

    @Test
    void getTaskById_WhenTaskNotExists_ShouldThrowException() {
        String taskId = "non-existent-task";
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskById(taskId));
        assertEquals("Task not found with id: " + taskId, exception.getMessage());
    }


    @Test
    void createTask_WithPriorityNotFound_ShouldThrowException() {
        Task task = new Task();
        Priority priority = new Priority();
        priority.setId(999);
        task.setPriority(priority);

        when(priorityRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskService.createTask(task));
        assertEquals("Priority not found", exception.getMessage());

        verify(taskRepository, never()).save(any(Task.class));
        verify(metricsService, never()).recordTaskCreated(anyString());
    }

    @Test
    void createTask_WithStatusNotFound_ShouldThrowException() {
        Task task = new Task();
        Status status = new Status();
        status.setId(999);
        task.setStatus(status);

        Priority priority = new Priority();
        priority.setId(1);
        task.setPriority(priority);

        when(priorityRepository.findById(1)).thenReturn(Optional.of(priority));
        when(statusRepository.findById(999)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskService.createTask(task));
        assertEquals("Status not found", exception.getMessage());

        verify(taskRepository, never()).save(any(Task.class));
        verify(metricsService, never()).recordTaskCreated(anyString());
    }

    @Test
    void updateTask_WithValidData_ShouldUpdateTask() {
        String taskId = "task-123";

        Status existingStatus = new Status();
        existingStatus.setId(1);
        existingStatus.setName("In Progress");

        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setTitle("Old Title");
        existingTask.setStatus(existingStatus);

        Task taskDetails = new Task();
        taskDetails.setTitle("New Title");
        taskDetails.setStartDate(LocalDate.now());
        taskDetails.setDeadlineDate(LocalDate.now().plusDays(7));

        Priority priority = new Priority();
        priority.setId(1);
        taskDetails.setPriority(priority);

        Status status = new Status();
        status.setId(1);
        taskDetails.setStatus(status);

        Set<User> assignees = new HashSet<>();
        User assignee = new User();
        assignee.setId("user2");
        assignees.add(assignee);
        taskDetails.setAssignees(assignees);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(priorityRepository.findById(1)).thenReturn(Optional.of(priority));
        when(statusRepository.findById(1)).thenReturn(Optional.of(status));
        when(userRepository.findById("user2")).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        Task result = taskService.updateTask(taskId, taskDetails);

        assertNotNull(result);
        verify(taskRepository).save(existingTask);
    }

    @Test
    void updateTask_WhenTaskIsCompleted_ShouldThrowException() {
        String taskId = "task-123";

        Status completedStatus = new Status();
        completedStatus.setId(2);
        completedStatus.setName("завершена");

        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setTitle("Old Title");
        existingTask.setStatus(completedStatus);

        Task taskDetails = new Task();
        taskDetails.setTitle("New Title");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> taskService.updateTask(taskId, taskDetails));
        assertEquals("Задача уже завершена", exception.getMessage());

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTask_WhenTaskNotExists_ShouldThrowException() {
        String taskId = "non-existent-task";
        Task taskDetails = new Task();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateTask(taskId, taskDetails));
        assertEquals("Task not found with id: " + taskId, exception.getMessage());

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteTask_WhenTaskExists_ShouldDeleteTask() {
        String taskId = "task-123";
        Task task = new Task();
        task.setId(taskId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(taskRepository).delete(task);

        taskService.deleteTask(taskId);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_WhenTaskNotExists_ShouldThrowException() {
        String taskId = "non-existent-task";
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.deleteTask(taskId));
        assertEquals("Task not found with id: " + taskId, exception.getMessage());

        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void searchTasksByTitle_ShouldReturnMatchingTasks() {
        String searchTitle = "test";
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findByTitleContainingIgnoreCase(searchTitle)).thenReturn(expectedTasks);

        List<Task> result = taskService.searchTasksByTitle(searchTitle);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByTitleContainingIgnoreCase(searchTitle);
    }

    @Test
    void getTasksByCreator_ShouldReturnCreatorTasks() {
        User creator = new User();
        creator.setId("user1");
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findByCreatorId("user1")).thenReturn(expectedTasks);

        List<Task> result = taskService.getTasksByCreator(creator);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByCreatorId("user1");
    }

    @Test
    void getTasksByStatus_ShouldReturnStatusTasks() {
        Integer statusId = 1;
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findByStatusId(statusId)).thenReturn(expectedTasks);

        List<Task> result = taskService.getTasksByStatus(statusId);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByStatusId(statusId);
    }

    @Test
    void getTasksByPriority_ShouldReturnPriorityTasks() {
        Integer priorityId = 1;
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findByPriorityId(priorityId)).thenReturn(expectedTasks);

        List<Task> result = taskService.getTasksByPriority(priorityId);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByPriorityId(priorityId);
    }

    @Test
    void getTasksByAssignee_ShouldReturnAssigneeTasks() {
        String userId = "user1";
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findByAssigneeId(userId)).thenReturn(expectedTasks);

        List<Task> result = taskService.getTasksByAssignee(userId);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByAssigneeId(userId);
    }

    @Test
    void getOverdueTasks_ShouldReturnOverdueTasks() {
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findByDeadlineDateBefore(any(LocalDate.class))).thenReturn(expectedTasks);

        List<Task> result = taskService.getOverdueTasks();

        assertEquals(expectedTasks, result);
        verify(taskRepository).findByDeadlineDateBefore(any(LocalDate.class));
    }

    @Test
    void getTasksWithUpcomingDeadlines_ShouldReturnTasks() {
        int daysAhead = 7;
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(daysAhead);
        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());
        when(taskRepository.findTasksWithUpcomingDeadlines(start, end)).thenReturn(expectedTasks);

        List<Task> result = taskService.getTasksWithUpcomingDeadlines(daysAhead);

        assertEquals(expectedTasks, result);
        verify(taskRepository).findTasksWithUpcomingDeadlines(start, end);
    }
    @Test
    void createTask_WithValidData_ShouldCreateTask() {
        Task task = new Task();
        task.setTitle("Test Task");

        Priority priority = new Priority();
        priority.setId(1);
        task.setPriority(priority);

        Status status = new Status();
        status.setId(1);
        status.setName("в процессе");
        task.setStatus(status);

        User creator = new User();
        creator.setId("user1");
        task.setCreator(creator);

        User assignee = new User();
        assignee.setId("user2");
        task.setAssignees(Set.of(assignee));

        Project project = new Project();
        project.setId("project1");
        project.setName("Test Project");
        task.setProject(project);

        when(priorityRepository.findById(1)).thenReturn(Optional.of(priority));
        when(statusRepository.findById(1)).thenReturn(Optional.of(status));
        when(userRepository.findById("user1")).thenReturn(Optional.of(creator));
        when(userRepository.findById("user2")).thenReturn(Optional.of(assignee));
        when(projectRepository.findById("project1")).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(task);

        assertNotNull(result);
        assertNotNull(result.getCreateDate());
        verify(taskRepository).save(task);
        verify(metricsService).recordTaskCreated(task.getId());
        verify(notificationService, atLeastOnce()).sendNotification(anyString(), any());
    }

    @Test
    void createTask_WithCompletedStatus_ShouldSetCompletionDate() {
        Task task = new Task();

        Status status = new Status();
        status.setId(2);
        status.setName("завершена");
        task.setStatus(status);

        Priority priority = new Priority();
        priority.setId(1);
        task.setPriority(priority);

        when(priorityRepository.findById(1)).thenReturn(Optional.of(priority));
        when(statusRepository.findById(2)).thenReturn(Optional.of(status));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(task);

        assertNotNull(result);
        assertNotNull(result.getCompletionDate());
        assertEquals(LocalDate.now(), result.getCompletionDate());
    }

    @Test
    void createTask_WithoutOptionalFields_ShouldCreateTask() {
        Task task = new Task();
        task.setTitle("Test Task");
        task.setCreateDate(LocalDate.now());

        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(task);

        assertNotNull(result);
        verify(taskRepository).save(task);
        verify(metricsService).recordTaskCreated(task.getId());
    }

    @Test
    void createTask_WithCreatorNotFound_ShouldThrowException() {
        Task task = new Task();
        User creator = new User();
        creator.setId("non-existent-user");
        task.setCreator(creator);

        when(userRepository.findById("non-existent-user")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskService.createTask(task));
        assertEquals("Creator not found", exception.getMessage());
    }

    @Test
    void createTask_WithAssigneeNotFound_ShouldThrowException() {
        Task task = new Task();
        User assignee = new User();
        assignee.setId("non-existent-user");
        task.setAssignees(Set.of(assignee));

        when(userRepository.findById("non-existent-user")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskService.createTask(task));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void createTask_WithProjectNotFound_ShouldThrowException() {
        Task task = new Task();
        Project project = new Project();
        project.setId("non-existent-project");
        task.setProject(project);

        when(projectRepository.findById("non-existent-project")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskService.createTask(task));
        assertEquals("Project not found", exception.getMessage());
    }

    @Test
    void updateTask_WithNullFields_ShouldHandleCorrectly() {
        String taskId = "task-123";

        Status existingStatus = new Status();
        existingStatus.setId(1);

        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setStatus(existingStatus);

        Task taskDetails = new Task();
        taskDetails.setPriority(null);
        taskDetails.setStatus(null);
        taskDetails.setProject(null);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        Task result = taskService.updateTask(taskId, taskDetails);

        assertNotNull(result);
        verify(taskRepository).save(existingTask);
    }

    @Test
    void updateTask_WithCompletionStatus_ShouldSetCompletionDate() {
        String taskId = "task-123";

        Status existingStatus = new Status();
        existingStatus.setId(1);
        existingStatus.setName("в процессе");

        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setStatus(existingStatus);

        Status completedStatus = new Status();
        completedStatus.setId(2);
        completedStatus.setName("завершена");

        Task taskDetails = new Task();
        taskDetails.setStatus(completedStatus);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(statusRepository.findById(2)).thenReturn(Optional.of(completedStatus));
        when(taskRepository.save(any(Task.class))).thenReturn(existingTask);

        Task result = taskService.updateTask(taskId, taskDetails);

        assertNotNull(result);
        assertNotNull(result.getCompletionDate());
        assertEquals(LocalDate.now(), result.getCompletionDate());
    }

    @Test
    void updateTask_WithNullAssigneeId_ShouldThrowException() {
        String taskId = "task-123";

        Status existingStatus = new Status();
        existingStatus.setId(1);

        Task existingTask = new Task();
        existingTask.setId(taskId);
        existingTask.setStatus(existingStatus);

        Task taskDetails = new Task();
        User assignee = new User();
        assignee.setId(null);
        taskDetails.setAssignees(Set.of(assignee));

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTask(taskId, taskDetails));
        assertEquals("User ID cannot be null", exception.getMessage());
    }

    @Test
    void getProjectTasks_ShouldReturnProjectTasks() {
        String projectId = "project-123";
        Project project = new Project();
        project.setId(projectId);

        List<Task> expectedTasks = Arrays.asList(new Task(), new Task());

        when(projectRepository.getReferenceById(projectId)).thenReturn(project);
        when(taskRepository.getTasksByProject(project)).thenReturn(expectedTasks);

        List<Task> result = taskService.getProjectTasks(projectId);

        assertEquals(expectedTasks, result);
        verify(taskRepository).getTasksByProject(project);
    }

    @Test
    void notifyAssignees_WithNotificationError_ShouldLogError() {
        User creator = new User();
        creator.setId("user1");
        creator.setUsername("creator");

        User assignee = new User();
        assignee.setId("user2");

        Task task = new Task();
        task.setId("task-123");
        task.setTitle("Test Task");
        task.setCreator(creator);
        task.setAssignees(Set.of(assignee));

        when(userRepository.findById("user1")).thenReturn(Optional.of(creator));
        when(userRepository.findById("user2")).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        doThrow(new RuntimeException("Notification failed"))
                .when(notificationService)
                .sendNotification(anyString(), any());

        assertDoesNotThrow(() -> taskService.createTask(task));

        verify(taskRepository).save(task);
    }
}