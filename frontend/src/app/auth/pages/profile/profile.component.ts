import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { finalize, take } from 'rxjs';

import { APP_ROUTES, AUTH_UI, type UserRole } from '../../contracts/auth.contracts';
import { AuthApiService } from '../../services/auth-api.service';
import { AuthStateService } from '../../services/auth-state.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly authApi = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly busy = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly homeRoute = computed(() => this.authService.roleHome(this.authService.role()));
  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    username: [{ value: '', disabled: true }],
    role: [{ value: '', disabled: true }],
    password: ['', [Validators.minLength(6)]],
  });

  constructor() {
    const user = this.authState.user();
    if (!user) {
      return;
    }

    this.form.patchValue({
      name: user.name,
      username: user.username,
      role: this.toRoleLabel(user.role),
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }

    this.formError.set(null);
    this.busy.set(true);

    const payload = {
      name: this.form.controls.name.getRawValue(),
      password: this.form.controls.password.getRawValue() || null,
    };

    this.authApi
      .updateMe(payload)
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: (updatedUser) => {
          this.authState.patchSessionUser(updatedUser);
          this.form.patchValue({
            name: updatedUser.name,
            username: updatedUser.username,
            role: this.toRoleLabel(updatedUser.role),
            password: '',
          });
          this.snackBar.open('Perfil actualizado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo actualizar el perfil');
          this.formError.set(message);
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }

  private toRoleLabel(role: UserRole): string {
    if (role === 'ADMIN') {
      return 'Administrador';
    }
    if (role === 'PROFESSOR') {
      return 'Profesor';
    }
    return 'Estudiante';
  }

  protected readonly routes = APP_ROUTES;
}
