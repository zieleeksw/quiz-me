import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { startWith } from 'rxjs';
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
  readonly questionFormValue = toSignal(
    this.questionForm.valueChanges.pipe(startWith(this.questionForm.getRawValue())),
    { initialValue: this.questionForm.getRawValue() }
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
  readonly hasQuestionChanges = computed(() => {
    if (!this.isEditing()) {
      return true;
    }

    const question = this.editedQuestion();

    if (!question) {
      return false;
    }

    const formValue = this.questionFormValue();
    const normalizedCorrectOptionIndex = this.normalizeCorrectOptionIndex(formValue.correctOptionIndex);
    const currentAnswerContents = question.options
      .slice()
      .sort((left, right) => left.displayOrder - right.displayOrder)
      .map((option) => option.content.trim());
    const draftAnswerContents = [
      formValue.optionA?.trim() ?? '',
      formValue.optionB?.trim() ?? '',
      formValue.optionC?.trim() ?? '',
      formValue.optionD?.trim() ?? ''
    ].filter((content) => content.length > 0);
    const currentCategoryIds = question.categories.map((category) => category.id).slice().sort((left, right) => left - right);
    const draftCategoryIds = this.selectedComposerCategoryIds()
      .slice()
      .sort((left, right) => left - right);

    return (
      question.prompt !== (formValue.prompt?.trim() ?? '') ||
      JSON.stringify(currentAnswerContents) !== JSON.stringify(draftAnswerContents) ||
      question.correctOptionIndex !== normalizedCorrectOptionIndex ||
      JSON.stringify(currentCategoryIds) !== JSON.stringify(draftCategoryIds)
    );
  });
  readonly isSaveDisabled = computed(
    () => this.isSavingQuestion() || this.studio.isLoading() || !this.studio.activeCourseId() || (this.isEditing() && !this.hasQuestionChanges())
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
    if (this.isEditing() && !this.hasQuestionChanges()) {
      this.questionMessage.set('Make at least one change before saving a new version.');
      return;
    }

    const validationMessage = this.validateQuestionForm();

    if (validationMessage) {
      this.questionForm.markAllAsTouched();
      this.questionMessage.set(validationMessage);
      return;
    }

    const value = this.questionForm.getRawValue();
    const normalizedCorrectOptionIndex = this.normalizeCorrectOptionIndex(value.correctOptionIndex);
    const answers = [
      { content: value.optionA.trim(), correct: normalizedCorrectOptionIndex === 0 },
      { content: value.optionB.trim(), correct: normalizedCorrectOptionIndex === 1 },
      { content: value.optionC.trim(), correct: normalizedCorrectOptionIndex === 2 },
      { content: value.optionD.trim(), correct: normalizedCorrectOptionIndex === 3 }
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
    const normalizedCorrectOptionIndex = this.normalizeCorrectOptionIndex(value.correctOptionIndex);
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

    if (!options[normalizedCorrectOptionIndex]) {
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

  private normalizeCorrectOptionIndex(value: unknown): number {
    return typeof value === 'number' ? value : Number(value);
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
    this.selectedComposerCategoryIds.set([]);
  }
}
