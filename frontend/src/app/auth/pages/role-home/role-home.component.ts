import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { take } from 'rxjs';

import { APP_ROUTES, AUTH_UI } from '../../contracts/auth.contracts';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-role-home',
  imports: [MatCardModule, MatButtonModule, MatSnackBarModule],
  templateUrl: './role-home.component.html',
  styleUrl: './role-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleHomeComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly title = computed(() => this.route.snapshot.data['title'] as string);
  protected readonly username = computed(() => this.authService.currentSession()?.user?.username ?? 'unknown');

  protected logout(): void {
    this.authService
      .logout()
      .pipe(take(1))
      .subscribe(() => {
        this.snackBar.open('Session closed.', 'Close', { duration: AUTH_UI.snackbarDurationMs });
        void this.router.navigateByUrl(APP_ROUTES.login);
      });
  }
}
