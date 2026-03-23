package pl.zieleeksw.quiz_me.auth;

import java.util.UUID;

public final class RandomEmailGenerator {

    private RandomEmailGenerator() {
        throw new IllegalStateException("RandomEmailGenerator utility class");
    }

    public static String generate() {
        final String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        return "user_" + uniquePart + "@testmail.com";
    }
}
