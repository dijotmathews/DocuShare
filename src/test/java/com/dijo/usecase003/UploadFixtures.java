package com.dijo.usecase003;

import com.dijo.client.ClientDetails;
import com.dijo.client.ClientService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared helpers for UC-003 tests, which share one database and document store. */
final class UploadFixtures {

    /** Matches app.documents.root in the test application.properties. */
    static final Path STORE = Path.of("target/test-documents").toAbsolutePath().normalize();

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    private UploadFixtures() {
    }

    /** A unique, well-formed PAN. */
    static String nextPan() {
        return "TESTB" + COUNTER.incrementAndGet() + "Z";
    }

    /** A unique folder in the document store. */
    static String nextLocation() {
        return "uc003/run-" + ProcessHandle.current().pid() + "-" + COUNTER.incrementAndGet();
    }

    static void signInAs(String username, String role) {
        var principal = User.withUsername(username).password("n/a").roles(role).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    /** Creates a client with the given PAN; the caller must be signed in as admin. */
    static void createClient(ClientService clientService, String pan, String name) {
        ClientDetails d = new ClientDetails();
        d.setClientName(name);
        d.setPanNumber(pan);
        d.setDateOfIncorporation(LocalDate.of(2015, 4, 1));
        d.setAddress("12 MG Road, Kochi");
        d.setEmail("accounts@acme.in");
        d.setContactNumber("+91 98765 43210");
        clientService.createClient(d);
    }
}
