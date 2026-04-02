import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
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
  private static readonly MIN_ANSWERS = 2;
  private static readonly MAX_ANSWERS = 6;

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
    answers: this.formBuilder.nonNullable.array([
      this.createAnswerControl(),
      this.createAnswerControl()
    ]),
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
  readonly canAddMoreAnswers = computed(() => this.answersArray().length < CourseQuestionEditorPageComponent.MAX_ANSWERS);
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
    const draftAnswerContents = (formValue.answers ?? []).map((content) => (content ?? '').trim()).filter((content) => content.length > 0);
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
      this.setAnswers(question.options.map((option) => option.content));
      this.questionForm.patchValue({
        prompt: question.prompt,
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
    const answers = value.answers.map((content, index) => ({
      content: content.trim(),
      correct: normalizedCorrectOptionIndex === index
    }));

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

  answersArray(): FormArray<FormControl<string>> {
    return this.questionForm.controls.answers;
  }

  answerLabel(index: number): string {
    return String.fromCharCode(65 + index);
  }

  addAnswer(): void {
    if (!this.canAddMoreAnswers()) {
      return;
    }

    this.answersArray().push(this.createAnswerControl());
    this.questionMessage.set('');
  }

  removeAnswer(index: number): void {
    if (this.answersArray().length <= CourseQuestionEditorPageComponent.MIN_ANSWERS) {
      return;
    }

    this.answersArray().removeAt(index);
    const normalizedCorrectOptionIndex = this.normalizeCorrectOptionIndex(this.questionForm.controls.correctOptionIndex.value);

    if (normalizedCorrectOptionIndex > index) {
      this.questionForm.controls.correctOptionIndex.setValue(normalizedCorrectOptionIndex - 1);
    } else if (normalizedCorrectOptionIndex === index) {
      this.questionForm.controls.correctOptionIndex.setValue(Math.min(index, this.answersArray().length - 1));
    }

    this.questionMessage.set('');
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
    const options = value.answers.map((entry) => entry.trim());

    if (!this.selectedComposerCategoryIds().length) {
      return 'Choose at least one category.';
    }

    if (!prompt) {
      return 'Question prompt is required.';
    }

    if (prompt.length < 12) {
      return 'Question prompt must be at least 12 characters long.';
    }

    if (options.length < CourseQuestionEditorPageComponent.MIN_ANSWERS) {
      return 'Question must contain at least two answers.';
    }

    if (options.some((entry) => !entry)) {
      return 'Fill in every visible answer option.';
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
    this.questionForm.patchValue({
      prompt: '',
      correctOptionIndex: 0
    });
    this.setAnswers(['', '']);
    this.selectedComposerCategoryIds.set([]);
  }

  private createAnswerControl(value = ''): FormControl<string> {
    return this.formBuilder.nonNullable.control(value, Validators.required);
  }

  private setAnswers(contents: string[]): void {
    const answers = contents.length >= CourseQuestionEditorPageComponent.MIN_ANSWERS
      ? contents
      : [...contents, ...Array.from({ length: CourseQuestionEditorPageComponent.MIN_ANSWERS - contents.length }, () => '')];
    const nextArray = this.formBuilder.nonNullable.array(
      answers.map((content) => this.createAnswerControl(content))
    );

    this.questionForm.setControl('answers', nextArray);
  }
}
