package com.chacha.multitenantsaas.security;

import java.util.Set;

public final class SystemRoleCodes {

    public static final String ADMIN = "ADMIN";

    public static final String MANAGER = "MANAGER";

    public static final String MEMBER = "MEMBER";

    public static final Set<String> ALL = Set.of(ADMIN, MANAGER, MEMBER);

    private SystemRoleCodes() {}
}
