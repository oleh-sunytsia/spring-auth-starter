import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideAuth } from '../../ng-auth-lib/src/public-api';

import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, {
  providers: [
    provideAnimations(),
    provideRouter(routes),

    /**
     * Wire the ng-auth-lib with the demo backend.
     * The Angular dev-server proxies /api → http://localhost:8080
     * (see proxy.conf.json), so we use relative URLs here.
     */
    provideAuth({
      apiEndpoint:  '',               // empty = same origin (proxied)
      loginUrl:     '/api/auth/login',
      refreshUrl:   '/api/auth/refresh',
      logoutUrl:    '/api/auth/logout',
      validateUrl:  '/api/auth/validate',
      blacklistedUrls: [
        '/api/auth/login',
        '/api/auth/refresh',
      ],
      loginRedirectUrl:   '/login',
      defaultRedirectUrl: '/dashboard',
    }),
  ],
}).catch(err => console.error(err));
