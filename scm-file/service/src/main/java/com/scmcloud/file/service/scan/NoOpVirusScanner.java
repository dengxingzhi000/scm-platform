package com.scmcloud.file.service.scan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.InputStream;

@Slf4j
@Component
@ConditionalOnProperty(name = "storage.virus-scan.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVirusScanner implements FileVirusScanner {

    @Override
    public ScanResult scan(InputStream content) {
        return ScanResult.clean();
    }

    @Override
    public boolean enabled() {
        return false;
    }
}
