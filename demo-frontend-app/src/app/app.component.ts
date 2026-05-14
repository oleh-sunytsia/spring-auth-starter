import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AsyncPipe, NgIf } from '@angular/common';
import { AuthService } from '../../../ng-auth-lib/src/public-api';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, AsyncPipe, NgIf],
  template: `
    <nav class="navbar" *ngIf="(authService.isAuthenticated$ | async)">
      <span class="brand">🔐 Benatti Auth Demo</span>
      <a routerLink="/dashboard">Dashboard</a>
      <a routerLink="/profile">Profile</a>
      <a routerLink="/admin">Admin</a>
      <button class="logout-btn" (click)="logout()">Logout</button>
    </nav>
    <router-outlet />
  `,
})
export class AppComponent {
  protected authService = inject(AuthService);

  logout(): void {
    // logout() is synchronous — it clears tokens and navigates to login
    this.authService.logout();
  }
}
