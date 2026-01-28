package com.example.businessLogic.config;

import com.example.businessLogic.entity.Priority;
import com.example.businessLogic.entity.Status;
import com.example.businessLogic.repository.PriorityRepository;
import com.example.businessLogic.repository.StatusRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InitialDataLoader implements ApplicationRunner {
    PriorityRepository priorityRepository;
    StatusRepository statusRepository;

    public void run(ApplicationArguments args) {
        if (priorityRepository.count() == 0) {
            Priority priority = new Priority();
            priority.setName("Высокий");
            priorityRepository.save(priority);

            Priority priority2 = new Priority();
            priority2.setName("Средний");
            priorityRepository.save(priority2);

            Priority priority3 = new Priority();
            priority3.setName("Низкий");
            priorityRepository.save(priority3);
        }

        if (statusRepository.count() == 0) {
            Status status = new Status();
            status.setName("Активная");
            statusRepository.save(status);

            Status status2 = new Status();
            status2.setName("Завершена");
            statusRepository.save(status2);

            Status status3 = new Status();
            status3.setName("В работе");
            statusRepository.save(status3);

            Status status4 = new Status();
            status4.setName("На проверке");
            statusRepository.save(status4);
        }
    }
}
