package com.ramsrib.springbootmultitenant2.repository;

import com.ramsrib.springbootmultitenant2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
