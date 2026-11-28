package com.banking.user.controller;

import com.banking.user.dto.UserInformation;
import com.banking.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Tag(name = "Internal User Verification", description = "Inter-service APIs for Account and Transaction services to verify user existence and details")
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping(value = "/{userId}/exists")
    @SecurityRequirements
    @Operation(summary = "Verify User Exists", description = "Checks whether a user exists by UUID for downstream account creation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification result returned", content = @Content(schema = @Schema(implementation = Boolean.class)))
    })
    public ResponseEntity<Boolean> existsByUserId(
            @Parameter(description = "User unique UUID identifier", required = true)
            @PathVariable UUID userId) {
        log.debug("Internal user check exists: userId={}", userId);
        return ResponseEntity.ok(userRepository.existsById(userId));
    }

    @GetMapping(value = "/{userId}")
    @SecurityRequirements
    @Operation(summary = "Get User Details by ID", description = "Fetches user demographic and contact information by UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details found", content = @Content(schema = @Schema(implementation = UserInformation.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserInformation> getUserById(
            @Parameter(description = "User unique UUID identifier", required = true)
            @PathVariable UUID userId) {
        log.debug("Internal user fetch details: userId={}", userId);
        return userRepository.findById(userId)
                .map(u -> new UserInformation(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
