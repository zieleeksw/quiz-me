package pl.zieleeksw.quiz_me.auth.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
class JwtProperties {

    private String secretKey;

    private Expiration expiration = new Expiration();

    String getSecretKey() {
        return secretKey;
    }

    void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    Expiration getExpiration() {
        return expiration;
    }

    void setExpiration(Expiration expiration) {
        this.expiration = expiration;
    }

    static class Expiration {
        private long accessTokenMs;
        private long refreshTokenMs;

        long getAccessTokenMs() {
            return accessTokenMs;
        }

        void setAccessTokenMs(long accessTokenMs) {
            this.accessTokenMs = accessTokenMs;
        }

        long getRefreshTokenMs() {
            return refreshTokenMs;
        }

        void setRefreshTokenMs(long refreshTokenMs) {
            this.refreshTokenMs = refreshTokenMs;
        }
    }
}
