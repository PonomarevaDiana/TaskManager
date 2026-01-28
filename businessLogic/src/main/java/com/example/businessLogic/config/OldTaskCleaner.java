package com.example.businessLogic.config;

import com.example.businessLogic.entity.Project;
import com.example.businessLogic.entity.Task;
import com.example.businessLogic.repository.ProjectRepository;
import com.example.businessLogic.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@AllArgsConstructor
public class OldTaskCleaner {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void deleteOldCompletedTasks() {
        List<Project> projects = projectRepository.findAll();

        for (Project project : projects) {
            if (project.getAutoDeleteDays() == null) {
                continue;
            }

            LocalDate cutoffDate = LocalDate.now().minusDays(project.getAutoDeleteDays());

            List<Task> oldTasks = taskRepository.findCompletedTasksByProjectAndCompletedBefore(project.getId(), cutoffDate);

            if (!oldTasks.isEmpty()) {
                taskRepository.deleteAll(oldTasks);
            }
        }
    }
}
