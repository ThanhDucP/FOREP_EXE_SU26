package com.forep.exe.security;

import com.forep.exe.domain.Enums.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID workspaceId, Role role, String email) {
}
