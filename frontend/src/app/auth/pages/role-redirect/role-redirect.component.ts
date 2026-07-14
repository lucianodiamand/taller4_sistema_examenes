import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-role-redirect',
  template: '',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleRedirectComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  constructor() {
    effect(() => {
      void this.router.navigateByUrl(this.authService.roleHome(this.authService.role()));
    });
  }
}
