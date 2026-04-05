import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { pendingChangesGuard } from './core/navigation/pending-changes.guard';
import { HomePageComponent } from './features/home/home-page.component';
import { DashboardPageComponent } from './features/dashboard/dashboard-page.component';
import { LoginPageComponent } from './features/auth/login-page.component';
import { RegisterPageComponent } from './features/auth/register-page.component';
import { CourseSummaryPageComponent } from './features/course-studio/course-summary-page.component';
import { CourseStudioPageComponent } from './features/course-studio/course-studio-page.component';
import { CourseStudioEditorPageComponent } from './features/course-studio/course-studio-editor-page.component';
import { CourseQuestionEditorPageComponent } from './features/course-studio/course-question-editor-page.component';
import { CourseQuizEditorPageComponent } from './features/course-studio/course-quiz-editor-page.component';
import { CourseQuizPlayPageComponent } from './features/course-studio/course-quiz-play-page.component';

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
    component: CourseSummaryPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'courses/:courseSlug/quizzes',
    component: CourseStudioPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'courses/:courseSlug/editor',
    component: CourseStudioEditorPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'courses/:courseSlug/quizzes/:quizId/play',
    component: CourseQuizPlayPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'courses/:courseSlug/editor/quizzes',
    component: CourseStudioEditorPageComponent,
    canActivate: [authGuard]
  },
  {
    path: 'courses/:courseSlug/editor/questions/new',
    component: CourseQuestionEditorPageComponent,
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard]
  },
  {
    path: 'courses/:courseSlug/editor/questions/:questionId/edit',
    component: CourseQuestionEditorPageComponent,
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard]
  },
  {
    path: 'courses/:courseSlug/editor/quizzes/new',
    component: CourseQuizEditorPageComponent,
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard]
  },
  {
    path: 'courses/:courseSlug/editor/quizzes/:quizId/edit',
    component: CourseQuizEditorPageComponent,
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard]
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
