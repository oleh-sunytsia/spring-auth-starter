import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgIf } from '@angular/common';
import { AuthService } from '../../../../../ng-auth-lib/src/public-api';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, NgIf],
  template: `
    <div class="center">
      <div class="card" style="width:380px">
        <h2 style="margin-bottom:1.5rem;color:#1a237e">Sign In</h2>

        <div class="alert alert-info" style="margin-bottom:1rem">
          <strong>Demo accounts:</strong><br>
          <code>alice / password</code> &nbsp;(USER role)<br>
          <code>bob / password</code> &nbsp;(USER + ADMIN role)
        </div>

        <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="username">Username</label>
            <input id="username" class="form-control" [(ngModel)]="username"
                   name="username" required autocomplete="username">
          </div>

          <div class="form-group">
            <label for="password">Password</label>
            <input id="password" class="form-control" type="password"
                   [(ngModel)]="password" name="password"
                   required autocomplete="current-password">
          </div>

          <button class="btn btn-primary" style="width:100%" type="submit"
                  [disabled]="loading()">
            {{ loading() ? 'Signing in…' : 'Sign In' }}
          </button>
        </form>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private authService: AuthService = inject(AuthService);
  private router: Router           = inject(Router);

  username = '';
  password = '';
  loading  = signal(false);
  error    = signal('');

  onSubmit(): void {
    if (!this.username || !this.password) return;

    this.loading.set(true);
    this.error.set('');

    this.authService.login(this.username, this.password)
      .subscribe({
        next:  () => this.router.navigate(['/dashboard']),
        error: (err: any) => {
          this.error.set(err?.error?.detail ?? err?.message ?? 'Login failed. Check credentials.');
          this.loading.set(false);
        },
      });
  }
}
