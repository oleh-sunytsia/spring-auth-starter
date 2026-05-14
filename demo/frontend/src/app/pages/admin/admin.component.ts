import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NgFor, NgIf, JsonPipe } from '@angular/common';

interface AdminStats {
  totalUsers: number;
  activeSessionsEst: number;
  serverTime: string;
  message: string;
}

interface AdminUser {
  userId: string;
  username: string;
  email: string;
  roles: string[];
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [NgIf, NgFor, JsonPipe],
  template: `
    <div class="container" style="padding-top:2rem">
      <h1 class="page-header">Admin Panel</h1>

      <div *ngIf="stats()" class="card">
        <h3 style="margin-bottom:1rem">Server Stats</h3>
        <div class="info-grid">
          <div class="info-item">
            <label>Total Users</label>
            <p style="font-size:2rem;font-weight:700;color:#1a237e">
              {{ stats()!.totalUsers }}
            </p>
          </div>
          <div class="info-item">
            <label>Est. Active Sessions</label>
            <p style="font-size:2rem;font-weight:700;color:#3949ab">
              {{ stats()!.activeSessionsEst }}
            </p>
          </div>
          <div class="info-item" style="grid-column:span 2">
            <label>Server Time</label>
            <p>{{ stats()!.serverTime }}</p>
          </div>
        </div>
      </div>

      <div *ngIf="statsError()" class="alert alert-danger" style="margin-top:1rem">
        {{ statsError() }}
      </div>

      <div class="card" style="margin-top:1.5rem">
        <h3 style="margin-bottom:1rem">All Users</h3>
        <table style="width:100%;border-collapse:collapse;font-size:.95rem" *ngIf="users().length">
          <thead>
            <tr style="border-bottom:2px solid #e8eaf6">
              <th style="text-align:left;padding:.6rem">Username</th>
              <th style="text-align:left;padding:.6rem">Email</th>
              <th style="text-align:left;padding:.6rem">Roles</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let u of users()" style="border-bottom:1px solid #f0f2f5">
              <td style="padding:.6rem"><code>{{ u.username }}</code></td>
              <td style="padding:.6rem">{{ u.email }}</td>
              <td style="padding:.6rem">
                <span *ngFor="let r of u.roles" class="badge"
                      [class.badge-admin]="r === 'ADMIN' || r === 'ROLE_ADMIN'"
                      [class.badge-user]="r !== 'ADMIN' && r !== 'ROLE_ADMIN'">
                  {{ r }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <div *ngIf="usersError()" class="alert alert-danger">{{ usersError() }}</div>
      </div>
    </div>
  `,
})
export class AdminComponent implements OnInit {
  private http = inject(HttpClient);

  stats      = signal<AdminStats | null>(null);
  statsError = signal('');
  users      = signal<AdminUser[]>([]);
  usersError = signal('');

  ngOnInit(): void {
    this.http.get<AdminStats>('/api/admin/stats').subscribe({
      next:  s  => this.stats.set(s),
      error: () => this.statsError.set('Access denied or server error.'),
    });

    this.http.get<AdminUser[]>('/api/admin/users').subscribe({
      next:  u  => this.users.set(u),
      error: () => this.usersError.set('Failed to load user list.'),
    });
  }
}
