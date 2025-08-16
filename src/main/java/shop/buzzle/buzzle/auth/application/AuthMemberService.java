package shop.buzzle.buzzle.auth.application;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.buzzle.buzzle.auth.api.dto.response.MemberLoginResDto;
import shop.buzzle.buzzle.auth.api.dto.response.UserInfo;
import shop.buzzle.buzzle.auth.exception.EmailNotFoundException;
import shop.buzzle.buzzle.auth.exception.ExistsMemberEmailException;
import shop.buzzle.buzzle.global.entity.Status;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.SocialType;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthMemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberLoginResDto saveUserInfo(UserInfo userInfo, SocialType provider) {
        validateNotFoundEmail(userInfo.email());

        Member member = getExistingMemberOrCreateNew(userInfo, provider);

        validateSocialType(member, provider);

        return MemberLoginResDto.from(member);
    }

    private void validateNotFoundEmail(String email) {
        if (email == null) {
            throw new EmailNotFoundException();
        }
    }

    private Member getExistingMemberOrCreateNew(UserInfo userInfo, SocialType provider) {
        return memberRepository.findByEmail(userInfo.email()).orElseGet(() -> createMember(userInfo, provider));
        // email로 확인 : email 값이 있다면 get, 없으면 create
    }

    private Member createMember(UserInfo userInfo, SocialType provider) {
        String userPicture = getUserPicture(userInfo.picture());
        String name = unionName(userInfo.name(), userInfo.nickname());

        Member member = Member.builder()
                .status(Status.ACTIVE)
                .email(userInfo.email())
                .name(name)
                .picture(userPicture)
                .socialType(provider)
                .introduction("")
                .build();

        return memberRepository.save(member);
    }

    private String unionName(String name, String nickname) {
        return nickname != null ? nickname : name;
    }
    
    private String getUserPicture(String picture) {
        return Optional.ofNullable(picture)
                .map(this::convertToHighRes)
                .orElseThrow();
    }

    private String convertToHighRes(String url){
        return url.replace("s96-c", "s2048-c");
    }

    private void validateSocialType(Member member, SocialType provider) {
        if (!provider.equals(member.getSocialType())) {
            throw new ExistsMemberEmailException();
        }
    }
}
