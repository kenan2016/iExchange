package com.iexchange.user.init;

import com.iexchange.user.entity.UserEntity;
import com.iexchange.user.service.UserService;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化演示用户。
 */
@Component
public class UserDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_USERNAME = "demo";
    private static final String DEFAULT_PASSWORD = "demo123";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserDataInitializer(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 如果演示用户不存在，则创建
        if (userService.findByUsername(DEFAULT_USERNAME) == null) {
            UserEntity user = new UserEntity();
            user.setUsername(DEFAULT_USERNAME);
            user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userService.save(user);
        }
    }
}
