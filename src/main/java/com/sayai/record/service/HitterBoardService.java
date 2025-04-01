package com.sayai.record.service;

import com.sayai.record.model.HitterBoard;
import com.sayai.record.repository.HitterBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HitterBoardService {
    private final HitterBoardRepository hitterBoardRepository;

    public HitterBoard save(HitterBoard hitterBoard){
        return hitterBoardRepository.save(hitterBoard);
    }

    public void saveAll(List<HitterBoard> list){
        hitterBoardRepository.saveAll(list);
    }
}
