import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioQuestion } from './course-studio.service';

@Component({
  selector: 'app-course-quiz-play-page',
  imports: [ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-quiz-play-page.component.html',
  styleUrl: './course-quiz-play-page.component.scss'
})
export class CourseQuizPlayPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly coursesCatalogService = inject(CoursesCatalogService);
  private readonly startedAttemptKeyState = signal<string | null>(null);

  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly quizId = Number(this.route.snapshot.paramMap.get('quizId') ?? Number.NaN);
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly activeAttempt = computed(() => this.studio.activeAttempt());
  readonly attemptResult = computed(() => this.activeAttempt()?.result ?? null);
  readonly activeQuiz = computed(() => (Number.isFinite(this.quizId) ? this.studio.findQuizById(this.quizId) : null));
  readonly currentQuestion = computed(() => this.studio.currentQuestion());
  readonly currentAnswerId = computed(() => this.studio.currentAnswerId());
  readonly totalAttemptQuestions = computed(() => this.studio.activeAttemptQuestions().length);
  readonly currentQuestionNumber = computed(() => (this.activeAttempt()?.currentIndex ?? 0) + 1);
  readonly answeredQuestionCount = computed(() => Object.keys(this.activeAttempt()?.answers ?? {}).length);
  readonly isFirstQuestion = computed(() => (this.activeAttempt()?.currentIndex ?? 0) === 0);
  readonly isLastQuestion = computed(() => {
    const attempt = this.activeAttempt();

    if (!attempt) {
      return false;
    }

    return attempt.currentIndex >= attempt.questionIds.length - 1;
  });
  readonly canMoveForward = computed(() => this.currentAnswerId() !== null && !this.studio.isSubmittingAttempt());
  readonly canFinishAttempt = computed(
    () =>
      this.currentAnswerId() !== null &&
      this.answeredQuestionCount() === this.totalAttemptQuestions() &&
      !this.studio.isSubmittingAttempt()
  );
  readonly playSessionKey = computed(() => {
    const course = this.currentCourse();

    if (!course || !Number.isFinite(this.quizId)) {
      return null;
    }

    return `${course.id}:${this.quizId}`;
  });
  readonly questionNavItems = computed(() =>
    this.studio.activeAttemptQuestions().map((question, index) => ({
      id: question.id,
      number: index + 1,
      preview: this.createQuestionPreview(question),
      answered: this.activeAttempt()?.answers[question.id] !== undefined,
      active: index === this.activeAttempt()?.currentIndex
    }))
  );
  readonly currentQuestionOptions = computed(() => {
    const question = this.currentQuestion();

    if (!question) {
      return [];
    }
    const answerOrder = this.activeQuiz()?.answerOrder ?? 'fixed';

    if (answerOrder !== 'random') {
      return question.options;
    }

    return this.createDeterministicShuffle(question.options, `${question.id}:${this.activeQuiz()?.id ?? 'practice'}`);
  });
  readonly isQuizUnavailable = computed(() => {
    if (this.studio.isLoading()) {
      return false;
    }

    if (!Number.isFinite(this.quizId)) {
      return true;
    }

    const quiz = this.activeQuiz();

    return !quiz || !quiz.active;
  });
  readonly isQuizEmpty = computed(() => {
    if (this.studio.isLoading() || this.isQuizUnavailable()) {
      return false;
    }

    return this.startedAttemptKeyState() === this.playSessionKey() && !this.activeAttempt();
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
      const playSessionKey = this.playSessionKey();

      if (!playSessionKey || !this.studio.isLoaded()) {
        return;
      }

      if (this.startedAttemptKeyState() === playSessionKey) {
        return;
      }

      this.startedAttemptKeyState.set(playSessionKey);
      this.studio.startQuiz(this.quizId);
    });
  }

  answerLabel(index: number): string {
    return String.fromCharCode(65 + index);
  }

  openQuestion(questionIndex: number): void {
    this.studio.goToQuestion(questionIndex);
  }

  selectAnswer(answerId: number): void {
    this.studio.selectAnswer(answerId);
  }

  goToPreviousQuestion(): void {
    this.studio.goToPreviousQuestion();
  }

  goToNextQuestion(): void {
    if (!this.canMoveForward()) {
      return;
    }

    this.studio.goToNextQuestion();
  }

  finishAttempt(): void {
    if (!this.canFinishAttempt()) {
      return;
    }

    this.studio.finishAttempt();
  }

  playAgain(): void {
    this.studio.startQuiz(this.quizId);
  }

  returnToCourse(): void {
    this.studio.clearAttempt();
    void this.router.navigate(['/courses', this.courseSlug]);
  }

  private createQuestionPreview(question: StudioQuestion): string {
    const compactPrompt = question.prompt.replace(/\s+/g, ' ').trim();

    if (compactPrompt.length <= 72) {
      return compactPrompt;
    }

    return `${compactPrompt.slice(0, 69).trimEnd()}...`;
  }

  private createDeterministicShuffle<T extends { id: number }>(items: T[], seed: string): T[] {
    return [...items].sort((left, right) => this.hashSeed(`${seed}:${left.id}`) - this.hashSeed(`${seed}:${right.id}`));
  }

  private hashSeed(seed: string): number {
    let hash = 0;

    for (let index = 0; index < seed.length; index++) {
      hash = (hash * 31 + seed.charCodeAt(index)) >>> 0;
    }

    return hash;
  }
}
