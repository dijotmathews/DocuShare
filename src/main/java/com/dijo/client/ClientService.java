package com.dijo.client;

import com.dijo.user.Pan;
import com.dijo.user.Role;
import com.dijo.user.User;
import com.dijo.user.UserRepository;
import com.dijo.user.UserService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Creating and maintaining clients (UC-006). Only admins may call these
 * methods (BR-08).
 */
@Service
@RolesAllowed("ADMIN")
public class ClientService {

    /** Result of creating a client: the record plus its default password (BR-07). */
    public record CreatedClient(Client client, String defaultPassword) {
    }

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final Clock clock;

    public ClientService(ClientRepository clientRepository, UserRepository userRepository, UserService userService,
                         Clock clock) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return clientRepository.findAllByOrderByPanNumberAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Client> findByPan(String panNumber) {
        return clientRepository.findByPanNumber(Pan.normalize(panNumber));
    }

    /**
     * Creates the CLIENT user account (username = PAN, default password
     * PAN@DDMMYYYY of the date of incorporation) and the client record in one transaction (steps 9–13, BR-06).
     * If anything fails, neither is created.
     */
    @Transactional
    public CreatedClient createClient(ClientDetails details) {
        ClientValidator.validate(details, true, LocalDate.now(clock));

        String pan = Pan.normalize(details.getPanNumber());
        if (clientRepository.existsByPanNumber(pan) || userRepository.findByUsername(pan).isPresent()) {
            throw new DuplicatePanException(pan);
        }

        String defaultPassword = DefaultPassword.forClient(pan, details.getDateOfIncorporation());
        User user = userService.createUser(pan, defaultPassword, Role.CLIENT);
        Client client = clientRepository.save(new Client(pan, user, details));
        return new CreatedClient(client, defaultPassword);
    }

    /**
     * Sets the client's password back to the default (PAN@DDMMYYYY, from the
     * current date of incorporation), for when the client has lost theirs.
     *
     * @return the default password, to be shown to the admin
     */
    @Transactional
    public String resetPassword(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        String password = DefaultPassword.forClient(client.getPanNumber(), client.getDateOfIncorporation());
        userService.changePassword(client.getUser(), password);
        return password;
    }

    /**
     * Updates a client's details. The PAN can't be changed because it is the
     * client's login username.
     */
    @Transactional
    public Client updateClient(Long clientId, ClientDetails details) {
        ClientValidator.validate(details, false, LocalDate.now(clock));
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        client.apply(details);
        return clientRepository.save(client);
    }
}
