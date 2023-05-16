package com.sayai.record.service;

import com.sayai.record.model.Pitch;
import com.sayai.record.repository.PitchRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PitchService {
    private final PitchRepository pitchRepository;

    @Transactional
    public Pitch save(Pitch pitch){
        return pitchRepository.save(pitch);
    }
    @Transactional
    public void saveAll(List<Pitch> pitchList){
        pitchRepository.saveAll(pitchList);
    }
    public Optional<Pitch> findPitch(Long id){
        return pitchRepository.findById(id);
    }
}
