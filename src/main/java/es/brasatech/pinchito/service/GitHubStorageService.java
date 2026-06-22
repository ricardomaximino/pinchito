package es.brasatech.pinchito.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.brasatech.pinchito.model.Account;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHubStorageService implements StorageService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String owner;
    private final String repo;
    private final String branch;

    public GitHubStorageService(ObjectMapper objectMapper, String owner, String repo, String branch, String token) {
        this.objectMapper = objectMapper;
        this.owner = owner;
        this.repo = repo;
        this.branch = branch;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }

    @Override
    public Account loadAccount(String accountName) {
        String path = getAccountFilePath(accountName);
        try {
            GitHubContentResponse response = restClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}?ref={branch}", owner, repo, path, branch)
                    .retrieve()
                    .body(GitHubContentResponse.class);

            if (response == null || response.getContent() == null) {
                throw new RuntimeException("Failed to read account " + accountName + " from GitHub: empty response");
            }

            String cleanBase64 = response.getContent().replaceAll("\\s", "");
            byte[] decodedBytes = Base64.getDecoder().decode(cleanBase64);
            String jsonContent = new String(decodedBytes, StandardCharsets.UTF_8);

            return objectMapper.readValue(jsonContent, Account.class);
        } catch (Exception e) {
            throw new RuntimeException("Account '" + accountName + "' could not be loaded from GitHub.", e);
        }
    }

    @Override
    public void saveAccount(Account account) {
        String accountName = account.getAccountName();
        String path = getAccountFilePath(accountName);
        try {
            String currentSha = null;
            try {
                GitHubContentResponse response = restClient.get()
                        .uri("/repos/{owner}/{repo}/contents/{path}?ref={branch}", owner, repo, path, branch)
                        .retrieve()
                        .body(GitHubContentResponse.class);
                if (response != null) {
                    currentSha = response.getSha();
                }
            } catch (Exception e) {
                // File does not exist yet or request failed
            }

            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(account);
            String base64Content = Base64.getEncoder().encodeToString(jsonContent.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = new HashMap<>();
            body.put("message", "Save account: " + accountName);
            body.put("content", base64Content);
            body.put("branch", branch);
            if (currentSha != null) {
                body.put("sha", currentSha);
            }

            restClient.put()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save account '" + accountName + "' to GitHub.", e);
        }
    }

    @Override
    public boolean accountExists(String accountName) {
        String path = getAccountFilePath(accountName);
        try {
            HttpStatusCode status = restClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}?ref={branch}", owner, repo, path, branch)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            return status.is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> listAccounts() {
        List<String> accounts = new ArrayList<>();
        try {
            List<Map> items = restClient.get()
                    .uri("/repos/{owner}/{repo}/contents/accounts?ref={branch}", owner, repo, branch)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<Map>>() {});
            if (items != null) {
                for (Map item : items) {
                    String name = (String) item.get("name");
                    if (name != null && name.toLowerCase().endsWith(".json")) {
                        accounts.add(name.substring(0, name.length() - 5));
                    }
                }
            }
        } catch (Exception e) {
            // Folder doesn't exist or request failed
        }
        return accounts;
    }

    private String getAccountFilePath(String accountName) {
        String safeName = accountName.replaceAll("[^a-zA-Z0-9_\\-]", "");
        return "accounts/" + safeName.toLowerCase() + ".json";
    }

    public static class GitHubContentResponse {
        private String name;
        private String path;
        private String sha;
        private String content;
        private String encoding;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getEncoding() { return encoding; }
        public void setEncoding(String encoding) { this.encoding = encoding; }
    }
}
