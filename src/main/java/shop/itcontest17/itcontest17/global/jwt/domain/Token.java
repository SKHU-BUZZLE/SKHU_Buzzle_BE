package shop.itcontest17.itcontest17.global.jwt.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.itcontest17.itcontest17.member.domain.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tokenId")
    private Long tokenId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberId")
    private Member member;

    private String refreshToken;

    @Builder
    public Token(Member member, String refreshToken) {
        this.member = member;
        this.refreshToken = refreshToken;
    }

    public void refreshTokenUpdate(String refreshToken) {
        if (!this.refreshToken.equals(refreshToken)) {
            this.refreshToken = refreshToken;
        }
    }
}
