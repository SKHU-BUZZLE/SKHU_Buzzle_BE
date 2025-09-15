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

    @Override
    public Long findMemberRankingByStreak(Long memberId) {
        QMember member = QMember.member;
        QMember targetMember = new QMember("targetMember");

        return queryFactory
                .select(member.id.count().add(1))
                .from(member)
                .join(targetMember).on(targetMember.id.eq(memberId))
                .where(member.streak.gt(targetMember.streak))
                .fetchOne();
    }
}
