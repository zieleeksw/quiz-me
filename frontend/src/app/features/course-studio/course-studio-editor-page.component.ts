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
import { CourseStudioService, StudioQuestion, StudioQuestionVersion } from './course-studio.service';

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
  readonly activeTab = signal<'questions' | 'quizzes' | 'categories'>('questions');

  readonly selectedQuestionIds = signal<number[]>([]);
  readonly bankSearch = signal('');
  readonly activeCategoryFilter = signal<number | 'All'>('All');
  readonly selectedComposerCategoryIds = signal<number[]>([]);
  readonly newManagedCategory = signal('');
  readonly categoryMessage = signal('');
  readonly editingCategoryId = signal<number | null>(null);
  readonly editingCategoryDraft = signal('');
  readonly editingQuestionId = signal<number | null>(null);
  readonly questionMessage = signal('');
  readonly questionHistoryMessage = signal('');
  readonly questionHistoryTargetId = signal<number | null>(null);
  readonly isSavingQuestion = signal(false);
  readonly isSavingCategory = signal(false);

  readonly questionForm = this.formBuilder.nonNullable.group({
    prompt: ['', [Validators.required, Validators.minLength(12)]],
    optionA: ['', [Validators.required]],
    optionB: ['', [Validators.required]],
    optionC: [''],
    optionD: [''],
    correctOptionIndex: [0, [Validators.required]]
  });

  readonly quizForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(4)]],
    mode: ['manual' as 'manual' | 'random', [Validators.required]],
    randomCount: [10, [Validators.min(1)]]
  });

  readonly filteredQuestions = computed(() => {
    const normalizedSearch = this.bankSearch().trim().toLowerCase();
    const activeCategory = this.activeCategoryFilter();

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
  readonly selectedComposerCategories = computed(() =>
    this.selectedComposerCategoryIds()
      .map((categoryId) => this.studio.categories().find((category) => category.id === categoryId))
      .filter((category): category is NonNullable<typeof category> => Boolean(category))
  );
  readonly questionHistory = computed<StudioQuestionVersion[]>(() => {
    const questionId = this.questionHistoryTargetId();

    if (!questionId) {
      return [];
    }

    return this.studio.getQuestionVersions(questionId);
  });
  readonly isLoadingQuestionHistory = computed(
    () => this.studio.versionLoadingQuestionId() === this.questionHistoryTargetId()
  );

  constructor() {
    this.coursesCatalogService.loadCourses();

    effect(() => {
      const course = this.currentCourse();

      if (course) {
        this.studio.loadCourseContext(course.id);
      }
    });

    effect(() => {
      const categories = this.studio.categories();
      const availableIds = new Set(categories.map((category) => category.id));
      const validSelectedIds = this.selectedComposerCategoryIds().filter((categoryId) => availableIds.has(categoryId));

      if (validSelectedIds.length !== this.selectedComposerCategoryIds().length) {
        this.selectedComposerCategoryIds.set(validSelectedIds);
      }

      if (!validSelectedIds.length && categories.length && this.editingQuestionId() === null) {
        this.selectedComposerCategoryIds.set([categories[0].id]);
      }

      const activeFilter = this.activeCategoryFilter();

      if (activeFilter !== 'All' && !availableIds.has(activeFilter)) {
        this.activeCategoryFilter.set('All');
      }
    });
  }

  setActiveTab(tab: 'questions' | 'quizzes' | 'categories'): void {
    this.activeTab.set(tab);
  }

  saveQuestion(): void {
    const validationMessage = this.validateQuestionForm();

    if (validationMessage) {
      this.questionForm.markAllAsTouched();
      this.questionMessage.set(validationMessage);
      return;
    }

    const value = this.questionForm.getRawValue();
    const answers = [
      { content: value.optionA.trim(), correct: value.correctOptionIndex === 0 },
      { content: value.optionB.trim(), correct: value.correctOptionIndex === 1 },
      { content: value.optionC.trim(), correct: value.correctOptionIndex === 2 },
      { content: value.optionD.trim(), correct: value.correctOptionIndex === 3 }
    ].filter((answer) => answer.content.length > 0);

    const payload = {
      prompt: value.prompt.trim(),
      answers,
      categoryIds: this.selectedComposerCategoryIds()
    };

    this.isSavingQuestion.set(true);
    this.questionMessage.set('');

    let request$;

    try {
      request$ = this.editingQuestionId()
        ? this.studio.updateQuestion(this.editingQuestionId()!, payload)
        : this.studio.createQuestion(payload);
    } catch {
      this.isSavingQuestion.set(false);
      this.questionMessage.set('Load the course before saving a question.');
      return;
    }

    request$
      .pipe(
        finalize(() => {
          this.isSavingQuestion.set(false);
        })
      )
      .subscribe({
        next: (question) => {
          const previousEditingId = this.editingQuestionId();
          this.questionMessage.set(previousEditingId ? 'A new question version has been saved.' : 'Question added to the course.');

          if (previousEditingId) {
            this.questionHistoryTargetId.set(question.id);
            this.loadQuestionHistory(question.id, true);
          }

          this.resetQuestionComposer();
        },
        error: (error: unknown) => {
          this.questionMessage.set(this.resolveQuestionSaveError(error));
        }
      });
  }

  startQuestionEdit(question: StudioQuestion): void {
    this.activeTab.set('questions');
    this.editingQuestionId.set(question.id);
    this.questionMessage.set('');
    this.selectedComposerCategoryIds.set(question.categories.map((category) => category.id));

    this.questionForm.setValue({
      prompt: question.prompt,
      optionA: question.options[0]?.content ?? '',
      optionB: question.options[1]?.content ?? '',
      optionC: question.options[2]?.content ?? '',
      optionD: question.options[3]?.content ?? '',
      correctOptionIndex: question.correctOptionIndex
    });
  }

  cancelQuestionEdit(): void {
    this.resetQuestionComposer();
    this.questionMessage.set('');
  }

  openQuestionHistory(questionId: number): void {
    this.questionHistoryTargetId.set(questionId);
    this.questionHistoryMessage.set('');
    this.loadQuestionHistory(questionId);
  }

  clearQuestionHistory(): void {
    this.questionHistoryTargetId.set(null);
    this.questionHistoryMessage.set('');
  }

  private loadQuestionHistory(questionId: number, force = false): void {
    this.studio.loadQuestionVersions(questionId, force).subscribe({
      error: (error: unknown) => {
        this.questionHistoryMessage.set(extractApiMessage(error) ?? 'Unable to load question history right now.');
      }
    });
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

  setBankSearch(value: string): void {
    this.bankSearch.set(value);
  }

  setCategoryFilter(category: number | 'All'): void {
    this.activeCategoryFilter.set(category);
  }

  toggleComposerCategory(categoryId: number): void {
    this.categoryMessage.set('');
    this.questionMessage.set('');
    this.selectedComposerCategoryIds.update((categories) =>
      categories.includes(categoryId) ? categories.filter((entry) => entry !== categoryId) : [...categories, categoryId]
    );
  }

  removeComposerCategory(categoryId: number): void {
    this.categoryMessage.set('');
    this.questionMessage.set('');
    this.selectedComposerCategoryIds.update((categories) => categories.filter((entry) => entry !== categoryId));
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
          this.selectedComposerCategoryIds.update((categories) =>
            categories.includes(category.id) ? categories : [...categories, category.id]
          );
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
          this.selectedComposerCategoryIds.update((categories) => categories.filter((entry) => entry !== categoryId));

          const remainingCategories = this.studio.categories();

          if (!this.selectedComposerCategoryIds().length && remainingCategories.length && this.editingQuestionId() === null) {
            this.selectedComposerCategoryIds.set([remainingCategories[0].id]);
          }

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

  private validateQuestionForm(): string | null {
    const value = this.questionForm.getRawValue();
    const prompt = value.prompt.trim();
    const options = [value.optionA, value.optionB, value.optionC, value.optionD].map((entry) => entry.trim());
    const filledOptions = options.filter((entry) => entry.length > 0);

    if (!this.selectedComposerCategoryIds().length) {
      return 'Choose at least one category.';
    }

    if (!prompt) {
      return 'Question prompt is required.';
    }

    if (prompt.length < 12) {
      return 'Question prompt must be at least 12 characters long.';
    }

    if (!options[0] || !options[1]) {
      return 'Fill in at least Option A and Option B.';
    }

    if (filledOptions.length < 2) {
      return 'Question must contain at least two answers.';
    }

    if (!options[value.correctOptionIndex]) {
      return 'Choose a correct answer that points to a filled option.';
    }

    return null;
  }

  private resolveQuestionSaveError(error: unknown): string {
    const fieldErrors = extractFieldErrors(error);
    const firstFieldError = Object.values(fieldErrors)[0];

    if (firstFieldError) {
      return firstFieldError;
    }

    if (error instanceof HttpErrorResponse) {
      if (error.status === 403) {
        return 'Only the course owner or an admin can add and edit questions in this course.';
      }

      if (error.status === 401) {
        return 'Your session has expired. Sign in again and try once more.';
      }
    }

    return extractApiMessage(error) ?? 'Unable to save this question right now.';
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

  private resetQuestionComposer(): void {
    this.editingQuestionId.set(null);
    this.questionForm.reset({
      prompt: '',
      optionA: '',
      optionB: '',
      optionC: '',
      optionD: '',
      correctOptionIndex: 0
    });

    if (this.studio.categories().length) {
      this.selectedComposerCategoryIds.set([this.studio.categories()[0].id]);
    } else {
      this.selectedComposerCategoryIds.set([]);
    }
  }
}
