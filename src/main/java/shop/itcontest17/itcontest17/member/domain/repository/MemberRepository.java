package shop.itcontest17.itcontest17.member.domain.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import shop.itcontest17.itcontest17.member.domain.Member;

public interface MemberRepository extends
        JpaRepository<Member, Long>,
        JpaSpecificationExecutor<Member>,
        MemberCustomRepository {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByName(String name);

    @Modifying
    @Transactional
    @Query("UPDATE Member m SET m.life = 50")
    void resetLife();
}