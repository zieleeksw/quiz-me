import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioQuestion } from './course-studio.service';

@Component({
  selector: 'app-course-studio-editor-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-studio-editor-page.component.html',
  styleUrl: './course-studio-editor-page.component.scss'
})
export class CourseStudioEditorPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);
  readonly studio = inject(CourseStudioService);

  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly studioLink = ['/courses', this.courseSlug];
  readonly addQuestionLink = ['/courses', this.courseSlug, 'editor', 'questions', 'new'];
  readonly activeTab = signal<'questions' | 'quizzes' | 'categories'>('questions');

  readonly selectedQuestionIds = signal<number[]>([]);
  readonly questionPreviewSearch = signal('');
  readonly questionPreviewCategoryFilter = signal<number | 'All'>('All');
  readonly questionPreviewRequestedPage = signal(0);
  readonly quizBankSearch = signal('');
  readonly quizCategoryFilter = signal<number | 'All'>('All');
  readonly newManagedCategory = signal('');
  readonly categoryMessage = signal('');
  readonly editingCategoryId = signal<number | null>(null);
  readonly editingCategoryDraft = signal('');
  readonly isSavingCategory = signal(false);

  readonly quizForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(4)]],
    mode: ['manual' as 'manual' | 'random', [Validators.required]],
    randomCount: [10, [Validators.min(1)]]
  });

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

  readonly filteredQuestions = computed(() => {
    const normalizedSearch = this.quizBankSearch().trim().toLowerCase();
    const activeCategory = this.quizCategoryFilter();

    return this.studio.questions().filter((question) => {
      const matchesCategory =
        activeCategory === 'All' || question.categories.some((category) => category.id === activeCategory);
      const matchesSearch =
        !normalizedSearch ||
        question.prompt.toLowerCase().includes(normalizedSearch) ||
        question.categories.some((category) => category.name.toLowerCase().includes(normalizedSearch));

      return matchesCategory && matchesSearch;
    });
  });

  readonly selectedQuestions = computed(() =>
    this.selectedQuestionIds()
      .map((questionId) => this.studio.questions().find((question) => question.id === questionId))
      .filter((question): question is StudioQuestion => Boolean(question))
  );

  readonly canCreateManualQuiz = computed(() => this.selectedQuestionIds().length > 0);

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
      const activeQuizFilter = this.quizCategoryFilter();

      if (activeQuestionPreviewFilter !== 'All' && !availableIds.has(activeQuestionPreviewFilter)) {
        this.questionPreviewCategoryFilter.set('All');
      }

      if (activeQuizFilter !== 'All' && !availableIds.has(activeQuizFilter)) {
        this.quizCategoryFilter.set('All');
      }
    });
  }

  setActiveTab(tab: 'questions' | 'quizzes' | 'categories'): void {
    this.activeTab.set(tab);
  }

  questionEditLink(questionId: number): unknown[] {
    return ['/courses', this.courseSlug, 'editor', 'questions', questionId, 'edit'];
  }

  createQuiz(): void {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      return;
    }

    const value = this.quizForm.getRawValue();

    if (value.mode === 'manual' && !this.selectedQuestionIds().length) {
      return;
    }

    this.studio.addQuiz({
      title: value.title,
      mode: value.mode,
      questionIds: this.selectedQuestionIds(),
      randomCount: value.mode === 'random' ? value.randomCount : null
    });

    this.quizForm.reset({
      title: '',
      mode: 'manual',
      randomCount: value.randomCount
    });
    this.selectedQuestionIds.set([]);
  }

  toggleQuestionSelection(questionId: number): void {
    this.selectedQuestionIds.update((selectedQuestionIds) =>
      selectedQuestionIds.includes(questionId)
        ? selectedQuestionIds.filter((id) => id !== questionId)
        : [...selectedQuestionIds, questionId]
    );
  }

  isQuestionSelected(questionId: number): boolean {
    return this.selectedQuestionIds().includes(questionId);
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

  setQuizBankSearch(value: string): void {
    this.quizBankSearch.set(value);
  }

  setQuizCategoryFilter(category: number | 'All'): void {
    this.quizCategoryFilter.set(category);
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

  clearSelectedQuestions(): void {
    this.selectedQuestionIds.set([]);
  }

  removeSelectedQuestion(questionId: number): void {
    this.selectedQuestionIds.update((selectedQuestionIds) => selectedQuestionIds.filter((id) => id !== questionId));
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
}
