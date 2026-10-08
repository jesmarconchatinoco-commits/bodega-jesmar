package com.test.desarrollo_web.config;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class StoragePathResolver {

    public Path resolve(String configuredDir) {
        Path path = Path.of(configuredDir);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return findProjectRoot().resolve(configuredDir).normalize();
    }

    private Path findProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && current != null; i++) {
            if (Files.exists(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }
}
