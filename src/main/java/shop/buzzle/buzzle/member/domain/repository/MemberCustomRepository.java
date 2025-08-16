package shop.buzzle.buzzle.member.domain.repository;

import java.util.List;
import shop.buzzle.buzzle.member.domain.Member;

public interface MemberCustomRepository {
    List<Member> findTop10ByStreak();
}