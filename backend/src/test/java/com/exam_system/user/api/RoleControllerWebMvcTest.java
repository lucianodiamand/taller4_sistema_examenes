package com.exam_system.user.api;

import com.exam_system.auth.security.JwtAuthenticationFilter;
import com.exam_system.user.application.RolePermissionService;
import com.exam_system.user.domain.Role;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.exam_system.shared.api.ApiExceptionHandler.class)
class RoleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RolePermissionService rolePermissionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "permissions.manage")
    void getRolesReturnsList() throws Exception {
        Role role = new Role();
        role.setId(1L);
        role.setName("ADMIN");
        role.setDescription("Admin role");

        when(rolePermissionService.findAllRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ADMIN"));
    }

    @Test
    @WithMockUser(authorities = "permissions.manage")
    void patchRolePermissionsUpdates() throws Exception {
        when(rolePermissionService.replacePermissions(eq("ADMIN"), eq(List.of("permissions.manage"))))
                .thenReturn(Set.of("permissions.manage"));

        mockMvc.perform(patch("/api/roles/admin/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["permissions.manage"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
