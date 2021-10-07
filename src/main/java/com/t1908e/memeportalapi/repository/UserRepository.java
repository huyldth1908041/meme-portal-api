package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
