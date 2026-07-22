package com.exam_system.user.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.auth.security.JwtAuthenticationFilter;
import com.exam_system.user.application.UserService;
import com.exam_system.user.domain.Role;
import com.exam_system.user.domain.User;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.exam_system.shared.api.ApiExceptionHandler.class)
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CurrentUser currentUser;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "users.read.any")
    void getUsersReturnsList() throws Exception {
        User user = buildUser(10L, "Ana", "ana", "PROFESSOR");
        when(currentUser.id()).thenReturn(1L);
        when(userService.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("ana"))
                .andExpect(jsonPath("$[0].manageable").value(true));
    }

    @Test
    @WithMockUser(authorities = "users.create.any")
    void postUsersCreatesManagedUser() throws Exception {
        when(currentUser.id()).thenReturn(1L);
        when(userService.createByAdmin(eq("Ana"), eq("ana"), eq("secret12"), eq("PROFESSOR")))
                .thenReturn(buildUser(11L, "Ana", "ana", "PROFESSOR"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","username":"ana","password":"secret12","role":"PROFESSOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PROFESSOR"));
    }

    @Test
    @WithMockUser(authorities = "users.update.any")
    void patchUserUpdatesManagedUser() throws Exception {
        when(currentUser.id()).thenReturn(1L);
        when(userService.updateByAdmin(eq(10L), eq("Nuevo"), eq(null), eq("STUDENT")))
                .thenReturn(buildUser(10L, "Nuevo", "ana", "STUDENT"));

        mockMvc.perform(patch("/api/users/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nuevo","role":"STUDENT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo"));
    }

    @Test
    @WithMockUser(authorities = "users.delete.any")
    void deleteUserReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/10"))
                .andExpect(status().isNoContent());
    }

    private static User buildUser(Long id, String name, String username, String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
