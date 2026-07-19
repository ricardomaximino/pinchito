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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InMemoryStorageService implements StorageService, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(InMemoryStorageService.class);

    private final StorageService backingStorage;
    private final Map<String, Account> cache = new ConcurrentHashMap<>();
    private final Set<String> dirtyAccounts = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        String storageType = (backingStorage instanceof GitHubStorageService) ? "GITHUB" : "LOCAL_DISK";
        log.info("Saved account '{}' in memory. Marked as dirty. Queuing asynchronous sync to {}.", key, storageType);

        executor.submit(() -> {
            try {
                log.info("Asynchronously syncing account '{}' to {}...", key, storageType);
                backingStorage.saveAccount(account);
                dirtyAccounts.remove(key);
                log.info("Asynchronous sync completed to {} for account '{}'.", storageType, key);
            } catch (org.springframework.web.client.RestClientResponseException e) {
                log.error("Failed to asynchronously sync account '{}' to {}! HTTP Status: {} {}, Response Body: {}", 
                          key, storageType, e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString(), e);
            } catch (Exception e) {
                log.error("Failed to asynchronously sync account '{}' to {}!", key, storageType, e);
            }
        });
    }

    @Override
    public boolean accountExists(String accountName) {
        String key = accountName.toLowerCase();
        if (cache.containsKey(key)) {
            return true;
        }
        // Fall back to checking backing storage to handle multi-instance environments or late registrations
        if (backingStorage.accountExists(accountName)) {
            try {
                log.info("Account '{}' exists in backing storage but not in cache. Pre-fetching it...", accountName);
                Account account = backingStorage.loadAccount(accountName);
                cache.put(key, account);
                return true;
            } catch (Exception e) {
                log.error("Failed to load account '{}' from backing storage after confirming existence!", accountName, e);
            }
        }
        return false;
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
        log.info("Shutdown signal received. Gracefully shutting down background sync executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Background sync executor did not terminate in 5s. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        shutdown();
    }

    public void shutdown() {
        String storageType = (backingStorage instanceof GitHubStorageService) ? "GITHUB" : "LOCAL_DISK";
        log.info("Shutdown signal received. Syncing dirty accounts to {}...", storageType);
        if (dirtyAccounts.isEmpty()) {
            log.info("No modifications detected. Exiting without backing sync.");
            return;
        }

        log.info("Found {} dirty accounts to sync to {}: {}", dirtyAccounts.size(), storageType, dirtyAccounts);
        for (String accountName : dirtyAccounts) {
            try {
                Account account = cache.get(accountName);
                if (account != null) {
                    log.info("Syncing account '{}' to {}...", accountName, storageType);
                    backingStorage.saveAccount(account);
                }
            } catch (org.springframework.web.client.RestClientResponseException e) {
                log.error("Failed to sync account '{}' to {} during shutdown! HTTP Status: {} {}, Response Body: {}", 
                          accountName, storageType, e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString(), e);
            } catch (Exception e) {
                log.error("Failed to sync account '{}' to {} during shutdown!", accountName, storageType, e);
            }
        }
        log.info("Sync completed. Exiting.");
    }
}
