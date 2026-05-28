package cn.ai.sales.module.agent.service.gewe;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GeweMessageTimeNormalizerTest {

    @Test
    void fromEpoch_shouldAcceptSecondMillisecondAndMicrosecondValues() {
        assertThat(GeweMessageTimeNormalizer.fromEpoch(1_716_330_000L))
                .isEqualTo(LocalDateTime.of(2024, 5, 22, 6, 20));
        assertThat(GeweMessageTimeNormalizer.fromEpoch(1_716_330_000_000L))
                .isEqualTo(LocalDateTime.of(2024, 5, 22, 6, 20));
        assertThat(GeweMessageTimeNormalizer.fromEpoch(1_716_330_000_000_000L))
                .isEqualTo(LocalDateTime.of(2024, 5, 22, 6, 20));
    }

    @Test
    void normalize_shouldRepairPersistedFutureYear() {
        LocalDateTime corrupted = LocalDateTime.of(56_363, 9, 22, 8, 10, 38);

        LocalDateTime normalized = GeweMessageTimeNormalizer.normalize(corrupted);

        assertThat(normalized.getYear()).isLessThan(2100);
    }

}
