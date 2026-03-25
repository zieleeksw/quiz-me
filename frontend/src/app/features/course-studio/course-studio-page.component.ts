import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CourseStudioService } from './course-studio.service';

@Component({
  selector: 'app-course-studio-page',
  imports: [RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-studio-page.component.html',
  styleUrl: './course-studio-page.component.scss'
})
export class CourseStudioPageComponent {
  readonly studio = inject(CourseStudioService);
}
