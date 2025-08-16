package shop.buzzle.buzzle.member.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import shop.buzzle.buzzle.member.api.dto.request.AccessTokenRequest;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberProfileUpdaterService {

    private final MemberRepository memberRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String kakaoUserInfoUrl= "https://kapi.kakao.com/v2/user/me";


    @Transactional
    public Void updateProfileImage(String email, AccessTokenRequest accessToken) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        // 카카오 API 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.accessToken());
        headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                kakaoUserInfoUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode root = objectMapper.readTree(response.getBody());
                String newImageUrl = root
                        .path("properties")
                        .path("profile_image")
                        .asText();

                member.updatePicture(newImageUrl);
            } catch (Exception e) {
                throw new RuntimeException("카카오 응답 파싱 실패", e);
            }
        } else {
            throw new RuntimeException("카카오 프로필 조회 실패: " + response.getStatusCode());
        }
        log.info("Access token = '{}'", accessToken.accessToken());

        return null;
    }
}
