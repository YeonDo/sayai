package com.sayai.record.service;

import com.sayai.record.model.Ligue;
import com.sayai.record.repository.LigueRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class LigueService {
    private final LigueRepository ligueRepository;

    public Optional<Ligue> findByName(String name){
        return ligueRepository.findByName(name);
    }
}
