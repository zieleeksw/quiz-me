import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService } from './course-studio.service';

@Component({
  selector: 'app-course-studio-page',
  imports: [RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-studio-page.component.html',
  styleUrl: './course-studio-page.component.scss'
})
export class CourseStudioPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);

  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly editorLink = ['/courses', this.courseSlug, 'editor'];

  constructor() {
    this.coursesCatalogService.loadCourses();
  }
}
