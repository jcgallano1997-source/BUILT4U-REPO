package com.built4u.pos.auth;

import com.built4u.pos.common.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);

                // Only ACCESS tokens may authenticate a request.
                String tokenType = claims.get("typ", String.class);
                if (tokenType != null && !"access".equals(tokenType)) {
                    throw new JwtException("Non-access token presented as bearer credential");
                }

                String username = claims.getSubject();

                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (roles != null) {
                    roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                }

                // Module grants. Wildcard ("*") expands to MOD_<every catalog
                // code> so hasAnyAuthority(...) matches without a DB hit.
                @SuppressWarnings("unchecked")
                List<String> modules = claims.get("modules", List.class);
                if (modules != null) {
                    if (modules.contains(Modules.WILDCARD)) {
                        Modules.ALL.forEach(m -> authorities.add(new SimpleGrantedAuthority("MOD_" + m)));
                    } else {
                        modules.forEach(m -> authorities.add(new SimpleGrantedAuthority("MOD_" + m)));
                    }
                }

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Site context (the single-business isolation key).
                Number siteId = claims.get("siteId", Number.class);
                if (siteId != null) {
                    TenantContext.setSiteId(siteId.longValue());
                }
                TenantContext.setSiteCode(claims.get("siteCode", String.class));
                TenantContext.setSiteName(claims.get("siteName", String.class));
            } catch (JwtException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Always clear ThreadLocals — request threads come from a pool.
            TenantContext.clear();
        }
    }
}
