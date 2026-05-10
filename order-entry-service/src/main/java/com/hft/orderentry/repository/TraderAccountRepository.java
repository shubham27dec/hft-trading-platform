package com.hft.orderentry.repository;

import com.hft.orderentry.entity.TraderAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraderAccountRepository extends JpaRepository<TraderAccount, Long> {

    Optional<TraderAccount> findByApiKey(String apiKey);

    Optional<TraderAccount> findByUsername(String username);
}
