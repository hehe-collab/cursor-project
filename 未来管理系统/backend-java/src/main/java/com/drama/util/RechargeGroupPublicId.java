package com.drama.util;

import java.util.concurrent.ThreadLocalRandom;

public final class RechargeGroupPublicId {

    private static final String CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private RechargeGroupPublicId() {}

    /** 与 Node `generateRechargeGroupPublicId` 一致：RG_ + 10 位随机 */
    public static String generate() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder("RG_");
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(r.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
