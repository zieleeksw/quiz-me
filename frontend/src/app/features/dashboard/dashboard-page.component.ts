import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

type CoursePreview = {
  title: string;
  subtitle: string;
  questionCount: number;
  quizCount: number;
  completion: number;
  status: 'available' | 'coming-soon';
  route: string | null;
  accent: 'primary' | 'secondary';
};

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent {
  readonly courses: CoursePreview[] = [
    {
      title: 'Spring Boot Associate',
      subtitle: 'Architecture, web, security, persistence, testing, and real certification-style drills.',
      questionCount: 214,
      quizCount: 6,
      completion: 64,
      status: 'available',
      route: '/studio',
      accent: 'secondary'
    },
    {
      title: 'Spring Security Deep Dive',
      subtitle: 'Authorization flows, filter chains, JWTs, method security, and common exam traps.',
      questionCount: 138,
      quizCount: 4,
      completion: 0,
      status: 'coming-soon',
      route: null,
      accent: 'primary'
    },
    {
      title: 'Docker For Java Engineers',
      subtitle: 'Images, layers, networks, compose flows, and production-minded runtime fundamentals.',
      questionCount: 96,
      quizCount: 3,
      completion: 0,
      status: 'coming-soon',
      route: null,
      accent: 'primary'
    }
  ];

}
