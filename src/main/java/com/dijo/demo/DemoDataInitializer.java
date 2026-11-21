package com.dijo.demo;

import com.dijo.client.ClientDetails;
import com.dijo.client.ClientService;
import com.dijo.client.DefaultPassword;
import com.dijo.document.Document;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Demo data, only with the {@code demo} Spring profile: a demo client with
 * one document of every type (so every group on My Documents has documents)
 * for each of the last three completed financial years, with upload dates
 * backdated to when such a document is typically uploaded. Runs once the app is
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
    private final TransactionTemplate transactions;
    private final EntityManager entityManager;

    public DemoDataInitializer(ClientService clientService, DocumentService documentService,
                               UserRepository userRepository, Clock clock,
                               PlatformTransactionManager transactionManager, EntityManager entityManager) {
        this.clientService = clientService;
        this.documentService = documentService;
        this.userRepository = userRepository;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
        this.entityManager = entityManager;
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

    /**
     * Oldest year first and, within a year, in upload-date order, so ids follow
     * the dates. Each document is then backdated to {@link #plannedUpload}.
     */
    private int uploadDemoDocuments() {
        Instant now = clock.instant();
        int latest = latestCompletedFinancialYear(LocalDate.now(clock));
        int count = 0;
        for (int fy = latest - YEARS + 1; fy <= latest; fy++) {
            int ay = fy + 1;
            List<DocumentType> inUploadOrder = Arrays.stream(DocumentType.values())
                    .sorted(Comparator.comparing(DemoDataInitializer::uploadDayInAssessmentYear))
                    .toList();
            for (int rank = 0; rank < inUploadOrder.size(); rank++) {
                DocumentType type = inUploadOrder.get(rank);
                String fileName = type.name().toLowerCase(Locale.ROOT).replace('_', '-')
                        + "-fy" + fy + "-" + String.format("%02d", (fy + 1) % 100) + ".pdf";
                byte[] pdf = DemoPdf.withLines(List.of(
                        type.label(),
                        DEMO_NAME + " (PAN " + DEMO_PAN + ")",
                        "Financial Year " + fy + "-" + String.format("%02d", (fy + 1) % 100)
                                + "  |  Assessment Year " + ay + "-" + String.format("%02d", (ay + 1) % 100),
                        "",
                        "Demo document generated by DocuShare's demo profile."));
                Document document = documentService.upload(new DocumentUpload(DEMO_PAN, String.valueOf(ay),
                        String.valueOf(fy), type, DEMO_PAN + "/" + ay, fileName, pdf), true);
                backdate(document.getId(), plannedUpload(type, fy, rank, inUploadOrder.size(), now));
                count++;
            }
        }
        return count;
    }

    /**
     * When a document of this type would typically be uploaded: days after
     * 1 April of the assessment year (the April after the financial year ends).
     * Accounts first, then audits, the tax audit before its 30 September due
     * date, the computation, and the return acknowledgement before 31 October.
     */
    static int uploadDayInAssessmentYear(DocumentType type) {
        return switch (type) {
            case MISC_REPORT -> 75;            // mid June
            case BALANCE_SHEET -> 135;         // mid August
            case PROFIT_LOSS -> 136;
            case AUDIT_REPORT -> 160;          // early September
            case TAX_AUDIT_REPORT -> 175;      // late September
            case TAX_COMPUTATION -> 195;       // mid October
            case IT_ACKNOWLEDGEMENT -> 205;    // late October
        };
    }

    /**
     * The demo upload time for a document: 11:00 (app time zone) on its typical
     * day, never in the future. For a financial year that ended recently some
     * typical days are still ahead; those documents get the last hours before
     * now instead, one hour apart, keeping their order.
     */
    Instant plannedUpload(DocumentType type, int financialYear, int rank, int typesInYear, Instant now) {
        ZoneId zone = clock.getZone();
        Instant typical = LocalDate.of(financialYear + 1, Month.APRIL, 1)
                .plusDays(uploadDayInAssessmentYear(type))
                .atTime(LocalTime.of(11, 0).plusMinutes(rank * 7L))
                .atZone(zone).toInstant();
        Instant latestAllowed = now.minus(Duration.ofHours(typesInYear - rank));
        return (typical.isAfter(latestAllowed) ? latestAllowed : typical).truncatedTo(ChronoUnit.MILLIS);
    }

    /** Demo only: replaces the upload time the service recorded (always "now"). */
    private void backdate(Long documentId, Instant uploadedAt) {
        transactions.executeWithoutResult(status -> entityManager
                .createQuery("update Document d set d.uploadedAt = :uploadedAt where d.id = :id")
                .setParameter("uploadedAt", uploadedAt)
                .setParameter("id", documentId)
                .executeUpdate());
    }

    /** The financial year (April–March, by start year) that most recently ended. */
    static int latestCompletedFinancialYear(LocalDate today) {
        return today.getMonthValue() >= Month.APRIL.getValue() ? today.getYear() - 1 : today.getYear() - 2;
    }
}
