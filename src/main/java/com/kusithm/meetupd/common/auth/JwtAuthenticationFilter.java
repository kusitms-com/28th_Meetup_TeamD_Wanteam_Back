package com.kusithm.meetupd.common.auth;

import com.kusithm.meetupd.common.error.ErrorCode;
import com.kusithm.meetupd.common.error.ForbiddenException;
import com.kusithm.meetupd.common.jwt.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;


    // TODO api 추가될 때 white list url 확인해서 추가하기.
    private static final List<RequestMatcher> whiteUrlMatchers = Arrays.asList(

            // 보안 관련 URI
            new AntPathRequestMatcher("/api/auth/register"),
            new AntPathRequestMatcher("/api/auth/login"),
            new AntPathRequestMatcher("/api/auth/reissue"),

            // 유저 관련 URI
            new AntPathRequestMatcher("/api/users/profiles/*"),

            // 팀 관련 URI
            new AntPathRequestMatcher(("/api/teams/recruiting")),
            new AntPathRequestMatcher(("/api/teams/contest/*")),

            // 공모전 관련 URI
            new AntPathRequestMatcher("/api/contests/search"),
            new AntPathRequestMatcher("/api/contests/categories"),
            new AntPathRequestMatcher("/api/contests/detail"),
            new AntPathRequestMatcher("/api/contests/main-recommendation"),

            // 추천사 관련 URI
            new AntPathRequestMatcher("/api/reviews/non-user"),
            new AntPathRequestMatcher("/api/reviews/non-user/check/*"),

            // 기타 URI
            new AntPathRequestMatcher("/"),
            new AntPathRequestMatcher("/api/health"),
            new AntPathRequestMatcher("/api/s3/upload"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-ui/**")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // white list url의 경우 jwt 토큰 인증 절차 무시
        for (RequestMatcher requestMatcher : whiteUrlMatchers) {
            if (requestMatcher.matches(request)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String accessToken = getAccessTokenFromHttpServletRequest(request);
        tokenProvider.validateAccessToken(accessToken);
        Long userId = tokenProvider.getTokenSubject(accessToken);
        setAuthentication(request, userId);
        filterChain.doFilter(request, response);
    }


    private String getAccessTokenFromHttpServletRequest(HttpServletRequest request) {
        String accessToken = request.getHeader(AuthConstants.AUTH_HEADER);
        if (StringUtils.hasText(accessToken) && accessToken.startsWith(AuthConstants.TOKEN_TYPE)) {
            return accessToken.split(" ")[1];
        }
        throw new ForbiddenException(ErrorCode.UNAUTHORIZED);
    }


    private void setAuthentication(HttpServletRequest request, Long userId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, null);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
