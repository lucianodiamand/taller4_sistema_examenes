import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';

import { AUTH_MESSAGES, AUTH_ROUTE_URLS, PASSWORD_MIN_LENGTH } from '../../auth/auth.constants';
import { getApiErrorMessage } from '../../auth/auth.error';
import { SessionService } from '../../auth/session.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss'
})
export class RegisterPage {
  protected readonly loginUrl = AUTH_ROUTE_URLS.LOGIN;
  protected readonly isSubmitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly sessionService: SessionService,
    private readonly router: Router
  ) {
    this.form = this.formBuilder.nonNullable.group({
      name: ['', Validators.required],
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(PASSWORD_MIN_LENGTH)]]
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, username, password } = this.form.getRawValue();
    this.error.set(null);
    this.isSubmitting.set(true);

    this.sessionService
      .register({ name, username, password })
      .pipe(
        switchMap(() => this.sessionService.login({ username, password })),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl(this.sessionService.homeUrlByRole());
        },
        error: (error) => {
          this.error.set(getApiErrorMessage(error, AUTH_MESSAGES.REGISTER_ERROR));
        }
      });
  }
}
