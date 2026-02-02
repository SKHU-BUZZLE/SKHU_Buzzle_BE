package shop.buzzle.buzzle.member.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import shop.buzzle.buzzle.global.dto.PageInfoResDto;
import shop.buzzle.buzzle.member.api.dto.response.FullRankingMemberResDto;
import shop.buzzle.buzzle.member.api.dto.response.FullTopRankingResDto;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRankingService {

    private static final String RANKING_KEY = "ranking:streak";

    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    /**
     * 시작 시 자동으로 Redis 랭킹 초기화
     */
    @PostConstruct
    public void init() {
        try {
            Long size = redisTemplate.opsForZSet().zCard(RANKING_KEY);
            if (size == null || size == 0) {
                log.info("Redis 랭킹 데이터가 비어있음. 초기화 시작...");
                initializeRanking();
                log.info("Redis 랭킹 초기화 완료. 총 {}명 적재", redisTemplate.opsForZSet().zCard(RANKING_KEY));
            } else {
                log.info("Redis 랭킹 데이터 존재. 초기화 스킵 ({}명)", size);
            }
        } catch (Exception e) {
            log.warn("Redis 연결 실패. 랭킹 초기화 스킵: {}", e.getMessage());
        }
    }

    /**
     * Redis에 전체 회원 랭킹 초기화 (최초 1회 또는 동기화용)
     */
    public void initializeRanking() {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // 기존 데이터 삭제
        redisTemplate.delete(RANKING_KEY);

        // 모든 회원 데이터를 Redis에 적재
        List<Member> allMembers = memberRepository.findAll();
        for (Member member : allMembers) {
            zSetOps.add(RANKING_KEY, String.valueOf(member.getId()), member.getStreak());
        }
    }

    /**
     * 상위 랭킹 조회 + 내 랭킹 Redis Sorted Set
     * - ZREVRANGE: 상위 N명 조회
     * - ZREVRANK: 내 랭킹 조회
     */
    public FullTopRankingResDto getTopRankingFromRedis(int page, int size, String currentUserEmail) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        int start = page * size;
        int end = start + size - 1;

        // 상위 랭킹 조회
        Set<ZSetOperations.TypedTuple<String>> topMembers =
                zSetOps.reverseRangeWithScores(RANKING_KEY, start, end);

        List<FullRankingMemberResDto> rankings = new ArrayList<>();
        int rank = start + 1;

        if (topMembers != null) {
            for (ZSetOperations.TypedTuple<String> tuple : topMembers) {
                Long memberId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
                Member member = memberRepository.findById(memberId).orElse(null);

                if (member != null) {
                    rankings.add(FullRankingMemberResDto.from(member, rank));
                }
                rank++;
            }
        }

        // 전체 회원 수
        Long totalMembers = zSetOps.zCard(RANKING_KEY);
        if (totalMembers == null) totalMembers = 0L;

        // 현재 사용자 랭킹 조회
        Member currentMember = memberRepository.findByEmail(currentUserEmail)
                .orElseThrow(MemberNotFoundException::new);

        Long myRank = zSetOps.reverseRank(RANKING_KEY, String.valueOf(currentMember.getId()));
        int myRankInt = myRank != null ? myRank.intValue() + 1 : 1;

        FullRankingMemberResDto currentUser = FullRankingMemberResDto.from(currentMember, myRankInt);

        PageInfoResDto pageInfo = PageInfoResDto.builder()
                .currentPage(page)
                .totalPages((int) Math.ceil((double) totalMembers / size))
                .totalItems(totalMembers)
                .build();

        return FullTopRankingResDto.of(pageInfo, rankings, totalMembers, currentUser);
    }
}
