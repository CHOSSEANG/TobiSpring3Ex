package com.example.spring_start.user.service;

import com.example.spring_start.user.dao.UserDao;
import com.example.spring_start.user.domain.User;

import java.util.List;

public interface UserService {
    void add(User user);

    User get(String id);

    List<User> getAll();

    void update(User user);

    void deleteAll();

    void upgradeLevels();

    void setUserDao(UserDao userDao);
}
