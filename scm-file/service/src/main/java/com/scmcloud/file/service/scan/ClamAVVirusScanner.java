package com.scmcloud.file.service.scan;

import com.scmcloud.file.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * ClamAV (clamd) integration via the INSTREAM protocol.
 *
 * Enable with {@code storage.virus-scan.enabled=true} and configure
 * {@code storage.virus-scan.clamd-host} / {@code storage.virus-scan.clamd-port}.
 * The streamed file content is sent to clamd so no intermediate copy is required.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.virus-scan.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ClamAVVirusScanner implements FileVirusScanner {

    private final StorageConfig storageConfig;

    @Override
    public ScanResult scan(InputStream content) {
        StorageConfig.VirusScan clamd = storageConfig.getVirusScan();
        int chunkSize = 64 * 1024;
        byte[] buffer = new byte[chunkSize];
            try (Socket socket = new Socket(clamd.getClamdHost(), clamd.getClamdPort());
             BufferedInputStream bis = new BufferedInputStream(content)) {
            socket.getOutputStream().write("zINSTREAM\0".getBytes());
            int read;
            while ((read = bis.read(buffer)) != -1) {
                byte[] sizeHeader = new byte[] {
                        (byte) ((read >> 24) & 0xff),
                        (byte) ((read >> 16) & 0xff),
                        (byte) ((read >> 8) & 0xff),
                        (byte) (read & 0xff)
                };
                socket.getOutputStream().write(sizeHeader);
                socket.getOutputStream().write(buffer, 0, read);
            }
            socket.getOutputStream().write(new byte[] {0, 0, 0, 0});
            socket.getOutputStream().flush();

            String response = new String(socket.getInputStream().readAllBytes()).trim();
            if (response.contains("OK")) {
                return ScanResult.clean();
            }
            String threat = response.contains("FOUND")
                    ? response.substring(0, response.indexOf("FOUND")).trim()
                    : "unknown";
            return ScanResult.infected(threat);
        } catch (IOException e) {
            log.error("ClamAV scan failed, treating as infected to be safe", e);
            return ScanResult.infected("scan-unavailable");
        }
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
