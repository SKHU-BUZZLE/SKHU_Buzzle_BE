package shop.itcontest17.itcontest17.member.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberLifeResDto;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public List<Member> getTop10MembersByStreak() {
        return memberRepository.findTop10ByStreak();
    }

    public MemberLifeResDto getMemberLife(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        return new MemberLifeResDto(member.getLife());
    }
}
