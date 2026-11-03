package com.scmcloud.file.service.scan;

import java.io.InputStream;

public interface FileVirusScanner {

    ScanResult scan(InputStream content);

    default boolean enabled() {
        return true;
    }

    class ScanResult {
        private final boolean clean;
        private final String threatName;

        private ScanResult(boolean clean, String threatName) {
            this.clean = clean;
            this.threatName = threatName;
        }

        public static ScanResult clean() {
            return new ScanResult(true, null);
        }

        public static ScanResult infected(String threatName) {
            return new ScanResult(false, threatName);
        }

        public boolean isClean() {
            return clean;
        }

        public String getThreatName() {
            return threatName;
        }
    }
}
