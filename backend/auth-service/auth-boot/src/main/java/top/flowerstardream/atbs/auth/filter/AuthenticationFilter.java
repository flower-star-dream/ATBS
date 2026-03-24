package top.flowerstardream.atbs.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.flowerstardream.atbs.tools.constants.ClientType;
import top.flowerstardream.atbs.tools.constants.JwtClaimsConstant;
import top.flowerstardream.base.context.RequestContext;
import top.flowerstardream.base.utils.TtlContextHolder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.tools.constants.CommonConstant.ROLE;
import static top.flowerstardream.atbs.tools.constants.JwtClaimsConstant.ROLES;
import static top.flowerstardream.base.utils.GetInfoUtil.*;
import static top.flowerstardream.base.utils.GetInfoUtil.getOperatorId;

/**
 * @Author: 花海
 * @Date: 2026/03/15/17:05
 * @Description: 认证过滤器
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 从TTL上下文获取（由TtlContextInterceptor解析网关传来的Header）
        RequestContext ctx = TtlContextHolder.get();

        if (ctx != null && getOperatorId() != null) {
            // 构建Spring Security认证对象
            Long userId = getOperatorId();
            String username = getOperatorName();
            List<String> roles = getExtra(ROLES, List.class);


            Collection<SimpleGrantedAuthority> authorities = roles != null ? roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE + role))
                .collect(Collectors.toList()) : List.of();

            var authentication = new UsernamePasswordAuthenticationToken(
                new UserDetails(userId, username, getExtra(JwtClaimsConstant.CLIENT_TYPE, ClientType.class), roles),
                null,
                authorities
            );
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    public record UserDetails(Long userId, String username, ClientType clientType, List<String> roles) { }
}