package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String userRole);

}
