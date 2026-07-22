package com.exam_system.user.api;

import com.exam_system.auth.security.JwtAuthenticationFilter;
import com.exam_system.user.application.PermissionService;
import com.exam_system.user.domain.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.exam_system.shared.api.ApiExceptionHandler.class)
class PermissionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "permissions.manage")
    void getPermissionsReturnsList() throws Exception {
        Permission permission = new Permission();
        permission.setId(1L);
        permission.setCode("users.read.any");
        permission.setDescription("Read users");
        when(permissionService.findAll()).thenReturn(List.of(permission));

        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("users.read.any"));
    }

    @Test
    @WithMockUser(authorities = "permissions.manage")
    void postPermissionCreates() throws Exception {
        Permission permission = new Permission();
        permission.setId(2L);
        permission.setCode("users.delete.any");
        permission.setDescription("Delete users");

        when(permissionService.create(eq("users.delete.any"), eq("Delete users"))).thenReturn(permission);

        mockMvc.perform(post("/api/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"users.delete.any","description":"Delete users"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @WithMockUser(authorities = "permissions.manage")
    void deletePermissionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/permissions/2"))
                .andExpect(status().isNoContent());
    }
}
