import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, map, of, tap } from 'rxjs';

type CourseApiDto = {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  ownerUserId: number;
  questionCount: number;
  quizCount: number;
  progressPercent: number;
};

export type CourseCatalogItem = {
  id: number;
  slug: string;
  title: string;
  subtitle: string;
  createdAt: string;
  ownerUserId: number;
  questionCount: number;
  quizCount: number;
  progressPercent: number;
};

export type SaveCoursePayload = {
  name: string;
  description: string;
};

@Injectable({ providedIn: 'root' })
export class CoursesCatalogService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;

  private readonly coursesState = signal<CourseCatalogItem[]>([]);
  private readonly loadingState = signal(false);
  private readonly loadedState = signal(false);
  private readonly errorState = signal<string | null>(null);

  readonly courses = this.coursesState.asReadonly();
  readonly isLoading = this.loadingState.asReadonly();
  readonly isLoaded = this.loadedState.asReadonly();
  readonly error = this.errorState.asReadonly();
  readonly hasCourses = computed(() => this.coursesState().length > 0);

  loadCourses(force = false): void {
    if (this.loadingState()) {
      return;
    }

    if (this.loadedState() && !force) {
      return;
    }

    this.loadingState.set(true);
    this.errorState.set(null);

    this.http
      .get<CourseApiDto[]>(`${this.apiBaseUrl}/courses`)
      .pipe(
        map((courses) => courses.map((course) => this.mapCourse(course))),
        tap((courses) => {
          this.coursesState.set(courses);
          this.loadedState.set(true);
        }),
        catchError(() => {
          this.errorState.set('Unable to load courses right now.');
          this.loadedState.set(true);
          return of([] as CourseCatalogItem[]);
        })
      )
      .subscribe({
        complete: () => {
          this.loadingState.set(false);
        }
      });
  }

  createCourse(payload: SaveCoursePayload) {
    return this.http.post<CourseApiDto>(`${this.apiBaseUrl}/courses`, payload).pipe(
      map((course) => this.mapCourse(course)),
      tap((course) => {
        this.coursesState.update((courses) => [course, ...courses]);
      })
    );
  }

  updateCourse(courseId: number, payload: SaveCoursePayload) {
    return this.http.put<CourseApiDto>(`${this.apiBaseUrl}/courses/${courseId}`, payload).pipe(
      map((course) => this.mapCourse(course)),
      tap((updatedCourse) => {
        this.coursesState.update((courses) =>
          courses.map((course) => (course.id === updatedCourse.id ? updatedCourse : course))
        );
      })
    );
  }

  findBySlug(courseSlug: string | null): CourseCatalogItem | null {
    if (!courseSlug) {
      return null;
    }

    const prefixedId = Number(courseSlug.split('-', 1)[0]);

    if (!Number.isNaN(prefixedId)) {
      return this.coursesState().find((course) => course.id === prefixedId) ?? null;
    }

    return this.coursesState().find((course) => course.slug === courseSlug) ?? null;
  }

  buildCourseLink(course: CourseCatalogItem): string[] {
    return ['/courses', course.slug];
  }

  buildEditorLink(course: CourseCatalogItem): string[] {
    return ['/courses', course.slug, 'editor'];
  }

  private mapCourse(course: CourseApiDto): CourseCatalogItem {
    return {
      id: course.id,
      slug: `${course.id}-${this.slugify(course.name)}`,
      title: course.name,
      subtitle: course.description,
      createdAt: course.createdAt,
      ownerUserId: course.ownerUserId,
      questionCount: course.questionCount,
      quizCount: course.quizCount,
      progressPercent: course.progressPercent
    };
  }

  private slugify(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFKD')
      .replace(/[^\w\s-]/g, '')
      .trim()
      .replace(/[\s_-]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }
}
