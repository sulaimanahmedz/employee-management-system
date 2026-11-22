package com.ems.identity.repository;

import com.ems.identity.entity.Account;
import com.ems.identity.entity.AccountRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Page<Account> findByRole(AccountRole role, Pageable pageable);
    Optional<Account> findByLogin(String login);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.login = :login")
    Optional<Account> lockByLogin(String login);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a order by a.id")
    java.util.List<Account> lockAll();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.resetDigest = :digest")
    Optional<Account> lockByResetDigest(String digest);

}
