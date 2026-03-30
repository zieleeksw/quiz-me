import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CourseCatalogItem, CoursesCatalogService } from './courses-catalog.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, ReactiveFormsModule, DatePipe, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  readonly coursesCatalogService = inject(CoursesCatalogService);

  readonly courses = this.coursesCatalogService.courses;
  readonly isLoading = this.coursesCatalogService.isLoading;
  readonly isLoaded = this.coursesCatalogService.isLoaded;
  readonly loadError = this.coursesCatalogService.error;

  readonly editorMode = signal<'create' | 'edit'>('create');
  readonly editingCourseId = signal<number | null>(null);
  readonly isEditorVisible = signal(false);
  readonly submitAttempted = signal(false);
  readonly isSaving = signal(false);
  readonly formMessage = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string>>({});

  readonly courseForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
  });

  constructor() {
    this.coursesCatalogService.loadCourses();

    this.courseForm.valueChanges.subscribe(() => {
      this.fieldErrors.set({});
      this.formMessage.set(null);
    });
  }

  openCreateEditor(): void {
    this.editorMode.set('create');
    this.editingCourseId.set(null);
    this.isEditorVisible.set(true);
    this.submitAttempted.set(false);
    this.fieldErrors.set({});
    this.formMessage.set(null);
    this.courseForm.reset({
      name: '',
      description: ''
    });
  }

  openEditEditor(course: CourseCatalogItem): void {
    this.editorMode.set('edit');
    this.editingCourseId.set(course.id);
    this.isEditorVisible.set(true);
    this.submitAttempted.set(false);
    this.fieldErrors.set({});
    this.formMessage.set(null);
    this.courseForm.reset({
      name: course.title,
      description: course.subtitle
    });
  }

  cancelEditor(): void {
    this.isEditorVisible.set(false);
    this.editingCourseId.set(null);
    this.submitAttempted.set(false);
    this.fieldErrors.set({});
    this.formMessage.set(null);
    this.courseForm.reset({
      name: '',
      description: ''
    });
  }

  saveCourse(): void {
    this.submitAttempted.set(true);
    this.fieldErrors.set({});
    this.formMessage.set(null);

    if (this.courseForm.invalid) {
      this.courseForm.markAllAsTouched();
      return;
    }

    const payload = this.courseForm.getRawValue();
    const courseId = this.editingCourseId();
    const request$ =
      this.editorMode() === 'edit' && typeof courseId === 'number'
        ? this.coursesCatalogService.updateCourse(courseId, payload)
        : this.coursesCatalogService.createCourse(payload);

    this.isSaving.set(true);

    request$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.cancelEditor();
      },
      error: (error: unknown) => {
        this.isSaving.set(false);
        this.fieldErrors.set(extractFieldErrors(error));
        this.formMessage.set(extractApiMessage(error) ?? 'Unable to save this course right now.');
      }
    });
  }

  canEditCourse(course: CourseCatalogItem): boolean {
    return this.authService.user()?.id === course.ownerUserId;
  }

  resolveOwnerLabel(course: CourseCatalogItem): string {
    return this.canEditCourse(course) ? 'You' : `User #${course.ownerUserId}`;
  }

  resolveCourseLink(course: CourseCatalogItem): string[] {
    return this.coursesCatalogService.buildCourseLink(course);
  }

  hasFieldError(fieldName: 'name' | 'description'): boolean {
    return Boolean(this.submitAttempted() && (this.courseForm.controls[fieldName].invalid || this.fieldErrors()[fieldName]));
  }

  fieldError(fieldName: 'name' | 'description'): string | null {
    if (!this.submitAttempted()) {
      return null;
    }

    if (this.fieldErrors()[fieldName]) {
      return this.fieldErrors()[fieldName];
    }

    const control = this.courseForm.controls[fieldName];

    if (control.hasError('required')) {
      return fieldName === 'name' ? 'Course name cannot be empty.' : 'Course description cannot be empty.';
    }

    if (control.hasError('minlength')) {
      return fieldName === 'name'
        ? 'Course name is too short. Min length is 3 characters.'
        : 'Course description is too short. Min length is 10 characters.';
    }

    if (control.hasError('maxlength')) {
      return fieldName === 'name'
        ? 'Course name is too long. Max length is 120 characters.'
        : 'Course description is too long. Max length is 1000 characters.';
    }

    return null;
  }

  trackCourse(index: number, course: CourseCatalogItem): string {
    return `${index}-${course.id}`;
  }
}
