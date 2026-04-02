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
import { CourseStudioService, StudioQuestionVersion } from './course-studio.service';

@Component({
  selector: 'app-course-question-editor-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-question-editor-page.component.html',
  styleUrl: './course-question-editor-page.component.scss'
})
export class CourseQuestionEditorPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);
  readonly studio = inject(CourseStudioService);

  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly questionId = computed(() => {
    const rawQuestionId = this.route.snapshot.paramMap.get('questionId');
    const parsedQuestionId = rawQuestionId ? Number(rawQuestionId) : NaN;

    return Number.isNaN(parsedQuestionId) ? null : parsedQuestionId;
  });
  readonly isEditing = computed(() => this.questionId() !== null);
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly editorLink = ['/courses', this.courseSlug, 'editor'];

  readonly selectedComposerCategoryIds = signal<number[]>([]);
  readonly questionMessage = signal('');
  readonly questionHistoryMessage = signal('');
  readonly isSavingQuestion = signal(false);
  readonly formInitializedForQuestionId = signal<number | null>(null);

  readonly questionForm = this.formBuilder.nonNullable.group({
    prompt: ['', [Validators.required, Validators.minLength(12)]],
    optionA: ['', [Validators.required]],
    optionB: ['', [Validators.required]],
    optionC: [''],
    optionD: [''],
    correctOptionIndex: [0, [Validators.required]]
  });

  readonly selectedComposerCategories = computed(() =>
    this.selectedComposerCategoryIds()
      .map((categoryId) => this.studio.categories().find((category) => category.id === categoryId))
      .filter((category): category is NonNullable<typeof category> => Boolean(category))
  );
  readonly editedQuestion = computed(() => {
    const questionId = this.questionId();

    if (!questionId) {
      return null;
    }

    return this.studio.questions().find((question) => question.id === questionId) ?? null;
  });
  readonly questionHistory = computed<StudioQuestionVersion[]>(() => {
    const questionId = this.questionId();

    if (!questionId) {
      return [];
    }

    return this.studio.getQuestionVersions(questionId);
  });
  readonly isLoadingQuestionHistory = computed(() => this.studio.versionLoadingQuestionId() === this.questionId());

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

      if (!validSelectedIds.length && categories.length && !this.isEditing()) {
        this.selectedComposerCategoryIds.set([categories[0].id]);
      }
    });

    effect(() => {
      const question = this.editedQuestion();
      const questionId = this.questionId();

      if (!questionId || !question || this.formInitializedForQuestionId() === questionId) {
        return;
      }

      this.selectedComposerCategoryIds.set(question.categories.map((category) => category.id));
      this.questionForm.setValue({
        prompt: question.prompt,
        optionA: question.options[0]?.content ?? '',
        optionB: question.options[1]?.content ?? '',
        optionC: question.options[2]?.content ?? '',
        optionD: question.options[3]?.content ?? '',
        correctOptionIndex: question.correctOptionIndex
      });
      this.formInitializedForQuestionId.set(questionId);
      this.loadQuestionHistory(questionId);
    });
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
      request$ = this.isEditing() ? this.studio.updateQuestion(this.questionId()!, payload) : this.studio.createQuestion(payload);
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
          this.questionMessage.set(this.isEditing() ? 'A new question version has been saved.' : 'Question added to the course.');

          if (this.isEditing()) {
            this.formInitializedForQuestionId.set(question.id);
            this.loadQuestionHistory(question.id, true);
            return;
          }

          this.resetQuestionComposer();
        },
        error: (error: unknown) => {
          this.questionMessage.set(this.resolveQuestionSaveError(error));
        }
      });
  }

  toggleComposerCategory(categoryId: number): void {
    this.questionMessage.set('');
    this.selectedComposerCategoryIds.update((categories) =>
      categories.includes(categoryId) ? categories.filter((entry) => entry !== categoryId) : [...categories, categoryId]
    );
  }

  removeComposerCategory(categoryId: number): void {
    this.questionMessage.set('');
    this.selectedComposerCategoryIds.update((categories) => categories.filter((entry) => entry !== categoryId));
  }

  private loadQuestionHistory(questionId: number, force = false): void {
    this.questionHistoryMessage.set('');

    this.studio.loadQuestionVersions(questionId, force).subscribe({
      error: (error: unknown) => {
        this.questionHistoryMessage.set(extractApiMessage(error) ?? 'Unable to load question history right now.');
      }
    });
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

  private resetQuestionComposer(): void {
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
