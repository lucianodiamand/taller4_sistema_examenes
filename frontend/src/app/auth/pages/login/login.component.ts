import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { finalize, take } from 'rxjs';

import { APP_ROUTES, AUTH_UI } from '../../contracts/auth.contracts';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly routes = APP_ROUTES;
  protected readonly busy = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly form = this.formBuilder.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }

    this.formError.set(null);
    this.busy.set(true);

    this.authService
      .login(this.form.getRawValue())
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Sesion iniciada correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          void this.router.navigateByUrl(this.authService.roleHome(this.authService.role()));
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo iniciar sesion');
          this.formError.set(message);
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }
}
