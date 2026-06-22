package es.brasatech.pinchito.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.brasatech.pinchito.service.GitHubStorageService;
import es.brasatech.pinchito.service.InMemoryStorageService;
import es.brasatech.pinchito.service.LocalStorageService;
import es.brasatech.pinchito.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${github.token:}")
    private String githubToken;

    @Value("${github.owner:ricardomaximino}")
    private String githubOwner;

    @Value("${github.repo:pichito-data}")
    private String githubRepo;

    @Value("${github.branch:main}")
    private String githubBranch;

    @Value("${pinchito.local.storage-path:${user.home}/.pinchito/accounts}")
    private String localStoragePath;

    @Bean
    public StorageService storageService() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        StorageService backingStorage;
        if (githubToken != null && !githubToken.trim().isEmpty()) {
            log.info("Configuring GitHub Storage Service targeting repository: {}/{}", githubOwner, githubRepo);
            backingStorage = new GitHubStorageService(objectMapper, githubOwner, githubRepo, githubBranch, githubToken);
        } else {
            java.io.File storageDir = new java.io.File(localStoragePath);
            log.info("Configuring Local File Storage Service targeting directory: {}", storageDir.getAbsolutePath());
            backingStorage = new LocalStorageService(objectMapper, localStoragePath);
        }
        return new InMemoryStorageService(backingStorage);
    }
}
