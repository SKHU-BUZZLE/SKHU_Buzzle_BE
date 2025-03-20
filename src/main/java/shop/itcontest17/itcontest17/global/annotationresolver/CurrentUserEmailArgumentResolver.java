package shop.itcontest17.itcontest17.global.annotationresolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import shop.itcontest17.itcontest17.auth.api.dto.request.TokenReqDto;
import shop.itcontest17.itcontest17.global.annotation.CurrentUserEmail;
import shop.itcontest17.itcontest17.global.jwt.TokenProvider;

@Component
public class CurrentUserEmailArgumentResolver implements HandlerMethodArgumentResolver {

    private final TokenProvider tokenProvider;

    public CurrentUserEmailArgumentResolver(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentUserEmail.class) != null;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            TokenReqDto tokenReqDto = new TokenReqDto(token);

            return tokenProvider.getUserEmailFromToken(tokenReqDto);
        }

        return null;
    }
}