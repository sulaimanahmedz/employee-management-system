package com.ems.identity.repository;
import com.ems.identity.entity.TeamMembership;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TeamMembershipRepository extends JpaRepository<TeamMembership,Long> {
    List<TeamMembership> findByManagerIdOrderByEmployeeId(Long managerId);
    void deleteByManagerId(Long managerId);
}
