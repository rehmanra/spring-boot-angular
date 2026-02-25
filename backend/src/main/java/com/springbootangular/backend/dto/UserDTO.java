package com.springbootangular.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UserDTO(Integer id, @NotBlank String name) {}

