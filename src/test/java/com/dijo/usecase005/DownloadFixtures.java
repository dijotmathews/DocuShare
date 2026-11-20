package com.dijo.usecase005;

import com.dijo.client.ClientDetails;
import com.dijo.client.ClientService;
import com.dijo.document.Document;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import com.dijo.document.DocumentUpload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates clients and uploads documents for UC-005 tests (shared database and store). */
final class DownloadFixtures {

    /** Matches app.documents.root in the test application.properties. */
    static final Path STORE = Path.of("target/test-documents").toAbsolutePath().normalize();

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    private DownloadFixtures() {
    }

    static void signInAs(String username, String role) {
        var principal = User.withUsername(username).password("n/a").roles(role).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    /** Creates a client as admin; leaves the admin signed in. */
    static String createClient(ClientService clientService) {
        signInAs("admin", "ADMIN");
        String pan = "TESTD" + COUNTER.incrementAndGet() + "Z";
        ClientDetails d = new ClientDetails();
        d.setClientName("Client " + pan);
        d.setPanNumber(pan);
        d.setDateOfIncorporation(LocalDate.of(2015, 4, 1));
        d.setAddress("12 MG Road, Kochi");
        d.setEmail("accounts@acme.in");
        d.setContactNumber("+91 98765 43210");
        clientService.createClient(d);
        return pan;
    }

    /** Uploads a document as admin (the caller must be signed in as admin). */
    static Document upload(DocumentService documentService, String pan, String fileName, byte[] content) {
        String location = "uc005/run-" + ProcessHandle.current().pid() + "/" + pan + "-" + COUNTER.incrementAndGet();
        return documentService.upload(new DocumentUpload(pan, "2025", "2024", DocumentType.IT_ACKNOWLEDGEMENT,
                location, fileName, content), false);
    }

    static Path fileOf(Document document) {
        return STORE.resolve(document.getLocation()).resolve(document.getDocumentIdentifier());
    }
}
