package com.example.businessLogic.service;

import com.example.businessLogic.dto.NotificationRequest;
import com.example.businessLogic.entity.*;
import com.example.businessLogic.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final MetricsService metricsService;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    @Transactional
    public Task createTask(Task task) {
        if (task.getCreateDate() == null) {
            task.setCreateDate(LocalDate.now());
        }

        if (task.getPriority() != null && task.getPriority().getId() != null) {
            Priority priority = priorityRepository.findById(task.getPriority().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Priority not found"));
            task.setPriority(priority);
        }

        if (task.getStatus() != null && task.getStatus().getId() != null) {
            Status status = statusRepository.findById(task.getStatus().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Status not found"));
            task.setStatus(status);

            if (status.getName().equals("завершена")) {
                task.setCompletionDate(LocalDate.now());
            }
        }

        if (task.getAssignees() != null) {
            Set<User> managedAssignees = task.getAssignees().stream()
                    .map(assignee -> userRepository.findById(assignee.getId())
                            .orElseThrow(() -> new EntityNotFoundException("User not found")))
                    .collect(Collectors.toSet());
            task.setAssignees(managedAssignees);
        }

        if (task.getCreator() != null && task.getCreator().getId() != null) {
            User creator = userRepository.findById(task.getCreator().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Creator not found"));
            task.setCreator(creator);
        }

        if (task.getProject() != null && task.getProject().getId() != null) {
            Project project = projectRepository.findById(task.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            task.setProject(project);
        }

        Task savedTask = taskRepository.save(task);

        notifyAssignees(savedTask);
        metricsService.recordTaskCreated();

        return savedTask;
    }

    @Transactional
    public Task updateTask(String id, Task taskDetails) {
        Task task = getTaskById(id);

        checkIfTaskCompleted(task);

        updateTitle(task, taskDetails);
        updateDates(task, taskDetails);
        updateAssignees(task, taskDetails);
        updatePriority(task, taskDetails);
        updateStatus(task, taskDetails);
        updateProject(task, taskDetails);

        return taskRepository.save(task);
    }

    private void checkIfTaskCompleted(Task task) {
        if (task.getStatus().getId().equals(2)) {
            throw new IllegalStateException("Задача уже завершена");
        }
    }

    private void updateTitle(Task task, Task taskDetails) {
        if (taskDetails.getTitle() != null) {
            task.setTitle(taskDetails.getTitle());
        }
    }

    private void updateDates(Task task, Task taskDetails) {
        if (taskDetails.getStartDate() != null) {
            task.setStartDate(taskDetails.getStartDate());
        }
        if (taskDetails.getDeadlineDate() != null) {
            task.setDeadlineDate(taskDetails.getDeadlineDate());
        }
    }

    private void updateAssignees(Task task, Task taskDetails) {
        if (taskDetails.getAssignees() == null) {
            return;
        }

        Set<User> managedAssignees = taskDetails.getAssignees().stream()
                .map(assignee -> {
                    if (assignee.getId() == null) {
                        throw new IllegalArgumentException("User ID cannot be null");
                    }
                    return userRepository.findById(assignee.getId())
                            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + assignee.getId()));
                })
                .collect(Collectors.toSet());
        task.setAssignees(managedAssignees);
    }

    private void updatePriority(Task task, Task taskDetails) {
        if (taskDetails.getPriority() == null) {
            return;
        }

        if (taskDetails.getPriority().getId() != null) {
            Priority priority = priorityRepository.findById(taskDetails.getPriority().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Priority not found"));
            task.setPriority(priority);
        } else {
            task.setPriority(null);
        }
    }

    private void updateStatus(Task task, Task taskDetails) {
        if (taskDetails.getStatus() == null) {
            return;
        }

        if (taskDetails.getStatus().getId() != null) {
            Status status = statusRepository.findById(taskDetails.getStatus().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Status not found"));
            task.setStatus(status);

            if ("завершена".equals(status.getName())) {
                task.setCompletionDate(LocalDate.now());
            }
        } else {
            task.setStatus(null);
        }
    }

    private void updateProject(Task task, Task taskDetails) {
        if (taskDetails.getProject() == null) {
            return;
        }

        if (taskDetails.getProject().getId() != null) {
            Project project = projectRepository.findById(taskDetails.getProject().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found"));
            task.setProject(project);
        } else {
            task.setProject(null);
        }
    }


    @Transactional
    public void deleteTask(String id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    public List<Task> searchTasksByTitle(String title) {
        return taskRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Task> getTasksByCreator(User creator) {
        return taskRepository.findByCreatorId(creator.getId());
    }

    public List<Task> getTasksByStatus(Integer statusId) {
        return taskRepository.findByStatusId(statusId);
    }

    public List<Task> getTasksByPriority(Integer priorityId) {
        return taskRepository.findByPriorityId(priorityId);
    }

    public List<Task> getTasksByAssignee(String userId) {
        return taskRepository.findByAssigneeId(userId);
    }

    public List<Task> getOverdueTasks() {
        return taskRepository.findByDeadlineDateBefore(LocalDate.now());
    }

    public List<Task> getTasksWithUpcomingDeadlines(int daysAhead) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(daysAhead);
        return taskRepository.findTasksWithUpcomingDeadlines(start, end);
    }

    public List<Task> getProjectTasks(String projectId) {
        Project project = projectRepository.getReferenceById(projectId);
        return taskRepository.getTasksByProject(project);
    }

    public List<Task> getPersonalTasks(String userId) {
        User user = userRepository.getReferenceById(userId);

        return getTasksByCreator(user).stream()
                .filter(Task::isPersonal)
                .toList();
    }

    private void notifyAssignees(Task task) {
        if (task.getAssignees() == null || task.getAssignees().isEmpty()) {
            return;
        }

        String creatorId = task.getCreator().getId();
        String projectName = task.getProject() != null ? task.getProject().getName() : null;

        for (User assignee : task.getAssignees()) {
            if (creatorId.equals(assignee.getId())) {
                continue;
            }

            try {
                NotificationRequest notification = NotificationRequest.builder()
                        .title("Новая задача")
                        .message(task.getCreator().getUsername() + " назначил вам задачу \"" + task.getTitle() + "\""
                                + " в проекте \"" + projectName + "\"")
                        .senderId(creatorId)
                        .type(NotificationType.TASK_ASSIGMENT)
                        .build();

                notificationService.sendNotification(assignee.getId(), notification);
                log.info("Уведомление отправлено: userId={}, taskId={}", assignee.getId(), task.getId());
            } catch (Exception e) {
                log.error("Ошибка отправки уведомления исполнителю: userId={}, taskId={}", assignee.getId(), task.getId());
            }
        }
    }

}
