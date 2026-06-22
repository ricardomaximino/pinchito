package es.brasatech.pinchito.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.brasatech.pinchito.model.Account;
import es.brasatech.pinchito.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    private LocalStorageService localStorageService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(@TempDir File tempDir) {
        objectMapper = new ObjectMapper();
        localStorageService = new LocalStorageService(objectMapper, tempDir.getAbsolutePath());
    }

    @Test
    void testSaveAndLoadAccount() {
        Account account = new Account("test_couple");
        Member member1 = new Member("Alice", "hashedpass1");
        Member member2 = new Member("Bob", "hashedpass2");
        account.getMembers().add(member1);
        account.getMembers().add(member2);

        localStorageService.saveAccount(account);

        assertTrue(localStorageService.accountExists("test_couple"));
        assertTrue(localStorageService.accountExists("TEST_COUPLE"));

        Account loaded = localStorageService.loadAccount("test_couple");
        assertEquals("test_couple", loaded.getAccountName());
        assertEquals(2, loaded.getMembers().size());
        assertEquals("Alice", loaded.getMembers().get(0).getUsername());
        assertEquals("hashedpass1", loaded.getMembers().get(0).getPassword());
    }

    @Test
    void testAccountNotFound() {
        assertFalse(localStorageService.accountExists("non_existent"));
        assertThrows(RuntimeException.class, () -> localStorageService.loadAccount("non_existent"));
    }

    @Test
    void testListAccounts() {
        assertTrue(localStorageService.listAccounts().isEmpty());

        Account account1 = new Account("couple_one");
        Account account2 = new Account("couple_two");
        localStorageService.saveAccount(account1);
        localStorageService.saveAccount(account2);

        java.util.List<String> list = localStorageService.listAccounts();
        assertEquals(2, list.size());
        assertTrue(list.contains("couple_one"));
        assertTrue(list.contains("couple_two"));
    }
}
