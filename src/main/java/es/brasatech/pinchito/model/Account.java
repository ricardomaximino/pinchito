package es.brasatech.pinchito.model;

import java.util.ArrayList;
import java.util.List;

public class Account {
    private String accountName;
    private List<Member> members = new ArrayList<>();
    private List<Debit> debits = new ArrayList<>();

    public Account() {}

    public Account(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public List<Debit> getDebits() {
        return debits;
    }

    public void setDebits(List<Debit> debits) {
        this.debits = debits;
    }
}
