import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, HostListener, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioQuestion } from './course-studio.service';

@Component({
  selector: 'app-course-studio-editor-page',
  imports: [DatePipe, RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-studio-editor-page.component.html',
  styleUrl: './course-studio-editor-page.component.scss'
})
export class CourseStudioEditorPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);
  readonly studio = inject(CourseStudioService);

  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly studioLink = ['/courses', this.courseSlug];
  readonly addQuestionLink = ['/courses', this.courseSlug, 'editor', 'questions', 'new'];
  readonly addQuizLink = ['/courses', this.courseSlug, 'editor', 'quizzes', 'new'];
  readonly activeTab = signal<'questions' | 'quizzes' | 'categories'>(
    this.route.snapshot.url.some((segment) => segment.path === 'quizzes') ? 'quizzes' : 'questions'
  );

  readonly questionPreviewSearch = signal('');
  readonly questionPreviewCategoryFilter = signal<number | 'All'>('All');
  readonly questionPreviewRequestedPage = signal(0);
  readonly newManagedCategory = signal('');
  readonly categoryMessage = signal('');
  readonly editingCategoryId = signal<number | null>(null);
  readonly editingCategoryDraft = signal('');
  readonly isSavingCategory = signal(false);
  readonly quizMessage = signal('');
  readonly isDeletingQuiz = signal<number | null>(null);
  readonly archiveConfirmationQuizId = signal<number | null>(null);

  readonly questionPreviewQuestions = computed(() => this.studio.questionPreviewItems());
  readonly questionPreviewPageLabel = computed(() => {
    if (!this.studio.questionPreviewTotalPages()) {
      return 'Page 0 of 0';
    }

    return `Page ${this.studio.questionPreviewPageNumber() + 1} of ${this.studio.questionPreviewTotalPages()}`;
  });
  readonly canGoToPreviousQuestionPreviewPage = computed(() => this.studio.questionPreviewPageNumber() > 0);
  readonly canGoToNextQuestionPreviewPage = computed(
    () => this.studio.questionPreviewTotalPages() > 0 && this.studio.questionPreviewPageNumber() < this.studio.questionPreviewTotalPages() - 1
  );
  readonly quizPreviewCards = computed(() => {
    const questions = this.studio.questions();

    return this.studio.allQuizzes().map((quiz) => {
      const manualQuestions =
        quiz.mode === 'manual'
          ? quiz.questionIds
              .map((questionId) => questions.find((question) => question.id === questionId))
              .filter((question): question is StudioQuestion => Boolean(question))
          : [];
      const previewQuestions =
        quiz.mode === 'manual'
          ? manualQuestions.slice(0, 3)
          : quiz.mode === 'random'
            ? questions.slice(0, Math.min(quiz.randomCount ?? 3, 3))
            : questions
                .filter((question) => question.categories.some((category) => quiz.categories.some((quizCategory) => quizCategory.id === category.id)))
                .slice(0, 3);
      const previewCategoryNames = quiz.mode === 'category' ? quiz.categories.map((category) => category.name) : [];

      return {
        ...quiz,
        previewQuestions,
        previewCategoryNames
      };
    });
  });

  constructor() {
    this.coursesCatalogService.loadCourses();

    effect(() => {
      const course = this.currentCourse();

      if (course) {
        this.studio.loadCourseContext(course.id);
      }
    });

    effect(() => {
      const course = this.currentCourse();

      if (!course) {
        return;
      }

      const previewCategoryId = this.questionPreviewCategoryFilter();

      this.studio.loadQuestionPreview(
        course.id,
        this.questionPreviewRequestedPage(),
        5,
        this.questionPreviewSearch(),
        previewCategoryId === 'All' ? null : previewCategoryId
      ).subscribe();
    });

    effect(() => {
      const categories = this.studio.categories();
      const availableIds = new Set(categories.map((category) => category.id));
      const activeQuestionPreviewFilter = this.questionPreviewCategoryFilter();

      if (activeQuestionPreviewFilter !== 'All' && !availableIds.has(activeQuestionPreviewFilter)) {
        this.questionPreviewCategoryFilter.set('All');
      }
    });
  }

  setActiveTab(tab: 'questions' | 'quizzes' | 'categories'): void {
    this.activeTab.set(tab);
  }

  questionEditLink(questionId: number): unknown[] {
    return ['/courses', this.courseSlug, 'editor', 'questions', questionId, 'edit'];
  }

  quizEditLink(quizId: number): unknown[] {
    return ['/courses', this.courseSlug, 'editor', 'quizzes', quizId, 'edit'];
  }

  requestQuizArchive(quizId: number): void {
    const quiz = this.studio.findQuizById(quizId);

    if (!quiz?.active) {
      return;
    }

    this.archiveConfirmationQuizId.set(quizId);
  }

  cancelQuizArchive(): void {
    if (this.isDeletingQuiz() !== null) {
      return;
    }

    this.archiveConfirmationQuizId.set(null);
  }

  confirmQuizArchive(): void {
    const quizId = this.archiveConfirmationQuizId();

    if (!quizId) {
      return;
    }

    const quiz = this.studio.findQuizById(quizId);

    if (!quiz?.active) {
      this.archiveConfirmationQuizId.set(null);
      return;
    }

    this.isDeletingQuiz.set(quizId);
    this.quizMessage.set('');

    this.studio.archiveQuiz(quizId)
      .pipe(
        finalize(() => {
          this.isDeletingQuiz.set(null);
        })
      )
      .subscribe({
        next: () => {
          this.archiveConfirmationQuizId.set(null);
          this.quizMessage.set(`Quiz "${quiz.title}" has been archived.`);
        },
        error: (error: unknown) => {
          this.quizMessage.set(this.resolveQuizError(error));
        }
      });
  }

  archiveConfirmationQuizTitle(): string {
    return this.studio.findQuizById(this.archiveConfirmationQuizId() ?? -1)?.title ?? 'this quiz';
  }

  quizStatusLabel(active: boolean): string {
    return active ? 'Active' : 'Archived';
  }

  @HostListener('window:keydown.escape')
  handleEscape(): void {
    this.cancelQuizArchive();
  }

  setQuestionPreviewSearch(value: string): void {
    this.questionPreviewSearch.set(value);
    this.questionPreviewRequestedPage.set(0);
  }

  setQuestionPreviewCategoryFilter(category: number | 'All'): void {
    this.questionPreviewCategoryFilter.set(category);
    this.questionPreviewRequestedPage.set(0);
  }

  goToPreviousQuestionPreviewPage(): void {
    if (!this.canGoToPreviousQuestionPreviewPage()) {
      return;
    }

    this.questionPreviewRequestedPage.update((page) => Math.max(page - 1, 0));
  }

  goToNextQuestionPreviewPage(): void {
    if (!this.canGoToNextQuestionPreviewPage()) {
      return;
    }

    this.questionPreviewRequestedPage.update((page) => page + 1);
  }

  setNewManagedCategory(value: string): void {
    this.newManagedCategory.set(value);
    this.categoryMessage.set('');
  }

  createCategory(): void {
    const draft = this.newManagedCategory().trim();

    if (!draft) {
      this.categoryMessage.set('Category name cannot be empty.');
      return;
    }

    this.isSavingCategory.set(true);
    this.categoryMessage.set('');

    let request$;

    try {
      request$ = this.studio.createCategory(draft);
    } catch {
      this.isSavingCategory.set(false);
      this.categoryMessage.set('Load the course before creating a category.');
      return;
    }

    request$
      .pipe(
        finalize(() => {
          this.isSavingCategory.set(false);
        })
      )
      .subscribe({
        next: (category) => {
          this.newManagedCategory.set('');
          this.categoryMessage.set(`Category "${category.name}" is ready to use.`);
        },
        error: (error: unknown) => {
          this.categoryMessage.set(this.resolveCategoryError(error));
        }
      });
  }

  startCategoryEdit(categoryId: number, categoryName: string): void {
    this.editingCategoryId.set(categoryId);
    this.editingCategoryDraft.set(categoryName);
    this.categoryMessage.set('');
  }

  setEditingCategoryDraft(value: string): void {
    this.editingCategoryDraft.set(value);
    this.categoryMessage.set('');
  }

  saveCategoryEdit(categoryId: number): void {
    const draft = this.editingCategoryDraft().trim();

    if (!draft) {
      this.categoryMessage.set('Category name cannot be empty.');
      return;
    }

    this.isSavingCategory.set(true);

    let request$;

    try {
      request$ = this.studio.renameCategory(categoryId, draft);
    } catch {
      this.isSavingCategory.set(false);
      this.categoryMessage.set('Load the course before renaming a category.');
      return;
    }

    request$
      .pipe(
        finalize(() => {
          this.isSavingCategory.set(false);
        })
      )
      .subscribe({
        next: (category) => {
          this.editingCategoryId.set(null);
          this.editingCategoryDraft.set('');
          this.categoryMessage.set(`Category renamed to "${category.name}".`);
        },
        error: (error: unknown) => {
          this.categoryMessage.set(this.resolveCategoryError(error));
        }
      });
  }

  cancelCategoryEdit(): void {
    this.editingCategoryId.set(null);
    this.editingCategoryDraft.set('');
  }

  deleteCategory(categoryId: number, categoryName: string): void {
    this.isSavingCategory.set(true);

    let request$;

    try {
      request$ = this.studio.deleteCategory(categoryId);
    } catch {
      this.isSavingCategory.set(false);
      this.categoryMessage.set('Load the course before archiving a category.');
      return;
    }

    request$
      .pipe(
        finalize(() => {
          this.isSavingCategory.set(false);
        })
      )
      .subscribe({
        next: () => {
          if (this.editingCategoryId() === categoryId) {
            this.cancelCategoryEdit();
          }

          this.categoryMessage.set(`Category "${categoryName}" has been archived.`);
        },
        error: (error: unknown) => {
          this.categoryMessage.set(this.resolveCategoryError(error));
        }
      });
  }

  private resolveCategoryError(error: unknown): string {
    const fieldErrors = extractFieldErrors(error);
    const firstFieldError = Object.values(fieldErrors)[0];

    if (firstFieldError) {
      return firstFieldError;
    }

    if (error instanceof HttpErrorResponse) {
      if (error.status === 403) {
        return 'Only the course owner or an admin can change categories in this course.';
      }

      if (error.status === 401) {
        return 'Your session has expired. Sign in again and try once more.';
      }
    }

    return extractApiMessage(error) ?? 'Unable to update categories right now.';
  }

  private resolveQuizError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 403) {
        return 'Only the course owner or an admin can change quizzes in this course.';
      }

      if (error.status === 401) {
        return 'Your session has expired. Sign in again and try once more.';
      }
    }

    return extractApiMessage(error) ?? 'Unable to update quizzes right now.';
  }
}
