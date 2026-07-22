import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { finalize, take } from 'rxjs';

import { APP_ROUTES, AUTH_UI, UserRole, type UserAdminResponse } from '../../contracts/auth.contracts';
import { AdminUsersApiService } from '../../services/admin-users-api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-users',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminUsersComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly adminUsersApi = inject(AdminUsersApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly authService = inject(AuthService);

  protected readonly users = signal<UserAdminResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly editingUserId = signal<number | null>(null);
  protected readonly query = signal('');
  protected readonly formError = signal<string | null>(null);

  protected readonly routes = APP_ROUTES;
  protected readonly managedRoles: readonly UserRole[] = [UserRole.PROFESSOR, UserRole.STUDENT];
  protected readonly visibleUsers = computed(() => {
    const term = this.query().trim().toLowerCase();
    if (!term) {
      return this.users();
    }

    return this.users().filter((user) => {
      const roleLabel = this.toRoleLabel(user.role).toLowerCase();
      return (
        user.name.toLowerCase().includes(term) ||
        user.username.toLowerCase().includes(term) ||
        roleLabel.includes(term)
      );
    });
  });

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    username: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]],
    role: [UserRole.PROFESSOR, Validators.required],
  });

  constructor() {
    this.reloadUsers();
  }

  protected setQuery(value: string): void {
    this.query.set(value);
  }

  protected startCreate(): void {
    this.editingUserId.set(null);
    this.formError.set(null);
    this.form.reset({
      name: '',
      username: '',
      password: '',
      role: UserRole.PROFESSOR,
    });
    this.form.controls.username.enable();
    this.form.controls.password.enable();
  }

  protected startEdit(user: UserAdminResponse): void {
    this.editingUserId.set(user.id);
    this.formError.set(null);
    this.form.reset({
      name: user.name,
      username: user.username,
      password: '',
      role: user.role,
    });
    this.form.controls.username.disable();
    this.form.controls.password.disable();
  }

  protected cancelEdit(): void {
    this.startCreate();
  }

  protected submit(): void {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }

    this.formError.set(null);
    this.busy.set(true);

    const userId = this.editingUserId();
    if (userId === null) {
      this.adminUsersApi
        .create({
          name: this.form.controls.name.getRawValue(),
          username: this.form.controls.username.getRawValue(),
          password: this.form.controls.password.getRawValue(),
          role: this.form.controls.role.getRawValue(),
        })
        .pipe(
          take(1),
          finalize(() => this.busy.set(false))
        )
        .subscribe({
          next: () => {
            this.snackBar.open('Usuario creado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
            this.startCreate();
            this.reloadUsers();
          },
          error: (error: unknown) => {
            const message = this.authService.errorMessage(error, 'No se pudo crear el usuario');
            this.formError.set(message);
            this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          },
        });
      return;
    }

    this.adminUsersApi
      .update(userId, {
        name: this.form.controls.name.getRawValue(),
        role: this.form.controls.role.getRawValue(),
      })
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Usuario actualizado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          this.startCreate();
          this.reloadUsers();
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo actualizar el usuario');
          this.formError.set(message);
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  protected remove(user: UserAdminResponse): void {
    if (!user.manageable || this.busy()) {
      return;
    }

    const confirmed = window.confirm(`¿Eliminar usuario ${user.username}?`);
    if (!confirmed) {
      return;
    }

    this.busy.set(true);
    this.adminUsersApi
      .delete(user.id)
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Usuario eliminado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          this.reloadUsers();
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo eliminar el usuario');
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

  private reloadUsers(): void {
    this.loading.set(true);
    this.adminUsersApi
      .findAll()
      .pipe(
        take(1),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (users) => {
          this.users.set(users);
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo cargar el listado de usuarios');
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }
}
