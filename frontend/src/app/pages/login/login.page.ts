import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AUTH_MESSAGES, AUTH_ROUTE_URLS } from '../../auth/auth.constants';
import { getApiErrorMessage } from '../../auth/auth.error';
import { SessionService } from '../../auth/session.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss'
})
export class LoginPage {
  protected readonly registerUrl = AUTH_ROUTE_URLS.REGISTER;
  protected readonly isSubmitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly sessionService: SessionService,
    private readonly router: Router
  ) {
    this.form = this.formBuilder.nonNullable.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.error.set(null);
    this.isSubmitting.set(true);

    this.sessionService
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl(this.sessionService.homeUrlByRole());
        },
        error: (error) => {
          this.error.set(getApiErrorMessage(error, AUTH_MESSAGES.LOGIN_ERROR));
        }
      });
  }
}
