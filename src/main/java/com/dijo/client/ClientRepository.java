package com.dijo.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByPanNumber(String panNumber);

    boolean existsByPanNumber(String panNumber);

    List<Client> findAllByOrderByPanNumberAsc();
}
