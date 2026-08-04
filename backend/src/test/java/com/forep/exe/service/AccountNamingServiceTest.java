package com.forep.exe.service;

import com.forep.exe.domain.Enums.AccountType;
import com.forep.exe.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountNamingServiceTest {
    @Test
    void normalizesVietnameseNamesForHrAccounts() {
        UserRepository users = mock(UserRepository.class);
        AccountNamingService service = new AccountNamingService(users);

        assertEquals("hr.forep.nguyenvanan",
                service.generateUniqueUsername(AccountType.HR, "Nguyễn Văn An", "FOREP"));
    }

    @Test
    void appendsBoundedSequenceWhenUsernameExists() {
        UserRepository users = mock(UserRepository.class);
        Set<String> existing = Set.of("owner.forep", "owner.forep2");
        when(users.existsByUsernameIgnoreCase(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> existing.contains(invocation.getArgument(0)));
        AccountNamingService service = new AccountNamingService(users);

        assertEquals("owner.forep3",
                service.generateUniqueUsername(AccountType.BUSINESS_OWNER, "FOREP Owner", "FOREP"));
    }

    @Test
    void employeeUsesTheSameNormalizerAndWorkspaceAwareFormat() {
        UserRepository users = mock(UserRepository.class);
        AccountNamingService service = new AccountNamingService(users);

        assertEquals("emp.forep.tranthibich",
                service.generateUniqueUsername(AccountType.EMPLOYEE, "Trần Thị Bích", "FOREP"));
    }
}
