package com.kaoyan.platform.ai.service;

import com.kaoyan.platform.ai.entity.AiUsage;
import com.kaoyan.platform.ai.mapper.AiUsageMapper;
import com.kaoyan.platform.ai.vo.AiQuotaVO;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** AI 每日配额：只在真正调用模型时扣减；缓存讲解和降级解析不扣。 */
@Service
public class AiQuotaService {

    private final AiUsageMapper usageMapper;
    private final Clock clock;
    private final int dailyQuota;

    public AiQuotaService(AiUsageMapper usageMapper, Clock clock,
                          @Value("${app.ai.daily-quota:20}") int dailyQuota) {
        this.usageMapper = usageMapper;
        this.clock = clock;
        this.dailyQuota = dailyQuota;
    }

    public boolean tryConsume(Long userId) {
        if (dailyQuota <= 0) {
            return false;
        }
        return usageMapper.tryConsume(userId, LocalDate.now(clock), dailyQuota) > 0;
    }

    public AiQuotaVO quota(Long userId) {
        LocalDate date = LocalDate.now(clock);
        AiUsage usage = usageMapper.selectByUserAndDate(userId, date);
        int used = usage == null || usage.getRequestCount() == null ? 0 : usage.getRequestCount();
        return new AiQuotaVO(date, dailyQuota, used, Math.max(0, dailyQuota - used));
    }

    public void addTokens(Long userId, int tokens) {
        if (tokens > 0) {
            usageMapper.addTokens(userId, LocalDate.now(clock), tokens);
        }
    }

    /** 模型调用失败、没有产出讲解时退款 */
    public void refund(Long userId) {
        usageMapper.refund(userId, LocalDate.now(clock));
    }

    public int getDailyQuota() {
        return dailyQuota;
    }
}
