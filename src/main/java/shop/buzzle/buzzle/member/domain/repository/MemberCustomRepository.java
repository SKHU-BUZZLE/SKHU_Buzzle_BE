package shop.buzzle.buzzle.member.domain.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import shop.buzzle.buzzle.member.domain.Member;

public interface MemberCustomRepository {
    List<Member> findTop10ByStreak();
    Page<Member> findMembersByStreakWithPagination(Pageable pageable);
    Long findMemberRankingByStreak(Long memberId);
    long countAllMembers();
}