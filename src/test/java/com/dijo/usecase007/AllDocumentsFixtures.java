package com.dijo.usecase007;

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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Clients and documents for UC-007 tests, which share one database with other tests. */
final class AllDocumentsFixtures {

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    private AllDocumentsFixtures() {
    }

    static void signInAs(String username, String role) {
        var principal = User.withUsername(username).password("n/a").roles(role).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    /** A word no other test data contains, for searching by client name. */
    static String uniqueWord() {
        return "Zq" + Long.toString(System.nanoTime(), 36) + COUNTER.incrementAndGet();
    }

    /** Creates a client as admin and leaves the admin signed in. */
    static String createClient(ClientService clientService, String name) {
        signInAs("admin", "ADMIN");
        String pan = "TESTE" + COUNTER.incrementAndGet() + "Z";
        ClientDetails d = new ClientDetails();
        d.setClientName(name);
        d.setPanNumber(pan);
        d.setDateOfIncorporation(LocalDate.of(2015, 4, 1));
        d.setAddress("12 MG Road, Kochi");
        d.setEmail("accounts@acme.in");
        d.setContactNumber("+91 98765 43210");
        clientService.createClient(d);
        return pan;
    }

    static Document upload(DocumentService documentService, String pan, DocumentType type) {
        String location = "uc007/run-" + ProcessHandle.current().pid() + "/" + pan + "-" + COUNTER.incrementAndGet();
        return documentService.upload(new DocumentUpload(pan, "2025", "2024", type, location,
                type.name().toLowerCase() + ".pdf", "%PDF-1.7".getBytes(StandardCharsets.UTF_8)), false);
    }
}
