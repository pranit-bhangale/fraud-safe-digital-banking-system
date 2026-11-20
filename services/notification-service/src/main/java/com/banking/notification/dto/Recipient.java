package com.banking.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.io.Serializable;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Recipient(

        @NotBlank(message = "Primary recipient email cannot be blank")
        @Email(message = "Primary recipient email must be a valid email format")
        @JsonProperty("to")
        String to
) implements Serializable {

    public static Recipient of(String to) {
        return Recipient.builder().to(to).build();
    }
}