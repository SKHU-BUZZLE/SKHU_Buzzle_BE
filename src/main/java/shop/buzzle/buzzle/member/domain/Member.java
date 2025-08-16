package shop.buzzle.buzzle.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.buzzle.buzzle.global.entity.BaseEntity;
import shop.buzzle.buzzle.global.entity.Status;

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

    private Integer life = 50;

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

    public void incrementStreak(int score) {
        this.streak += score;
    }

    public void decrementLife() {
        this.life--;
    }

    public void updatePicture(String newPictureUrl) {
        this.picture = newPictureUrl;
    }
}