import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

import { APP_ROUTES } from '../../contracts/auth.contracts';

@Component({
  selector: 'app-forbidden',
  imports: [MatCardModule, RouterLink],
  templateUrl: './forbidden.component.html',
  styleUrl: './forbidden.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForbiddenComponent {
  protected readonly appRoute = APP_ROUTES.app;
}
