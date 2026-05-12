package com.ccp.skAI_castle_server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class Sm2ServiceTest {

    private final Sm2Service sm2Service = new Sm2Service();

    @ParameterizedTest(name = "score={0} → q={1}")
    @CsvSource({
        "100, 5",
        "80,  5",
        "79,  3",
        "60,  3",
        "59,  2",
        "40,  2",
        "39,  0",
        "0,   0"
    })
    void toQ_convertsScoreToGrade(int score, int expectedQ) {
        assertThat(sm2Service.toQ(score)).isEqualTo(expectedQ);
    }

    @Test
    void compute_firstReview_pass_interval1() {
        Sm2Service.Sm2Result result = sm2Service.compute(100, 0, 2.5, 1);

        assertThat(result.lastInterval()).isEqualTo(1);
        assertThat(result.reviewCount()).isEqualTo(1);
        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void compute_secondReview_pass_interval6() {
        Sm2Service.Sm2Result result = sm2Service.compute(80, 1, 2.5, 1);

        assertThat(result.lastInterval()).isEqualTo(6);
        assertThat(result.reviewCount()).isEqualTo(2);
        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(6));
    }

    @Test
    void compute_thirdReview_pass_usesLastIntervalTimesEf() {
        double ef = 2.5;
        int lastInterval = 6;
        Sm2Service.Sm2Result result = sm2Service.compute(80, 2, ef, lastInterval);

        double newEf = Math.max(1.3, ef + 0.1 - (5 - 5) * 0.08);
        int expectedInterval = Math.max(1, (int) Math.round(lastInterval * newEf));

        assertThat(result.lastInterval()).isEqualTo(expectedInterval);
        assertThat(result.reviewCount()).isEqualTo(3);
        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(expectedInterval));
    }

    @Test
    void compute_fail_resetsReviewCountAndInterval() {
        Sm2Service.Sm2Result result = sm2Service.compute(30, 5, 2.5, 20);

        assertThat(result.reviewCount()).isEqualTo(0);
        assertThat(result.lastInterval()).isEqualTo(1);
        assertThat(result.nextReviewDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void compute_efNeverGoesBelowMinimum() {
        // Repeated failures should not push ef below 1.3
        Sm2Service.Sm2Result r1 = sm2Service.compute(0, 0, 1.3, 1);
        assertThat(r1.ef()).isGreaterThanOrEqualTo(1.3);

        Sm2Service.Sm2Result r2 = sm2Service.compute(0, 0, r1.ef(), r1.lastInterval());
        assertThat(r2.ef()).isGreaterThanOrEqualTo(1.3);
    }

    @Test
    void compute_passQ3_efUpdatesCorrectly() {
        double initialEf = 2.5;
        // q=3 (score 60~79)
        Sm2Service.Sm2Result result = sm2Service.compute(60, 0, initialEf, 1);

        double expectedEf = Math.max(1.3, initialEf + 0.1 - (5 - 3) * 0.08);
        assertThat(result.ef()).isEqualTo(expectedEf);
    }

    @Test
    void compute_passQ5_efIncreases() {
        double initialEf = 2.0;
        Sm2Service.Sm2Result result = sm2Service.compute(90, 0, initialEf, 1);

        double expectedEf = Math.max(1.3, initialEf + 0.1 - 0.0);
        assertThat(result.ef()).isEqualTo(expectedEf);
        assertThat(result.ef()).isGreaterThan(initialEf);
    }
}
