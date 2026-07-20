import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, take } from 'rxjs';

import { APP_ROUTES, AUTH_UI, UserRole, type PermissionResponse, type RoleAdminResponse } from '../../contracts/auth.contracts';
import { AdminAccessApiService } from '../../services/admin-access-api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-access',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatListModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './admin-access.component.html',
  styleUrl: './admin-access.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminAccessComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly adminAccessApi = inject(AdminAccessApiService);
  private readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly routes = APP_ROUTES;
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly roles = signal<RoleAdminResponse[]>([]);
  protected readonly permissions = signal<PermissionResponse[]>([]);
  protected readonly selectedRole = signal<UserRole>(UserRole.ADMIN);
  protected readonly rolePermissionCodes = signal<Set<string>>(new Set<string>());
  protected readonly permissionFormError = signal<string | null>(null);

  protected readonly roleDescriptionForm = this.formBuilder.nonNullable.group({
    description: ['', [Validators.required, Validators.minLength(3)]],
  });

  protected readonly permissionForm = this.formBuilder.nonNullable.group({
    id: [0],
    code: ['', Validators.required],
    description: ['', Validators.required],
  });

  protected readonly selectedRoleView = computed(() =>
    this.roles().find((role) => role.name === this.selectedRole()) ?? null
  );

  constructor() {
    this.reloadData();
  }

  protected chooseRole(role: UserRole): void {
    this.selectedRole.set(role);
    const selected = this.roles().find((item) => item.name === role);
    this.roleDescriptionForm.patchValue({ description: selected?.description ?? '' });
    this.loadRolePermissions(role);
  }

  protected hasPermission(code: string): boolean {
    return this.rolePermissionCodes().has(code);
  }

  protected togglePermission(code: string, checked: boolean): void {
    const next = new Set(this.rolePermissionCodes());
    if (checked) {
      next.add(code);
    } else {
      next.delete(code);
    }
    this.rolePermissionCodes.set(next);
  }

  protected saveRolePermissions(): void {
    if (this.busy()) {
      return;
    }

    const permissionCodes = Array.from(this.rolePermissionCodes());
    if (permissionCodes.length === 0) {
      this.snackBar.open('Debe seleccionar al menos un permiso.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
      return;
    }

    this.busy.set(true);
    this.adminAccessApi
      .replaceRolePermissions(this.selectedRole(), { permissionCodes })
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: (response) => {
          this.rolePermissionCodes.set(new Set(response.permissionCodes));
          this.snackBar.open('Permisos actualizados correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudieron actualizar los permisos');
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  protected saveRoleDescription(): void {
    if (this.roleDescriptionForm.invalid || this.busy()) {
      this.roleDescriptionForm.markAllAsTouched();
      return;
    }

    this.busy.set(true);
    this.adminAccessApi
      .updateRole(this.selectedRole(), {
        description: this.roleDescriptionForm.controls.description.getRawValue(),
      })
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: (role) => {
          this.roles.update((items) => items.map((item) => (item.id === role.id ? role : item)));
          this.snackBar.open('Descripción de rol actualizada.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo actualizar la descripción');
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  protected editPermission(permission: PermissionResponse): void {
    this.permissionFormError.set(null);
    this.permissionForm.patchValue({
      id: permission.id,
      code: permission.code,
      description: permission.description ?? '',
    });
  }

  protected resetPermissionForm(): void {
    this.permissionFormError.set(null);
    this.permissionForm.reset({ id: 0, code: '', description: '' });
  }

  protected savePermission(): void {
    if (this.permissionForm.invalid || this.busy()) {
      this.permissionForm.markAllAsTouched();
      return;
    }

    this.permissionFormError.set(null);
    this.busy.set(true);

    const payload = {
      code: this.permissionForm.controls.code.getRawValue(),
      description: this.permissionForm.controls.description.getRawValue(),
    };

    const id = this.permissionForm.controls.id.getRawValue();
    const request$ = id > 0 ? this.adminAccessApi.updatePermission(id, payload) : this.adminAccessApi.createPermission(payload);

    request$
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Permiso guardado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          this.resetPermissionForm();
          this.reloadPermissions();
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo guardar el permiso');
          this.permissionFormError.set(message);
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  protected deletePermission(permission: PermissionResponse): void {
    if (this.busy()) {
      return;
    }
    if (!window.confirm(`¿Eliminar permiso ${permission.code}?`)) {
      return;
    }

    this.busy.set(true);
    this.adminAccessApi
      .deletePermission(permission.id)
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Permiso eliminado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          this.reloadPermissions();
          this.loadRolePermissions(this.selectedRole());
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo eliminar el permiso');
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  protected toRoleLabel(role: UserRole): string {
    if (role === UserRole.ADMIN) {
      return 'Administrador';
    }
    if (role === UserRole.PROFESSOR) {
      return 'Profesor';
    }
    return 'Estudiante';
  }

  private reloadData(): void {
    this.loading.set(true);
    forkJoin({
      roles: this.adminAccessApi.roles(),
      permissions: this.adminAccessApi.permissions(),
      rolePermissions: this.adminAccessApi.rolePermissions(this.selectedRole()),
    })
      .pipe(
        take(1),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (response) => {
          this.roles.set(response.roles);
          this.permissions.set(response.permissions);
          this.rolePermissionCodes.set(new Set(response.rolePermissions.permissionCodes));
          const selected = response.roles.find((role) => role.name === this.selectedRole());
          this.roleDescriptionForm.patchValue({ description: selected?.description ?? '' });
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo cargar configuración de acceso');
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  private reloadPermissions(): void {
    this.adminAccessApi
      .permissions()
      .pipe(take(1))
      .subscribe({
        next: (permissions) => this.permissions.set(permissions),
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo refrescar permisos');
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  private loadRolePermissions(role: UserRole): void {
    this.adminAccessApi
      .rolePermissions(role)
      .pipe(take(1))
      .subscribe({
        next: (response) => this.rolePermissionCodes.set(new Set(response.permissionCodes)),
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudieron cargar permisos del rol');
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }
}
