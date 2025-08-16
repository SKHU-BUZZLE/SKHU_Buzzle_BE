package shop.buzzle.buzzle.global.jwt.domain.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shop.buzzle.buzzle.global.jwt.domain.Token;
import shop.buzzle.buzzle.member.domain.Member;

public interface TokenRepository extends JpaRepository<Token, Long> {
    boolean existsByMember(Member member);
    Optional<Token> findByMember(Member member);
    boolean existsByRefreshToken(String refreshToken);
    Optional<Token> findByRefreshToken(String refreshToken);
}