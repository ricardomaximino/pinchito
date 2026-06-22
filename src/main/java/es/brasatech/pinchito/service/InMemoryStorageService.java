package es.brasatech.pinchito.service;

import es.brasatech.pinchito.model.Account;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStorageService implements StorageService, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(InMemoryStorageService.class);

    private final StorageService backingStorage;
    private final Map<String, Account> cache = new ConcurrentHashMap<>();
    private final Set<String> dirtyAccounts = ConcurrentHashMap.newKeySet();

    public InMemoryStorageService(StorageService backingStorage) {
        this.backingStorage = backingStorage;
    }

    @Override
    public Account loadAccount(String accountName) {
        String key = accountName.toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        log.warn("Account '{}' not found in cache. Loading from backing storage.", accountName);
        Account account = backingStorage.loadAccount(accountName);
        cache.put(key, account);
        return account;
    }

    @Override
    public void saveAccount(Account account) {
        String key = account.getAccountName().toLowerCase();
        cache.put(key, account);
        dirtyAccounts.add(key);
        log.info("Saved account '{}' in memory. Marked as dirty.", key);
    }

    @Override
    public boolean accountExists(String accountName) {
        return cache.containsKey(accountName.toLowerCase());
    }

    @Override
    public List<String> listAccounts() {
        return new ArrayList<>(cache.keySet());
    }

    @PostConstruct
    public void init() {
        log.info("Initializing InMemoryStorageService. Pre-loading all data from backing storage...");
        try {
            List<String> accountNames = backingStorage.listAccounts();
            log.info("Found {} accounts to load: {}", accountNames.size(), accountNames);
            for (String name : accountNames) {
                try {
                    Account account = backingStorage.loadAccount(name);
                    cache.put(name.toLowerCase(), account);
                    log.info("Loaded account '{}' into memory", name);
                } catch (Exception e) {
                    log.error("Failed to load account '{}' at startup", name, e);
                }
            }
            log.info("Pre-loading finished. Loaded {} accounts.", cache.size());
        } catch (Exception e) {
            log.error("Failed to list accounts from backing storage during initialization", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }

    public void shutdown() {
        log.info("Shutdown signal received. Syncing dirty accounts to backing storage...");
        if (dirtyAccounts.isEmpty()) {
            log.info("No modifications detected. Exiting without backing sync.");
            return;
        }

        log.info("Found {} dirty accounts to sync: {}", dirtyAccounts.size(), dirtyAccounts);
        for (String accountName : dirtyAccounts) {
            try {
                Account account = cache.get(accountName);
                if (account != null) {
                    log.info("Syncing account '{}' to backing storage...", accountName);
                    backingStorage.saveAccount(account);
                }
            } catch (Exception e) {
                log.error("Failed to sync account '{}' during shutdown!", accountName, e);
            }
        }
        log.info("Sync completed. Exiting.");
    }
}
