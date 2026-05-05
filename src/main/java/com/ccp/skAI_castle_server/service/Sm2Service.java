package com.ccp.skAI_castle_server.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class Sm2Service {

    public record Sm2Result(LocalDate nextReviewDate, int reviewCount, double ef, int lastInterval) {}

    public int toQ(int score) {
        if (score >= 80) return 5;
        if (score >= 60) return 3;
        if (score >= 40) return 2;
        return 0;
    }

    // reviewCount = SM-2 n-1 (0-based), so SM-2 n = reviewCount + 1
    public Sm2Result compute(int score, int reviewCount, double ef, int lastInterval) {
        int q = toQ(score);
        int n = reviewCount + 1; // current n for interval calculation

        double newEf = Math.max(1.3, ef + 0.1 - (5 - q) * 0.08);

        if (q >= 3) {
            int newInterval;
            if (n == 1) newInterval = 1;
            else if (n == 2) newInterval = 6;
            else newInterval = Math.max(1, (int) Math.round(lastInterval * newEf));

            return new Sm2Result(
                    LocalDate.now().plusDays(newInterval),
                    reviewCount + 1,
                    newEf,
                    newInterval
            );
        } else {
            // Fail: reset to beginning (n=1, reviewCount=0)
            return new Sm2Result(LocalDate.now().plusDays(1), 0, newEf, 1);
        }
    }
}
