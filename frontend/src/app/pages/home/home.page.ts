import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AUTH_ROUTE_URLS, UserRole } from '../../auth/auth.constants';
import { SessionService } from '../../auth/session.service';

@Component({
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.page.html',
  styleUrl: './home.page.scss'
})
export class HomePage {
  protected readonly isLoggingOut = signal(false);
  protected readonly title = computed(() => {
    const role = this.sessionService.role();

    if (role === UserRole.ADMIN) {
      return 'Panel administrador';
    }

    if (role === UserRole.PROFESSOR) {
      return 'Panel profesor';
    }

    return 'Panel estudiante';
  });

  constructor(
    public readonly sessionService: SessionService,
    private readonly router: Router
  ) {}

  protected logout(): void {
    if (this.isLoggingOut()) {
      return;
    }

    this.isLoggingOut.set(true);
    this.sessionService
      .logout()
      .pipe(finalize(() => this.isLoggingOut.set(false)))
      .subscribe(() => {
        void this.router.navigateByUrl(AUTH_ROUTE_URLS.LOGIN);
      });
  }
}
