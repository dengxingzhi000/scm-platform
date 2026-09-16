package com.scmcloud.common.rest.reload;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RestClient 璇佷功鐑洿鏂板姞杞藉櫒
 * <p>鏀寔闆跺仠锟絤TLS 璇佷功鏇存柊</p>
 *
 * <p>鍔熻兘锟?
 * <ul>
 *   <li>瀹氭椂妫€娴嬭瘉涔︽枃浠跺彉鍖栵紙姣忓垎閽熸鏌ヤ竴娆★級</li>
 *   <li>鑷姩閲嶆柊鍔犺浇璇佷功锛堟棤闇€閲嶅惎搴旂敤锟?li>
 *   <li>绾跨▼瀹夊叏锟絊SLContext 鏇存柊</li>
 *   <li>鐩戝惉鍣ㄦā寮忥細鏀寔鍏朵粬缁勪欢璁㈤槄璇佷功鏇存柊浜嬩欢</li>
 * </ul>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateReloaderRestClient {

    private final Resource keystoreResource;
    private final String keystorePassword;
    private final Resource truststoreResource;
    private final String truststorePassword;

    // 褰撳墠 SSLContext锛堢嚎绋嬪畨鍏ㄧ殑鍘熷瓙寮曠敤锟?
    private final AtomicReference<SSLContext> sslContextRef = new AtomicReference<>();

    // 璇佷功鏂囦欢鏈€鍚庝慨鏀规椂闂寸紦锟?
    private volatile FileTime lastKeystoreModified;
    private volatile FileTime lastTruststoreModified;

    // 鐩戝惉鍣ㄥ垪琛紙绾跨▼瀹夊叏锟?
    private final CopyOnWriteArrayList<CertificateReloadListener> listeners =
        new CopyOnWriteArrayList<>();

    /**
     * 鍒濆鍖栧姞杞借瘉锟?
     */
    @PostConstruct
    public void initialize() throws Exception {
        log.info("Initializing RestClient certificates...");
        loadCertificates(true);
        updateLastModifiedTimes();
        log.info("RestClient certificates initialized successfully");
    }

    /**
     * 瀹氭椂妫€鏌ヨ瘉涔︽枃浠跺彉鍖栵紙姣忓垎閽熸鏌ヤ竴娆★級
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void checkAndReload() {
        try {
            boolean keystoreChanged = hasFileChanged(keystoreResource, lastKeystoreModified);
            boolean truststoreChanged = hasFileChanged(truststoreResource, lastTruststoreModified);

            if (keystoreChanged || truststoreChanged) {
                log.info("Certificate file changes detected, starting hot reload...");
                loadCertificates(false);
                updateLastModifiedTimes();
                notifyListeners();
                log.info("Certificate hot reload successful (no restart required)");
            }
        } catch (Exception e) {
            log.error("Certificate hot reload failed, continuing with old certificates: {}",
                      e.getMessage(), e);
        }
    }

    /**
     * 鍔犺浇璇佷功骞舵瀯锟絊SLContext
     *
     * @param isInitial 鏄惁涓哄垵濮嬪寲鍔犺浇
     */
    private void loadCertificates(boolean isInitial) throws Exception {
        // 鍔犺浇 KeyStore 锟絋rustStore
        KeyStore keyStore = loadKeyStore(keystoreResource, keystorePassword);
        KeyStore trustStore = loadKeyStore(truststoreResource, truststorePassword);

        // 鍒濆锟終eyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        );
        kmf.init(keyStore, keystorePassword.toCharArray());

        // 鍒濆锟絋rustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        tmf.init(trustStore);

        // 鍒涘缓 SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
            kmf.getKeyManagers(),
            tmf.getTrustManagers(),
            new SecureRandom()
        );

        // 鍘熷瓙鎬ф洿锟絊SLContext
        sslContextRef.set(sslContext);

        if (!isInitial) {
            log.info("SSLContext updated successfully");
        }
    }

    /**
     * 鑾峰彇褰撳墠 SSLContext
     *
     * @return 褰撳墠锟絊SLContext
     */
    public SSLContext getSslContext() {
        return sslContextRef.get();
    }

    /**
     * 娉ㄥ唽璇佷功閲嶈浇鐩戝惉锟?
     *
     * @param listener 鐩戝惉锟?
     */
    public void addListener(CertificateReloadListener listener) {
        listeners.add(listener);
        log.debug("Certificate reload listener registered: {}", listener.getClass().getSimpleName());
    }

    /**
     * 绉婚櫎璇佷功閲嶈浇鐩戝惉锟?
     *
     * @param listener 鐩戝惉锟?
     */
    public void removeListener(CertificateReloadListener listener) {
        listeners.remove(listener);
    }

    /**
     * 閫氱煡鎵€鏈夌洃鍚櫒璇佷功宸查噸锟?
     */
    private void notifyListeners() {
        SSLContext sslContext = getSslContext();
        for (CertificateReloadListener listener : listeners) {
            try {
                listener.onCertificateReloaded(sslContext);
            } catch (Exception e) {
                log.error("Failed to notify listener {}: {}",
                          listener.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 妫€鏌ユ枃浠舵槸鍚﹀凡鍙樺寲
     *
     * @param resource 璧勬簮
     * @param lastModified 涓婃淇敼鏃堕棿
     * @return true 濡傛灉鏂囦欢宸插彉锟?
     */
    private boolean hasFileChanged(Resource resource, FileTime lastModified) {
        try {
            Path path = getPathFromResource(resource);
            if (path == null) {
                return false;
            }

            FileTime currentModified = Files.getLastModifiedTime(path);
            return lastModified == null || currentModified.compareTo(lastModified) > 0;
        } catch (IOException e) {
            log.warn("Failed to check file modification time: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 鏇存柊鏈€鍚庝慨鏀规椂闂寸紦锟?
     */
    private void updateLastModifiedTimes() {
        try {
            Path keystorePath = getPathFromResource(keystoreResource);
            if (keystorePath != null) {
                lastKeystoreModified = Files.getLastModifiedTime(keystorePath);
            }

            Path truststorePath = getPathFromResource(truststoreResource);
            if (truststorePath != null) {
                lastTruststoreModified = Files.getLastModifiedTime(truststorePath);
            }
        } catch (IOException e) {
            log.warn("Failed to update last modified times: {}", e.getMessage());
        }
    }

    /**
     * 锟絉esource 鑾峰彇 Path
     */
    private Path getPathFromResource(Resource resource) {
        try {
            if (resource.isFile()) {
                return resource.getFile().toPath();
            } else if (resource.getURL().getProtocol().equals("file")) {
                return Paths.get(resource.getURL().toURI());
            }
        } catch (Exception e) {
            log.debug("Resource is not a file: {}", resource.getDescription());
        }
        return null;
    }

    /**
     * 鍔犺浇 KeyStore
     *
     * @param resource 璇佷功鏂囦欢璧勬簮
     * @param password 瀵嗙爜
     * @return KeyStore
     */
    private KeyStore loadKeyStore(Resource resource, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = resource.getInputStream()) {
            keyStore.load(is, password.toCharArray());
        }
        return keyStore;
    }

    /**
     * 璇佷功閲嶈浇鐩戝惉鍣ㄦ帴锟?
     */
    @FunctionalInterface
    public interface CertificateReloadListener {
        /**
         * 璇佷功閲嶈浇鍥炶皟
         *
         * @param sslContext 鏂扮殑 SSLContext
         */
        void onCertificateReloaded(SSLContext sslContext);
    }
}
