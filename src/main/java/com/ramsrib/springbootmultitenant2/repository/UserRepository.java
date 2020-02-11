package com.ramsrib.springbootmultitenant2.repository;

import com.ramsrib.springbootmultitenant2.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT * FROM user_info where username = '11111' ", nativeQuery = true)
    List<User> find11111( );
}
