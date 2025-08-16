package shop.buzzle.buzzle.global.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import shop.buzzle.buzzle.auth.api.dto.response.IdTokenAndAccessTokenResponse;
import shop.buzzle.buzzle.auth.api.dto.response.UserInfo;
import shop.buzzle.buzzle.auth.application.AuthService;
import shop.buzzle.buzzle.global.oauth.exception.OAuthException;
import shop.buzzle.buzzle.member.domain.SocialType;

@Slf4j
@Service
@Transactional(readOnly = true)
public class KakaoAuthService implements AuthService {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String JWT_DELIMITER = "\\.";
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    @Value("${oauth.kakao.rest-api-key}")
    private String restApiKey;
    @Value("${oauth.kakao.redirect-url}")
    private String redirectUri;

    public KakaoAuthService(ObjectMapper objectMapper, RestTemplate restTemplate) {
        // 생성자
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public IdTokenAndAccessTokenResponse getIdToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", restApiKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                KAKAO_TOKEN_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return new IdTokenAndAccessTokenResponse(
                        jsonNode.get("access_token").asText(),
                        jsonNode.get("id_token").asText()
                );
            } catch (Exception e) {
                throw new RuntimeException("카카오 토큰 파싱 실패", e);
            }
        }

        throw new RuntimeException("카카오 토큰 요청 실패");
    }


    @Override
    public String getProvider() {
        return String.valueOf(SocialType.KAKAO).toLowerCase();
    }

    @Transactional
    @Override
    public UserInfo getUserInfo(String idToken) {
        String decodePayload = getDecodePayload(idToken);

        try {
            return objectMapper.readValue(decodePayload, UserInfo.class);
        } catch (JsonProcessingException e) {
            throw new OAuthException("id 토큰을 읽을 수 없습니다.");
        }
    }

    private String getDecodePayload(String idToken) {
        String payload = getPayload(idToken);

        return new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
    }

    private String getPayload(String idToken) {
        return idToken.split(JWT_DELIMITER)[1];
    }
}