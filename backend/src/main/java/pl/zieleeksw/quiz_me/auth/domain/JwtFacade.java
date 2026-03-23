package pl.zieleeksw.quiz_me.auth.domain;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class JwtFacade {

    private final JwtProperties jwtProperties;
    private final Key signingKey;

    JwtFacade(
            final JwtProperties jwtProperties
    ) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecretKey()));
    }

    String generateAccessToken(final String email) {
        return generateToken(new HashMap<>(), email, jwtProperties.getExpiration().getAccessTokenMs());
    }

    String generateRefreshToken(final String email) {
        return generateToken(new HashMap<>(), email, jwtProperties.getExpiration().getRefreshTokenMs());
    }

    boolean isTokenValid(
            final String token,
            final String email
    ) {
        return email.equals(extractEmail(token)) && !isTokenExpired(token);
    }

    String extractEmail(
            final String token
    ) {
        return extractClaim(token, Claims::getSubject);
    }

    private String generateToken(
            final Map<String, Object> claims,
            final String email,
            long validityMs
    ) {
        final Date now = new Date();
        claims.put("jti", java.util.UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private <T> T extractClaim(
            final String token,
            final Function<Claims, T> resolver
    ) {
        return resolver.apply(extractAllClaims(token));
    }

    private boolean isTokenExpired(
            final String token
    ) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(
            final String token
    ) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(
            final String token
    ) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}