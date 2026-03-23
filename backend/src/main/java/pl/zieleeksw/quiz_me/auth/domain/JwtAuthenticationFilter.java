package pl.zieleeksw.quiz_me.auth.domain;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String JWT_PREFIX = "Bearer ";

    private final JwtFacade jwtFacade;

    private final UserDetailsService userDetailsService;

    JwtAuthenticationFilter(final JwtFacade jwtFacade,
                            final UserDetailsService userDetailsService) {
        this.jwtFacade = jwtFacade;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (isAuthHeaderMissingOrMalformed(authHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(JWT_PREFIX.length());
            final String email = jwtFacade.extractEmail(jwt);

            if (isEmailMissingOrUserAuthenticated(email)) {
                filterChain.doFilter(request, response);
                return;
            }

            final UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!jwtFacade.isTokenValid(jwt, userDetails.getUsername())) {
                filterChain.doFilter(request, response);
                return;
            }

            final UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
        } catch (final Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean isAuthHeaderMissingOrMalformed(final String authHeader) {
        return Objects.isNull(authHeader) || !authHeader.startsWith(JWT_PREFIX);
    }

    private boolean isEmailMissingOrUserAuthenticated(final String email) {
        return Objects.isNull(email) || SecurityContextHolder.getContext().getAuthentication() != null;
    }
}
