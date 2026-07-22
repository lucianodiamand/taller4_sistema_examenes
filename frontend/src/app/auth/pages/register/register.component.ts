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
  selector: 'app-register',
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
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly routes = APP_ROUTES;
  protected readonly busy = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    username: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }

    this.formError.set(null);
    this.busy.set(true);

    this.authService
      .register(this.form.getRawValue())
      .pipe(
        take(1),
        finalize(() => this.busy.set(false))
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Cuenta creada. Ahora inicia sesion.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          void this.router.navigateByUrl(APP_ROUTES.login);
        },
        error: (error: unknown) => {
          const message = this.authService.errorMessage(error, 'No se pudo crear la cuenta');
          this.formError.set(message);
          this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        },
      });
  }
}
