package shop.itcontest17.itcontest17.member.domain.repository;

import java.util.List;
import shop.itcontest17.itcontest17.member.domain.Member;

public interface MemberCustomRepository {
    List<Member> findTop10ByStreak();
}