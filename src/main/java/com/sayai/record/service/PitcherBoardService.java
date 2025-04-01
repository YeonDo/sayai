package com.sayai.record.service;

import com.sayai.record.model.HitterBoard;
import com.sayai.record.model.PitcherBoard;
import com.sayai.record.repository.PitcherBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PitcherBoardService {
    private final PitcherBoardRepository pitcherBoardRepository;

    public PitcherBoard save(PitcherBoard hitterBoard){
        return pitcherBoardRepository.save(hitterBoard);
    }

    public void saveAll(List<PitcherBoard> list){
        pitcherBoardRepository.saveAll(list);
    }
}
