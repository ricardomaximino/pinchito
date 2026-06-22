package es.brasatech.pinchito.service;

import es.brasatech.pinchito.model.Account;
import java.util.List;

public interface StorageService {
    Account loadAccount(String accountName);
    void saveAccount(Account account);
    boolean accountExists(String accountName);
    List<String> listAccounts();
}
