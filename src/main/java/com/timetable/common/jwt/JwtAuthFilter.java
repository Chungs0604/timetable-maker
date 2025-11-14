package com.timetable.common.jwt;

import com.timetable.user.entity.User;
import com.timetable.user.repo.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Jws<Claims> jws = jwtService.parse(token);
                Long userId = Long.valueOf(jws.getBody().getSubject());
                String email = jws.getBody().get("email", String.class);

                User u = userRepository.findById(userId).orElse(null);
                if (u != null && u.getEmail().equals(email)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            u.getId(), null, List.of()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
                // 토큰이 잘못됐거나 만료된 경우 → 인증 없이 통과
            }
        }
        filterChain.doFilter(request, response);
    }
}
