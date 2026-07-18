package es.brasatech.pinchito.service;

import es.brasatech.pinchito.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InMemoryStorageServiceTest {

    private StorageService backingStorage;
    private InMemoryStorageService inMemoryStorageService;

    @BeforeEach
    void setUp() {
        backingStorage = mock(StorageService.class);
        inMemoryStorageService = new InMemoryStorageService(backingStorage);
    }

    @Test
    void testInitPreloadsData() {
        when(backingStorage.listAccounts()).thenReturn(Arrays.asList("couple1", "couple2"));
        
        Account acc1 = new Account("couple1");
        Account acc2 = new Account("couple2");
        when(backingStorage.loadAccount("couple1")).thenReturn(acc1);
        when(backingStorage.loadAccount("couple2")).thenReturn(acc2);

        inMemoryStorageService.init();

        assertTrue(inMemoryStorageService.accountExists("couple1"));
        assertTrue(inMemoryStorageService.accountExists("couple2"));
        assertEquals(acc1, inMemoryStorageService.loadAccount("couple1"));
        assertEquals(acc2, inMemoryStorageService.loadAccount("couple2"));

        verify(backingStorage, times(1)).listAccounts();
        verify(backingStorage, times(1)).loadAccount("couple1");
        verify(backingStorage, times(1)).loadAccount("couple2");
    }

    @Test
    void testSaveWritesToCacheAndMarksDirty() throws Exception {
        when(backingStorage.listAccounts()).thenReturn(Collections.emptyList());
        inMemoryStorageService.init();

        Account newAccount = new Account("brand_new");
        inMemoryStorageService.saveAccount(newAccount);

        assertTrue(inMemoryStorageService.accountExists("brand_new"));
        assertEquals(newAccount, inMemoryStorageService.loadAccount("brand_new"));

        // Verify it is eventually saved to the backing storage in the background
        verify(backingStorage, timeout(3000).times(1)).saveAccount(newAccount);

        // Shutdown context using the destroy lifecycle method
        inMemoryStorageService.destroy();

        // Verify it was not written again (total invocation count remains 1)
        verify(backingStorage, times(1)).saveAccount(newAccount);
    }

    @Test
    void testShutdownWithoutChanges() throws Exception {
        when(backingStorage.listAccounts()).thenReturn(Collections.emptyList());
        inMemoryStorageService.init();

        inMemoryStorageService.destroy();

        verify(backingStorage, never()).saveAccount(any(Account.class));
    }
}
