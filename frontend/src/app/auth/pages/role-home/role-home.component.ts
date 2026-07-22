import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { take } from 'rxjs';

import { APP_ROUTES, AUTH_UI, UserRole } from '../../contracts/auth.contracts';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-role-home',
  imports: [MatCardModule, MatButtonModule, MatSnackBarModule, RouterLink],
  templateUrl: './role-home.component.html',
  styleUrl: './role-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleHomeComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly role = computed(() => this.authService.role());
  protected readonly title = computed(() => {
    const role = this.role();
    if (role === UserRole.ADMIN) {
      return 'Panel de administrador';
    }
    if (role === UserRole.PROFESSOR) {
      return 'Panel de profesor';
    }
    return 'Panel de estudiante';
  });
  protected readonly username = computed(() => this.authService.currentSession()?.user?.username ?? 'desconocido');
  protected readonly isAdmin = computed(() => this.role() === UserRole.ADMIN);
  protected readonly routes = APP_ROUTES;

  protected logout(): void {
    this.authService
      .logout()
      .pipe(take(1))
      .subscribe(() => {
        this.snackBar.open('Sesion cerrada correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
        void this.router.navigateByUrl(APP_ROUTES.login);
      });
  }
}
