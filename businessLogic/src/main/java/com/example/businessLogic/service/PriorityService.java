package com.example.businessLogic.service;

import com.example.businessLogic.repository.PriorityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class PriorityService {
    private PriorityRepository priorityRepository;
}
