package com.forep.exe.service;

import com.forep.exe.domain.Enums.AccountType;
import com.forep.exe.persistence.UserRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class AccountNamingService {
    private static final int MAX_USERNAME_LENGTH = 80;
    private static final int MAX_SUFFIX_ATTEMPTS = 10_000;

    private final UserRepository users;

    public AccountNamingService(UserRepository users) {
        this.users = users;
    }

    public String generateUniqueUsername(AccountType accountType, String fullName, String workspaceCode) {
        String normalizedName = normalizeSegment(fullName);
        String normalizedWorkspace = normalizeSegment(workspaceCode);
        String base = switch (accountType) {
            case BUSINESS_OWNER -> "owner." + nonBlank(normalizedWorkspace, normalizedName, "workspace");
            case HR -> "hr." + nonBlank(normalizedWorkspace, "workspace") + "." + nonBlank(normalizedName, "user");
            case EMPLOYEE -> "emp." + nonBlank(normalizedWorkspace, "workspace") + "." + nonBlank(normalizedName, "user");
        };
        base = trimToLength(base, MAX_USERNAME_LENGTH);
        for (int sequence = 1; sequence <= MAX_SUFFIX_ATTEMPTS; sequence++) {
            String suffix = sequence == 1 ? "" : Integer.toString(sequence);
            String candidate = trimToLength(base, MAX_USERNAME_LENGTH - suffix.length()) + suffix;
            if (!users.existsByUsernameIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Could not allocate a unique username.");
    }

    String normalizeSegment(String value) {
        String source = (value == null ? "" : value)
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .replace('\u00f0', 'd')
                .replace('\u00d0', 'D');
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return trimToLength(normalized, 48);
    }

    private String nonBlank(String first, String... fallbacks) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        for (String fallback : fallbacks) {
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
        }
        throw new IllegalArgumentException("Username source must not be empty.");
    }

    private String trimToLength(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
