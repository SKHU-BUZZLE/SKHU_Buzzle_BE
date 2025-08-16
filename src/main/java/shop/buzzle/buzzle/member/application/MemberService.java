package shop.buzzle.buzzle.member.application;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoListDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoResDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberLifeResDto;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;

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
