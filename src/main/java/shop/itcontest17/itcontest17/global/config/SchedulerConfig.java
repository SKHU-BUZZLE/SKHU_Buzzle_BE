package shop.itcontest17.itcontest17.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;

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