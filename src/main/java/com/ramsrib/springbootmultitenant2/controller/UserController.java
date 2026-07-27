package com.ramsrib.springbootmultitenant2.controller;

import com.ramsrib.springbootmultitenant2.model.User;
import com.ramsrib.springbootmultitenant2.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController("/api/users")
public class UserController {

  private final UserService userService;

  @Autowired
  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public List<User> listUsers() {
    return userService.listUsers();
  }

  @GetMapping("filter")
  public List<User> listFilters() {
    return userService.find11111();
  }

  @PostMapping
  public User createUser(@RequestBody User user) {
    return userService.createUser(user);
  }

  @GetMapping("/{id}")
  public Optional<User> getUser(@PathVariable("id") String userId) {
    return userService.getUser(userId);
  }

  @DeleteMapping("/{id}")
  public void deleteUser(@PathVariable("id") String userId) {
    userService.deleteUser(userId);
  }


}
