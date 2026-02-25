package com.springbootangular.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.springbootangular.backend.converter.user.UserDTOToModelConverter;
import com.springbootangular.backend.converter.user.UserToDTOConverter;
import com.springbootangular.backend.dto.UserDTO;
import com.springbootangular.backend.model.User;
import com.springbootangular.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    UserToDTOConverter userToDTOConverter;

    @MockitoBean
    UserDTOToModelConverter userDTOToModelConverter;

    // ── GET /api/user/ ─────────────────────────────────────────────

    @Test
    void getAllUsers_noUsers_returnsEmptyList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());
        when(userToDTOConverter.convertAll(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/user/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllUsers_hasUsers_returnsList() throws Exception {
        User user = new User(1, "Alice");
        UserDTO dto = new UserDTO(1, "Alice");
        when(userService.getAllUsers()).thenReturn(List.of(user));
        when(userToDTOConverter.convertAll(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/user/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Alice")));
    }

    @Test
    void getAllUsers_withNameParam_delegatesToSearchNotGetAll() throws Exception {
        User user = new User(2, "Bob");
        UserDTO dto = new UserDTO(2, "Bob");
        when(userService.getUsersContainingName("Bo")).thenReturn(List.of(user));
        when(userToDTOConverter.convertAll(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/user/").param("name", "Bo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Bob")));

        verify(userService).getUsersContainingName("Bo");
        verify(userService, never()).getAllUsers();
    }

    @Test
    void getAllUsers_serviceThrowsUnexpectedly_returns500WithProblemDetail() throws Exception {
        when(userService.getAllUsers()).thenThrow(new RuntimeException("DB unreachable"));

        mockMvc.perform(get("/api/user/"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail", is("An unexpected error occurred")));
    }

    // ── GET /api/user/{id} ─────────────────────────────────────────────

    @Test
    void getUserById_found_returns200WithDto() throws Exception {
        User user = new User(1, "Alice");
        UserDTO dto = new UserDTO(1, "Alice");
        when(userService.getUserById(1)).thenReturn(Optional.of(user));
        when(userToDTOConverter.convert(user)).thenReturn(dto);

        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Alice")));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        when(userService.getUserById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/user/ ─────────────────────────────────────────────

    @Test
    void createUser_validNewUser_returns201() throws Exception {
        UserDTO requestDto = new UserDTO(null, "Carol");
        User unsaved = new User(null, "Carol");
        User saved = new User(3, "Carol");
        UserDTO responseDto = new UserDTO(3, "Carol");

        when(userDTOToModelConverter.convert(any())).thenReturn(unsaved);
        when(userService.saveUser(any())).thenReturn(saved);
        when(userToDTOConverter.convert(saved)).thenReturn(responseDto);

        mockMvc.perform(post("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("Carol")));
    }

    @Test
    void createUser_withExistingId_returns400() throws Exception {
        UserDTO requestDto = new UserDTO(5, "Carol");

        mockMvc.perform(post("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(userDTOToModelConverter, never()).convert(any());
    }

    @Test
    void createUser_blankName_returnsValidationProblemDetail() throws Exception {
        UserDTO requestDto = new UserDTO(null, "");

        mockMvc.perform(post("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Validation failed")))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createUser_malformedJson_returns400WithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Malformed request body")));
    }

    // ── PUT /api/user/ ─────────────────────────────────────────────

    @Test
    void saveUser_existingId_returns200() throws Exception {
        UserDTO requestDto = new UserDTO(1, "Updated");
        User existing = new User(1, "Alice");
        User converted = new User(1, "Updated");
        User saved = new User(1, "Updated");
        UserDTO responseDto = new UserDTO(1, "Updated");

        // Controller now checks existence before converting
        when(userService.getUserById(1)).thenReturn(Optional.of(existing));
        when(userDTOToModelConverter.convert(any())).thenReturn(converted);
        when(userService.saveUser(any())).thenReturn(saved);
        when(userToDTOConverter.convert(saved)).thenReturn(responseDto);

        mockMvc.perform(put("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated")));
    }

    @Test
    void saveUser_noId_returns404() throws Exception {
        UserDTO requestDto = new UserDTO(null, "Orphan");

        mockMvc.perform(put("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(userService, never()).getUserById(any());
        verify(userDTOToModelConverter, never()).convert(any());
    }

    @Test
    void saveUser_nonExistentId_returns404() throws Exception {
        UserDTO requestDto = new UserDTO(999, "Ghost");

        when(userService.getUserById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(userDTOToModelConverter, never()).convert(any());
    }

    @Test
    void saveUser_blankName_returnsValidationProblemDetail() throws Exception {
        UserDTO requestDto = new UserDTO(1, "   ");

        mockMvc.perform(put("/api/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Validation failed")));
    }

    // ── DELETE /api/user/{id} ─────────────────────────────────────────────

    @Test
    void deleteUser_returns204() throws Exception {
        doNothing().when(userService).deleteUserById(1);

        mockMvc.perform(delete("/api/user/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUserById(1);
    }
}
