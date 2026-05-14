import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AsyncPipe, JsonPipe, NgFor, NgIf } from '@angular/common';
import { AuthService } from '../../../../../ng-auth-lib/src/public-api';

interface UserProfile {
  userId: string;
  username: string;
  email: string;
  roles: string[];
  permissions: string[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AsyncPipe, NgIf, NgFor, JsonPipe],
  template: `
    <div class="container" style="padding-top:2rem">
      <h1 class="page-header">Dashboard</h1>

      <div *ngIf="profile()" class="card">
        <h3 style="margin-bottom:1rem">Welcome, {{ profile()!.username }}! 👋</h3>

        <div class="info-grid">
          <div class="info-item">
            <label>User ID</label>
            <p><code>{{ profile()!.userId }}</code></p>
          </div>
          <div class="info-item">
            <label>Email</label>
            <p>{{ profile()!.email }}</p>
          </div>
          <div class="info-item">
            <label>Roles</label>
            <p>
              <span *ngFor="let r of profile()!.roles"
                    class="badge"
                    [class.badge-admin]="r === 'ROLE_ADMIN' || r === 'ADMIN'"
                    [class.badge-user]="r !== 'ROLE_ADMIN' && r !== 'ADMIN'">
                {{ r }}
              </span>
            </p>
          </div>
          <div class="info-item">
            <label>Permissions</label>
            <p style="font-size:.9rem;color:#555">{{ profile()!.permissions.join(', ') || '—' }}</p>
          </div>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger" style="margin-top:1rem">{{ error() }}</div>

      <div class="card" style="margin-top:1.5rem">
        <h3 style="margin-bottom:.75rem">Auth State (from ng-auth-lib)</h3>
        <pre style="background:#f5f5f5;padding:1rem;border-radius:4px;font-size:.85rem;overflow:auto">
{{ (authService.isAuthenticated$ | async) ? 'Authenticated ✓' : 'Not authenticated' }}</pre>
      </div>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  protected authService = inject(AuthService);
  private   http        = inject(HttpClient);

  profile = signal<UserProfile | null>(null);
  error   = signal('');

  ngOnInit(): void {
    this.http.get<UserProfile>('/api/users/me').subscribe({
      next:  p  => this.profile.set(p),
      error: () => this.error.set('Failed to load profile from backend.'),
    });
  }
}
