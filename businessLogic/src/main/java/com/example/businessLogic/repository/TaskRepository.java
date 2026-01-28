package com.example.businessLogic.repository;
import com.example.businessLogic.entity.Project;
import com.example.businessLogic.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    //название
    List<Task> findByTitleContainingIgnoreCase(String title);

    List<Task> findByCreatorId(String creatorId);

    //дате создания в определенном периоде
    List<Task> findByCreateDateBetween(LocalDate startDate, LocalDate endDate);

    //дедлайны
    List<Task> findByDeadlineDateBefore(LocalDate date);

    //статусы
    List<Task> findByStatusId(Integer statusId);

    //приоритет
    List<Task> findByPriorityId(Integer priorityId);

    //исполнители
    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :userId")
    List<Task> findByAssigneeId(@Param("userId") String userId);

    //дедлайны  в определенном периоде
    @Query("SELECT t FROM Task t WHERE t.deadlineDate BETWEEN :start AND :end")
    List<Task> findTasksWithUpcomingDeadlines(@Param("start") LocalDate start,
                                              @Param("end") LocalDate end);

    List<Task> getTasksByProject(Project project);

    void deleteByProjectId(String projectId);

    @Query("select t from Task t where t.project = :projectId and t.completionDate is not null and t.completionDate < :cutoff")
    List<Task> findCompletedTasksByProjectAndCompletedBefore(@Param("projectId") String projectId,
                                                             @Param("cutoff") LocalDate cutoff);
}