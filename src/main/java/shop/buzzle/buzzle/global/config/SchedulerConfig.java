package shop.buzzle.buzzle.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {

    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 (00:00)
    public void resetLife() {
        memberRepository.resetLife();
    }
}