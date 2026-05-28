package cn.ai.sales.module.agent.service.gewe;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class GeweMessageTimeNormalizer {

    private static final ZoneId WECHAT_MESSAGE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long MAX_REASONABLE_EPOCH_SECOND = 4_102_444_800L; // 2100-01-01 00:00:00 UTC

    private GeweMessageTimeNormalizer() {
    }

    public static LocalDateTime fromEpoch(Long rawCreateTime) {
        if (rawCreateTime == null || rawCreateTime <= 0) {
            return LocalDateTime.now();
        }
        long epochSecond = normalizeEpochSecond(rawCreateTime);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), WECHAT_MESSAGE_ZONE);
    }

    public static LocalDateTime normalize(LocalDateTime time) {
        if (time == null || time.getYear() <= 2100) {
            return time;
        }
        long epochSecond = time.atZone(WECHAT_MESSAGE_ZONE).toEpochSecond();
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(normalizeEpochSecond(epochSecond)), WECHAT_MESSAGE_ZONE);
    }

    private static long normalizeEpochSecond(long value) {
        long normalized = value;
        while (normalized > MAX_REASONABLE_EPOCH_SECOND) {
            normalized = normalized / 1000;
        }
        return normalized;
    }

}
