package shop.buzzle.buzzle.member.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public Page<Member> findMembersByStreakWithPagination(Pageable pageable) {
        QMember member = QMember.member;

        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.streak.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

        return new PageImpl<>(members, pageable, total);
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

    @Override
    public long countAllMembers() {
        QMember member = QMember.member;

        return queryFactory
                .selectFrom(member)
                .fetchCount();
    }
}
