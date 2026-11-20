package com.dijo.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes document files under the configured document store
 * ({@code app.documents.root}). Locations are always relative to it.
 */
@Component
class DocumentStorage {

    private final Path root;

    DocumentStorage(@Value("${app.documents.root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    Path root() {
        return root;
    }

    /** Where a stored document's file lives. */
    Path pathOf(String location, String fileName) {
        return resolve(location, fileName);
    }

    boolean exists(String location, String fileName) {
        return Files.exists(resolve(location, fileName));
    }

    /**
     * Writes the file. The content goes to a temporary file in the target
     * folder first and is then moved into place, so a failed write never
     * leaves a half-written document behind.
     *
     * @throws java.nio.file.FileAlreadyExistsException if the file exists and overwrite is false
     */
    void store(String location, String fileName, byte[] content, boolean overwrite) throws IOException {
        Path target = resolve(location, fileName);
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.write(temp, content);
            if (overwrite) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(temp, target);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Path resolve(String location, String fileName) {
        Path path = root.resolve(location).resolve(fileName).normalize();
        if (!path.startsWith(root) || !path.getParent().startsWith(root)) {
            throw new IllegalArgumentException("Location is outside the document store: " + location);
        }
        return path;
    }
}
