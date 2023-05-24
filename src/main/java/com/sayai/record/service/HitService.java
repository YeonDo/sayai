package com.sayai.record.service;

import com.sayai.record.model.Hit;
import com.sayai.record.repository.HitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class HitService {
    private final HitRepository hitRepository;
    @Transactional
    public Hit saveHit(Hit hit){
        return hitRepository.save(hit);
    }
    @Transactional
    public void saveAll(List<Hit> hitList){
        hitRepository.saveAll(hitList);
    }
    public Optional<Hit> findHit(Long id){
        return hitRepository.findById(id);
    }

    public List<Hit> findAllHit(Long playerid){
        return hitRepository.findAll();
    }
}
