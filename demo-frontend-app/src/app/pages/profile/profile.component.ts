import { Component, inject, OnInit, signal } from '@angular/core';
import { NgIf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService, TokenDecoderService, TokenStorageService } from '../../../../../ng-auth-lib/src/public-api';

interface TokenInfo {
  userId: string;
  username: string;
  lastLogin: string;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [NgIf],
  template: `
    <div class="container" style="padding-top:2rem">
      <h1 class="page-header">My Profile</h1>

      <div class="card">
        <h3 style="margin-bottom:1rem">Local Token Claims</h3>
        <p class="alert alert-info" style="font-size:.85rem">
          These values are decoded client-side from the JWT stored in localStorage —
          no round-trip to the server required.
        </p>

        <div class="info-grid" style="margin-top:1rem">
          <div class="info-item">
            <label>Sub (userId)</label>
            <p><code>{{ localPayload()?.['sub'] ?? '—' }}</code></p>
          </div>
          <div class="info-item">
            <label>Username</label>
            <p>{{ localPayload()?.['username'] ?? '—' }}</p>
          </div>
          <div class="info-item">
            <label>Email</label>
            <p>{{ localPayload()?.['email'] ?? '—' }}</p>
          </div>
          <div class="info-item">
            <label>Expires</label>
            <p>{{ expiresAt() ?? '—' }}</p>
          </div>
        </div>
      </div>

      <div class="card" style="margin-top:1.5rem">
        <h3 style="margin-bottom:1rem">Server Token Info</h3>
        <div *ngIf="serverInfo()" class="info-grid">
          <div class="info-item">
            <label>User ID</label>
            <p><code>{{ serverInfo()!.userId }}</code></p>
          </div>
          <div class="info-item">
            <label>Username</label>
            <p>{{ serverInfo()!.username }}</p>
          </div>
          <div class="info-item">
            <label>Last Login</label>
            <p>{{ serverInfo()!.lastLogin }}</p>
          </div>
        </div>
        <div *ngIf="serverError()" class="alert alert-danger">{{ serverError() }}</div>
      </div>
    </div>
  `,
})
export class ProfileComponent implements OnInit {
  private decoder: TokenDecoderService      = inject(TokenDecoderService);
  private tokenStorage: TokenStorageService = inject(TokenStorageService);
  private http: HttpClient                  = inject(HttpClient);

  localPayload = signal<Record<string, unknown> | null>(null);
  expiresAt    = signal<string | null>(null);
  serverInfo   = signal<TokenInfo | null>(null);
  serverError  = signal('');

  ngOnInit(): void {
    const token = this.tokenStorage.getAccessToken();
    if (token) {
      const payload = this.decoder.decodePayload(token);
      this.localPayload.set(payload as Record<string, unknown> | null);

      if (payload) {
        const secs = this.decoder.secondsUntilExpiry(token);
        const d = new Date(Date.now() + secs * 1000);
        this.expiresAt.set(d.toLocaleString());
      }
    }

    this.http.get<TokenInfo>('/api/users/token-info').subscribe({
      next:  info => this.serverInfo.set(info),
      error: ()   => this.serverError.set('Failed to load token info from backend.'),
    });
  }
}

