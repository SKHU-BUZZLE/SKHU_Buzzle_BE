package shop.itcontest17.itcontest17.member.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberCustomRepositoryImpl implements MemberCustomRepository {

    private final JPAQueryFactory queryFactory;

//    @Override
//    public boolean existsByNicknameAndNotId(String nickname, Long id) {
//        Integer count = queryFactory
//                .selectOne()
//                .from(member)
//                .where(member.nickname.eq(nickname)
//                        .and(member.id.ne(id)))
//                .fetchFirst();
//        return count != null;
//    }
}
