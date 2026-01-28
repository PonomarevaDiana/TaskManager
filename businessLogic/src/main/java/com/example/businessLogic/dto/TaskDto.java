package com.example.businessLogic.dto;

import com.example.businessLogic.entity.Priority;
import com.example.businessLogic.entity.Status;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class  TaskDto{
    private String title;
    private Set<Integer> assigneeIds;
    private LocalDate startDate;
    private LocalDate deadlineDate;
    private Priority priorityId;
    private Status statusId;
    private String creator;
    private LocalDate createDate;

}
