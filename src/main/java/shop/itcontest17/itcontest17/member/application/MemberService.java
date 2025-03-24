package shop.itcontest17.itcontest17.member.application;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberInfoListDto;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberInfoResDto;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberLifeResDto;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberInfoListDto getTop10MembersByStreak() {
        List<MemberInfoResDto> memberInfoList = memberRepository.findTop10ByStreak()
                .stream()
                .map(MemberInfoResDto::from)
                .collect(Collectors.toList());

        return MemberInfoListDto.from(memberInfoList);
    }

    public MemberLifeResDto getMemberLife(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        return new MemberLifeResDto(member.getLife());
    }

    public MemberInfoResDto getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        return MemberInfoResDto.from(member);
    }
}
