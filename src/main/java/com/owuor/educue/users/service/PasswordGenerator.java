package com.owuor.educue.users.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGenerator {

    private static final String CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate(int length) {

        StringBuilder password = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            password.append(
                    CHARS.charAt(
                            RANDOM.nextInt(CHARS.length())
                    )
            );
        }

        return password.toString();
    }
}
