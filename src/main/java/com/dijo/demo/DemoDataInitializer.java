package com.dijo.demo;

import com.dijo.client.ClientDetails;
import com.dijo.client.ClientService;
import com.dijo.client.DefaultPassword;
import com.dijo.document.DocumentService;
import com.dijo.document.DocumentType;
import com.dijo.document.DocumentUpload;
import com.dijo.user.Role;
import com.dijo.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Locale;

/**
 * Demo data, only with the {@code demo} Spring profile: a demo client with
 * one document of every type (so every group on My Documents has documents)
 * for each of the last three completed financial years. Runs once the app is
 * ready (after the admin account is created) and does nothing if the demo
 * client already exists.
 */
@Component
@Profile("demo")
public class DemoDataInitializer {

    static final String DEMO_PAN = "DEMOC1234D";
    static final String DEMO_NAME = "Demo Traders Pvt Ltd";
    static final LocalDate DEMO_INCORPORATION = LocalDate.of(2015, 4, 1);
    static final int YEARS = 3;

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final ClientService clientService;
    private final DocumentService documentService;
    private final UserRepository userRepository;
    private final Clock clock;

    public DemoDataInitializer(ClientService clientService, DocumentService documentService,
                               UserRepository userRepository, Clock clock) {
        this.clientService = clientService;
        this.documentService = documentService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createDemoData() {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            // Uploads are recorded as made by the admin account, like a real upload
            String admin = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN).map(u -> u.getUsername()).findFirst().orElseThrow();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    admin, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
            SecurityContextHolder.setContext(context);

            if (clientService.findByPan(DEMO_PAN).isPresent()) {
                log.info("Demo data already present (client {})", DEMO_PAN);
                return;
            }
            createDemoClient();
            int count = uploadDemoDocuments();
            log.info("Demo data created: client {} ({}), {} documents. Client login: {} / {}", DEMO_NAME, DEMO_PAN,
                    count, DEMO_PAN, DefaultPassword.forClient(DEMO_PAN, DEMO_INCORPORATION));
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private void createDemoClient() {
        ClientDetails details = new ClientDetails();
        details.setClientName(DEMO_NAME);
        details.setPanNumber(DEMO_PAN);
        details.setDateOfIncorporation(DEMO_INCORPORATION);
        details.setAddress("1 Demo Street, Kochi, Kerala 682001");
        details.setEmail("accounts@demo-traders.example");
        details.setContactNumber("+91 98765 43210");
        clientService.createClient(details);
    }

    /** Oldest year first, so within the app the latest year's documents are the most recent uploads. */
    private int uploadDemoDocuments() {
        int latest = latestCompletedFinancialYear(LocalDate.now(clock));
        int count = 0;
        for (int fy = latest - YEARS + 1; fy <= latest; fy++) {
            int ay = fy + 1;
            for (DocumentType type : DocumentType.values()) {
                String fileName = type.name().toLowerCase(Locale.ROOT).replace('_', '-')
                        + "-fy" + fy + "-" + String.format("%02d", (fy + 1) % 100) + ".pdf";
                byte[] pdf = DemoPdf.withLines(List.of(
                        type.label(),
                        DEMO_NAME + " (PAN " + DEMO_PAN + ")",
                        "Financial Year " + fy + "-" + String.format("%02d", (fy + 1) % 100)
                                + "  |  Assessment Year " + ay + "-" + String.format("%02d", (ay + 1) % 100),
                        "",
                        "Demo document generated by DocuShare's demo profile."));
                documentService.upload(new DocumentUpload(DEMO_PAN, String.valueOf(ay), String.valueOf(fy), type,
                        DEMO_PAN + "/" + ay, fileName, pdf), true);
                count++;
            }
        }
        return count;
    }

    /** The financial year (April–March, by start year) that most recently ended. */
    static int latestCompletedFinancialYear(LocalDate today) {
        return today.getMonthValue() >= Month.APRIL.getValue() ? today.getYear() - 1 : today.getYear() - 2;
    }
}
