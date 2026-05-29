package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.event.BotPickNeededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotPickTrigger {

    private final DraftAutoPickService draftAutoPickService;
    private final Set<Long> inFlightGameSeqs = ConcurrentHashMap.newKeySet();

    @Async
    @EventListener
    public void onBotPickNeeded(BotPickNeededEvent event) {
        Long gameSeq = event.gameSeq();
        if (!inFlightGameSeqs.add(gameSeq)) {
            return;
        }
        try {
            Thread.sleep(1000 + ThreadLocalRandom.current().nextInt(2000));
            draftAutoPickService.autoPick(gameSeq, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[BOT] onBotPickNeeded interrupted for game={}", gameSeq);
        } catch (Exception e) {
            log.warn("[BOT] autoPick failed for game={}, retrying once", gameSeq, e);
            try {
                draftAutoPickService.autoPick(gameSeq, true);
            } catch (Exception ex) {
                log.error("[BOT] autoPick retry failed for game={}", gameSeq, ex);
            }
        } finally {
            inFlightGameSeqs.remove(gameSeq);
        }
    }
}
