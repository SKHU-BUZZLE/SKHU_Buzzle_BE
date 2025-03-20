package shop.itcontest17.itcontest17.member.domain.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import shop.itcontest17.itcontest17.member.domain.Member;

public interface MemberRepository extends
        JpaRepository<Member, Long>,
        JpaSpecificationExecutor<Member>,
        MemberCustomRepository {
    Optional<Member> findByEmail(String email);
}