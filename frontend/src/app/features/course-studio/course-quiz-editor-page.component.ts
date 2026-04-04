import { Component, HostListener, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { PendingChangesAware } from '../../core/navigation/pending-changes.guard';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioQuestion } from './course-studio.service';

type QuizDraftSnapshot = {
  title: string;
  mode: 'manual' | 'random';
  randomCount: number | null;
  questionIds: number[];
};

@Component({
  selector: 'app-course-quiz-editor-page',
  imports: [ReactiveFormsModule, RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-quiz-editor-page.component.html',
  styleUrl: './course-quiz-editor-page.component.scss'
})
export class CourseQuizEditorPageComponent implements PendingChangesAware {
  private static readonly DEFAULT_RANDOM_COUNT = 10;
  private static readonly QUESTION_PAGE_SIZE = 5;
  private static readonly PROMPT_DEDUP_WINDOW_MS = 400;
  private skipBeforeUnloadUntil = 0;
  private lastBeforeUnloadAt = 0;
  private lastDiscardDecision: boolean | null = null;
  private lastDiscardDecisionAt = 0;

  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);
  readonly studio = inject(CourseStudioService);

  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly quizId = computed(() => this.route.snapshot.paramMap.get('quizId'));
  readonly isEditing = computed(() => Boolean(this.quizId()));
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly quizListLink = ['/courses', this.courseSlug, 'editor', 'quizzes'];
  readonly editedQuiz = computed(() => {
    const quizId = this.quizId();

    if (!quizId) {
      return null;
    }

    return this.studio.findQuizById(quizId);
  });

  readonly selectedQuestionIds = signal<number[]>([]);
  readonly quizBankSearch = signal('');
  readonly quizCategoryFilter = signal<number | 'All'>('All');
  readonly quizPreviewRequestedPage = signal(0);
  readonly quizMessage = signal('');
  readonly formInitializedForQuizId = signal<string | null>(null);
  readonly draggedQuestionId = signal<number | null>(null);
  readonly draggedQuestionSource = signal<'bank' | 'basket' | null>(null);
  readonly basketDropActive = signal(false);
  readonly basketDropTarget = signal<{ questionId: number | null; position: 'before' | 'after' | 'end' } | null>(null);
  readonly newQuizBaseline = signal<QuizDraftSnapshot>({
    title: '',
    mode: 'manual',
    randomCount: null,
    questionIds: []
  });

  readonly quizForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(4)]],
    mode: ['manual' as 'manual' | 'random', [Validators.required]],
    randomCount: [CourseQuizEditorPageComponent.DEFAULT_RANDOM_COUNT, [Validators.required, Validators.min(1)]]
  });

  readonly filteredQuestions = computed(() => {
    const normalizedSearch = this.quizBankSearch().trim().toLowerCase();
    const activeCategory = this.quizCategoryFilter();
    const selectedQuestionIds = new Set(this.selectedQuestionIds());

    return this.studio.questions().filter((question) => {
      if (selectedQuestionIds.has(question.id)) {
        return false;
      }

      const matchesCategory =
        activeCategory === 'All' || question.categories.some((category) => category.id === activeCategory);
      const matchesSearch =
        !normalizedSearch ||
        question.prompt.toLowerCase().includes(normalizedSearch) ||
        question.categories.some((category) => category.name.toLowerCase().includes(normalizedSearch));

      return matchesCategory && matchesSearch;
    });
  });
  readonly quizTotalPages = computed(() => Math.ceil(this.filteredQuestions().length / CourseQuizEditorPageComponent.QUESTION_PAGE_SIZE));
  readonly pagedQuestions = computed(() => {
    const startIndex = this.quizPreviewRequestedPage() * CourseQuizEditorPageComponent.QUESTION_PAGE_SIZE;
    return this.filteredQuestions().slice(startIndex, startIndex + CourseQuizEditorPageComponent.QUESTION_PAGE_SIZE);
  });
  readonly quizPreviewPageLabel = computed(() => {
    if (!this.quizTotalPages()) {
      return 'Page 0 of 0';
    }

    return `Page ${this.quizPreviewRequestedPage() + 1} of ${this.quizTotalPages()}`;
  });
  readonly canGoToPreviousQuizPreviewPage = computed(() => this.quizPreviewRequestedPage() > 0);
  readonly canGoToNextQuizPreviewPage = computed(() => this.quizTotalPages() > 0 && this.quizPreviewRequestedPage() < this.quizTotalPages() - 1);

  readonly selectedQuestions = computed(() =>
    this.selectedQuestionIds()
      .map((questionId) => this.studio.questions().find((question) => question.id === questionId))
      .filter((question): question is StudioQuestion => Boolean(question))
  );

  readonly canCreateManualQuiz = computed(() => this.selectedQuestionIds().length > 0);
  readonly isManualMode = computed(() => this.quizForm.controls.mode.value === 'manual');
  readonly hasQuizChanges = computed(() => {
    const draft = this.captureQuizDraft();

    if (!this.isEditing()) {
      return !this.areQuizDraftsEqual(draft, this.newQuizBaseline());
    }

    const quiz = this.editedQuiz();

    if (!quiz) {
      return false;
    }

    return !this.areQuizDraftsEqual(draft, this.createQuizSnapshotFromExistingQuiz());
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
      const categories = this.studio.categories();
      const availableIds = new Set(categories.map((category) => category.id));
      const activeQuizFilter = this.quizCategoryFilter();

      if (activeQuizFilter !== 'All' && !availableIds.has(activeQuizFilter)) {
        this.quizCategoryFilter.set('All');
      }
    });

    effect(() => {
      const quiz = this.editedQuiz();
      const quizId = this.quizId();

      if (!quiz || !quizId || this.formInitializedForQuizId() === quizId) {
        return;
      }

      this.quizForm.reset({
        title: quiz.title,
        mode: quiz.mode,
        randomCount: quiz.randomCount ?? Math.max(quiz.resolvedQuestionCount, 1)
      });
      this.selectedQuestionIds.set([...quiz.questionIds]);
      this.formInitializedForQuizId.set(quizId);
    });

    effect(() => {
      const totalPages = this.quizTotalPages();
      const currentPage = this.quizPreviewRequestedPage();

      if (!totalPages && currentPage !== 0) {
        this.quizPreviewRequestedPage.set(0);
        return;
      }

      if (totalPages && currentPage > totalPages - 1) {
        this.quizPreviewRequestedPage.set(totalPages - 1);
      }
    });
  }

  setQuizBankSearch(value: string): void {
    this.quizBankSearch.set(value);
    this.quizPreviewRequestedPage.set(0);
  }

  setQuizCategoryFilter(category: number | 'All'): void {
    this.quizCategoryFilter.set(category);
    this.quizPreviewRequestedPage.set(0);
  }

  goToPreviousQuizPreviewPage(): void {
    if (!this.canGoToPreviousQuizPreviewPage()) {
      return;
    }

    this.quizPreviewRequestedPage.update((page) => Math.max(page - 1, 0));
  }

  goToNextQuizPreviewPage(): void {
    if (!this.canGoToNextQuizPreviewPage()) {
      return;
    }

    this.quizPreviewRequestedPage.update((page) => page + 1);
  }

  toggleQuestionSelection(questionId: number): void {
    this.quizMessage.set('');
    this.selectedQuestionIds.update((selectedQuestionIds) =>
      selectedQuestionIds.includes(questionId)
        ? selectedQuestionIds.filter((id) => id !== questionId)
        : [...selectedQuestionIds, questionId]
    );
  }

  isQuestionSelected(questionId: number): boolean {
    return this.selectedQuestionIds().includes(questionId);
  }

  clearSelectedQuestions(): void {
    this.selectedQuestionIds.set([]);
  }

  removeSelectedQuestion(questionId: number): void {
    this.selectedQuestionIds.update((selectedQuestionIds) => selectedQuestionIds.filter((id) => id !== questionId));
  }

  startBankQuestionDrag(questionId: number): void {
    this.draggedQuestionId.set(questionId);
    this.draggedQuestionSource.set('bank');
  }

  startBasketQuestionDrag(questionId: number): void {
    this.draggedQuestionId.set(questionId);
    this.draggedQuestionSource.set('basket');
  }

  endQuestionDrag(): void {
    this.draggedQuestionId.set(null);
    this.draggedQuestionSource.set(null);
    this.basketDropActive.set(false);
    this.basketDropTarget.set(null);
  }

  markBasketEndDropTarget(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.isManualMode()) {
      this.basketDropActive.set(true);
      this.basketDropTarget.set({ questionId: null, position: 'end' });
    }
  }

  markBasketItemDropTarget(questionId: number, event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();

    if (!this.isManualMode()) {
      return;
    }

    const currentTarget = event.currentTarget;

    if (!(currentTarget instanceof HTMLElement)) {
      return;
    }

    const bounds = currentTarget.getBoundingClientRect();
    const midpoint = bounds.top + bounds.height / 2;
    const position = event.clientY < midpoint ? 'before' : 'after';

    this.basketDropActive.set(true);
    this.basketDropTarget.set({ questionId, position });
  }

  handleBasketDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();

    if (!this.isManualMode()) {
      this.endQuestionDrag();
      return;
    }

    const draggedQuestionId = this.draggedQuestionId();
    const source = this.draggedQuestionSource();

    if (!draggedQuestionId || !source) {
      this.endQuestionDrag();
      return;
    }

    const dropTarget = this.basketDropTarget();

    if (source === 'bank') {
      this.insertQuestionIntoBasket(draggedQuestionId, dropTarget);
    } else if (source === 'basket') {
      this.reorderQuestionInBasket(draggedQuestionId, dropTarget);
    }

    this.endQuestionDrag();
  }

  handleBasketDragLeave(): void {
    this.basketDropActive.set(false);
    this.basketDropTarget.set(null);
  }

  createQuiz(): void {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      this.quizMessage.set('Complete the quiz title and random count before saving.');
      return;
    }

    const value = this.quizForm.getRawValue();

    if (value.mode === 'manual' && !this.selectedQuestionIds().length) {
      this.quizMessage.set('Select at least one question for a manual quiz.');
      return;
    }

    if (this.isEditing() && !this.hasQuizChanges()) {
      this.quizMessage.set('Make at least one change before saving.');
      return;
    }

    const payload = {
      title: value.title.trim(),
      mode: value.mode,
      questionIds: this.selectedQuestionIds(),
      randomCount: value.mode === 'random' ? value.randomCount : null
    };

    if (this.isEditing() && this.quizId()) {
      this.studio.updateQuiz(this.quizId()!, payload);
      this.quizMessage.set('Quiz updated in the course preview.');
      return;
    }

    this.studio.addQuiz(payload);

    this.quizForm.reset({
      title: '',
      mode: 'manual',
      randomCount: value.randomCount
    });
    this.selectedQuestionIds.set([]);
    this.newQuizBaseline.set(this.captureQuizDraft());
    this.quizMessage.set('Quiz added to the course preview.');
  }

  hasPendingChanges(): boolean {
    return this.hasQuizChanges();
  }

  confirmDiscardChanges(): boolean {
    const now = Date.now();

    if (now - this.lastBeforeUnloadAt < CourseQuizEditorPageComponent.PROMPT_DEDUP_WINDOW_MS) {
      return false;
    }

    if (now - this.lastDiscardDecisionAt < CourseQuizEditorPageComponent.PROMPT_DEDUP_WINDOW_MS && this.lastDiscardDecision !== null) {
      return this.lastDiscardDecision;
    }

    this.skipBeforeUnloadUntil = Date.now() + 1500;
    const shouldLeave = window.confirm(
      'You have unsaved quiz changes. Click Cancel to stay here and save them first, or OK to leave without saving.'
    );

    this.lastDiscardDecision = shouldLeave;
    this.lastDiscardDecisionAt = now;

    return shouldLeave;
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent): void {
    if (Date.now() < this.skipBeforeUnloadUntil) {
      return;
    }

    if (!this.hasPendingChanges()) {
      return;
    }

    this.lastBeforeUnloadAt = Date.now();
    event.preventDefault();
    event.returnValue = true;
  }

  private insertQuestionIntoBasket(
    questionId: number,
    dropTarget: { questionId: number | null; position: 'before' | 'after' | 'end' } | null
  ): void {
    this.selectedQuestionIds.update((selectedQuestionIds) => {
      const withoutDraggedQuestion = selectedQuestionIds.filter((id) => id !== questionId);
      const targetIndex = this.resolveDropIndex(withoutDraggedQuestion, dropTarget);

      if (targetIndex === -1) {
        return [...withoutDraggedQuestion, questionId];
      }

      const nextSelectedQuestionIds = [...withoutDraggedQuestion];
      nextSelectedQuestionIds.splice(targetIndex, 0, questionId);
      return nextSelectedQuestionIds;
    });
  }

  private reorderQuestionInBasket(
    questionId: number,
    dropTarget: { questionId: number | null; position: 'before' | 'after' | 'end' } | null
  ): void {
    this.selectedQuestionIds.update((selectedQuestionIds) => {
      const sourceIndex = selectedQuestionIds.indexOf(questionId);

      if (sourceIndex === -1) {
        return selectedQuestionIds;
      }

      const reorderedQuestionIds = selectedQuestionIds.filter((id) => id !== questionId);
      const targetIndex = this.resolveDropIndex(reorderedQuestionIds, dropTarget);

      if (targetIndex === -1) {
        return [...reorderedQuestionIds, questionId];
      }

      const nextSelectedQuestionIds = [...reorderedQuestionIds];
      nextSelectedQuestionIds.splice(targetIndex, 0, questionId);
      return nextSelectedQuestionIds;
    });
  }

  private resolveDropIndex(
    questionIds: number[],
    dropTarget: { questionId: number | null; position: 'before' | 'after' | 'end' } | null
  ): number {
    if (!dropTarget || dropTarget.position === 'end' || dropTarget.questionId === null) {
      return questionIds.length;
    }

    const targetIndex = questionIds.indexOf(dropTarget.questionId);

    if (targetIndex === -1) {
      return questionIds.length;
    }

    return dropTarget.position === 'before' ? targetIndex : targetIndex + 1;
  }

  private captureQuizDraft(): QuizDraftSnapshot {
    const value = this.quizForm.getRawValue();

    return {
      title: value.title.trim(),
      mode: value.mode,
      randomCount: value.mode === 'random' ? value.randomCount : null,
      questionIds: [...this.selectedQuestionIds()]
    };
  }

  private createQuizSnapshotFromExistingQuiz(): QuizDraftSnapshot {
    const quiz = this.editedQuiz();

    if (!quiz) {
      return this.newQuizBaseline();
    }

    return {
      title: quiz.title,
      mode: quiz.mode,
      randomCount: quiz.mode === 'random' ? quiz.randomCount ?? Math.max(quiz.resolvedQuestionCount, 1) : null,
      questionIds: [...quiz.questionIds]
    };
  }

  private areQuizDraftsEqual(left: QuizDraftSnapshot, right: QuizDraftSnapshot): boolean {
    return (
      left.title === right.title &&
      left.mode === right.mode &&
      left.randomCount === right.randomCount &&
      JSON.stringify(left.questionIds) === JSON.stringify(right.questionIds)
    );
  }
}
