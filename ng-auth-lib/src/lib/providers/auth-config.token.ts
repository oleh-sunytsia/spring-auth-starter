import { InjectionToken } from '@angular/core';
import { ResolvedAuthConfig } from '../models/auth-config.model';

export const AUTH_CONFIG = new InjectionToken<ResolvedAuthConfig>('AUTH_CONFIG');
