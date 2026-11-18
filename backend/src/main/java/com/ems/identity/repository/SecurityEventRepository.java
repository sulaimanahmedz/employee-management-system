package com.ems.identity.repository;
import com.ems.identity.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {}
