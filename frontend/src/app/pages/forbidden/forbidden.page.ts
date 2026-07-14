import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AUTH_ROUTE_URLS } from '../../auth/auth.constants';

@Component({
  standalone: true,
  imports: [RouterLink],
  templateUrl: './forbidden.page.html',
  styleUrl: './forbidden.page.scss'
})
export class ForbiddenPage {
  protected readonly homeUrl = AUTH_ROUTE_URLS.APP;
}
