package shop.itcontest17.itcontest17.member.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.itcontest17.itcontest17.global.entity.BaseEntity;
import shop.itcontest17.itcontest17.global.entity.Status;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private Status status;

    private String email;

    private String name;

    private String nickname;

    private String picture;

    @Enumerated(value = EnumType.STRING)
    private SocialType socialType;

    private String introduction = "";

    private Integer streak = 0;

//    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Ranking ranking;

    @Builder
    private Member(Status status,
                   String email, String name,
                   String picture,
                   SocialType socialType,
                   String introduction
    ) {
        this.status = status;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.socialType = socialType;
        this.introduction = introduction;
        this.nickname = email.split("@")[0];
    }

    public void incrementStreak() {
        this.streak++;
    }
}