package pl.zieleeksw.quiz_me.auth;

import java.security.SecureRandom;

public class RandomPasswordGenerator {

    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int DEFAULT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomPasswordGenerator() {
        throw new IllegalStateException("RandomPasswordGenerator utility class");
    }

    public static String generate() {
        final StringBuilder sb = new StringBuilder(DEFAULT_LENGTH);
        for (int i = 0; i < DEFAULT_LENGTH; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

}
