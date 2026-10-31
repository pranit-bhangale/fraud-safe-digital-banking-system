package com.banking.user.controller;

import com.banking.user.dto.UserAuthResponse;
import com.banking.user.dto.UserRequest;
import com.banking.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/create", version = "v1")
    public ResponseEntity<Void> createUseAccount(@RequestBody @Valid UserRequest request) {
        this.userService.createUserAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping(value = "/internal/by-username/{username}", version = "v1")
    public ResponseEntity<UserAuthResponse> getUserByUsername(@PathVariable("username") String username) {
        final UserAuthResponse response = this.userService.getUserDataByUsername(username);
        return ResponseEntity.ok(response);
    }
}
