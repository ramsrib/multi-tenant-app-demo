package com.ramsrib.springbootmultitenant2.service;

import com.ramsrib.springbootmultitenant2.model.User;
import com.ramsrib.springbootmultitenant2.repository.UserRepository;
import com.ramsrib.springbootmultitenant2.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements ApplicationRunner {

  private final UserRepository userRepository;
  @PersistenceContext
  public EntityManager entityManager;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User createUser(User user) {
    return userRepository.save(user);
  }

  @Transactional
  public List<User> listUsers() {
    return userRepository.findAll();
  }

  @Transactional
  public Optional<User> getUser(String userId) {
    return userRepository.findById(userId);
  }

  @Transactional
  public void deleteUser(String userId) {
    userRepository.deleteById(userId);
  }

  @Override
  public void run(ApplicationArguments applicationArguments) throws Exception {
    TenantContext.setCurrentTenant("tenant1");
    userRepository.save(new User(null, "user1", "Test1", "User", null));
    TenantContext.setCurrentTenant("tenant2");
    userRepository.save(new User(null, "user2", "Test2", "User", null));
    TenantContext.clear();
  }

}
