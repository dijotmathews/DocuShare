package com.dijo.document;

import com.dijo.client.ClientRepository;
import com.dijo.user.User;
import com.dijo.user.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Uploading documents (UC-003) and listing all documents (UC-007). Admin only
 * (UC-003 BR-04, UC-007 BR-01).
 */
@Service
@RolesAllowed("ADMIN")
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final DocumentStorage storage;
    private final Clock clock;

    public DocumentService(DocumentRepository documentRepository, ClientRepository clientRepository,
                           UserRepository userRepository, DocumentStorage storage, Clock clock) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.clock = clock;
    }

    /**
     * Validates the upload, stores the file and records its metadata (steps
     * 9–12). The record is saved first and the file written last inside the
     * same transaction, so if writing fails the record is rolled back and no
     * file is left behind.
     *
     * @param overwrite true once the admin has confirmed replacing an existing file (AF-4)
     * @throws DocumentValidationException     missing or invalid fields (AF-2, BR-05)
     * @throws ClientNotFoundException         no client with the PAN (AF-1, BR-02)
     * @throws DocumentAlreadyExistsException  the file exists and overwrite is false (AF-4)
     * @throws DocumentStorageException        the file couldn't be written (AF-3)
     */
    @Transactional
    public Document upload(DocumentUpload upload, boolean overwrite) {
        DocumentValidator.ValidUpload valid = DocumentValidator.validate(upload, Year.now(clock));

        if (!clientRepository.existsByPanNumber(valid.panNumber())) {
            throw new ClientNotFoundException(valid.panNumber());
        }

        String storedPath = valid.location() + "/" + valid.fileName();
        Optional<Document> existing =
                documentRepository.findByLocationAndDocumentIdentifier(valid.location(), valid.fileName());
        boolean fileExists;
        try {
            fileExists = storage.exists(valid.location(), valid.fileName());
        } catch (RuntimeException e) {
            throw new DocumentStorageException(e);
        }
        if ((existing.isPresent() || fileExists) && !overwrite) {
            throw new DocumentAlreadyExistsException(storedPath);
        }

        Document document = existing.orElseGet(() -> new Document(valid.location(), valid.fileName()));
        // Milliseconds: what SQLite stores, so the returned record matches the saved one
        document.record(valid, currentUser(), Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
        documentRepository.saveAndFlush(document);

        try {
            storage.store(valid.location(), valid.fileName(), upload.content(), overwrite);
        } catch (FileAlreadyExistsException e) {
            // Someone else wrote the same file in the meantime
            throw new DocumentAlreadyExistsException(storedPath);
        } catch (IOException | RuntimeException e) {
            throw new DocumentStorageException(e);
        }
        return document;
    }

    /**
     * All documents in the system, from every client (BR-02), most recent
     * first (step 3), narrowed by the filter (step 7). Dates are upload dates
     * in the server's time zone, both ends included.
     */
    @Transactional(readOnly = true)
    public List<AdminDocumentRow> findAll(DocumentFilter filter) {
        String search = filter.search() == null || filter.search().isBlank() ? null
                : "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
        ZoneId zone = clock.getZone();
        Instant from = filter.uploadedFrom() == null ? null : filter.uploadedFrom().atStartOfDay(zone).toInstant();
        Instant until = filter.uploadedTo() == null ? null
                : filter.uploadedTo().plusDays(1).atStartOfDay(zone).toInstant();
        return documentRepository.findAllForAdmin(search, filter.documentType(), from, until);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Signed-in user not found: " + username));
    }
}
