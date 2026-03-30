import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { HomePageComponent } from './features/home/home-page.component';
import { DashboardPageComponent } from './features/dashboard/dashboard-page.component';
import { LoginPageComponent } from './features/auth/login-page.component';
import { RegisterPageComponent } from './features/auth/register-page.component';
import { CourseStudioPageComponent } from './features/course-studio/course-studio-page.component';
import { CourseStudioEditorPageComponent } from './features/course-studio/course-studio-editor-page.component';

export const routes: Routes = [
  {
    path: '',
    component: HomePageComponent,
    canActivate: [guestGuard]
  },
  {
    path: 'login',
    component: LoginPageComponent,
    canActivate: [guestGuard]
  },
  {
    path: 'register',
    component: RegisterPageComponent,
    canActivate: [guestGuard]
  },
  {
    path: 'courses',
    component: DashboardPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'courses/:courseSlug',
    component: CourseStudioPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'courses/:courseSlug/editor',
    component: CourseStudioEditorPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'studio',
    redirectTo: 'courses/spring-boot-associate',
    pathMatch: 'full'
  },
  {
    path: 'studio/editor',
    redirectTo: 'courses/spring-boot-associate/editor',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
