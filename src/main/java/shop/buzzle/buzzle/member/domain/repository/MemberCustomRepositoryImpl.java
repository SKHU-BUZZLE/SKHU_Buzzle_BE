package shop.buzzle.buzzle.member.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.QMember;

@RequiredArgsConstructor
public class MemberCustomRepositoryImpl implements MemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    // 랭킹 조회
    @Override
    public List<Member> findTop10ByStreak() {
        QMember member = QMember.member;

        return queryFactory
                .selectFrom(member)
                .orderBy(member.streak.desc())
                .limit(10)
                .fetch();
    }
}
