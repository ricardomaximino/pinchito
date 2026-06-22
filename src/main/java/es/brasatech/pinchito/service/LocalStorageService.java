package es.brasatech.pinchito.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.brasatech.pinchito.model.Account;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalStorageService implements StorageService {

    private final ObjectMapper objectMapper;
    private final File storageDir;

    public LocalStorageService(ObjectMapper objectMapper, String storagePath) {
        this.objectMapper = objectMapper;
        this.storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    @Override
    public Account loadAccount(String accountName) {
        File file = getAccountFile(accountName);
        if (!file.exists()) {
            throw new RuntimeException("Account '" + accountName + "' not found.");
        }
        try {
            return objectMapper.readValue(file, Account.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read account: " + accountName, e);
        }
    }

    @Override
    public void saveAccount(Account account) {
        File file = getAccountFile(account.getAccountName());
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, account);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save account: " + account.getAccountName(), e);
        }
    }

    @Override
    public boolean accountExists(String accountName) {
        return getAccountFile(accountName).exists();
    }

    @Override
    public List<String> listAccounts() {
        File[] files = storageDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        List<String> accounts = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                accounts.add(name.substring(0, name.length() - 5));
            }
        }
        return accounts;
    }

    private File getAccountFile(String accountName) {
        String safeName = accountName.replaceAll("[^a-zA-Z0-9_\\-]", "");
        return new File(storageDir, safeName.toLowerCase() + ".json");
    }
}
