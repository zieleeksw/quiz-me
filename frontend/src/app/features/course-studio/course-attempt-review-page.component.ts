import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioAttemptReview, StudioAttemptReviewQuestion } from './course-studio.service';

@Component({
  selector: 'app-course-attempt-review-page',
  imports: [RouterLink, DatePipe, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-attempt-review-page.component.html',
  styleUrl: './course-attempt-review-page.component.scss'
})
export class CourseAttemptReviewPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);

  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly attemptId = Number(this.route.snapshot.paramMap.get('attemptId') ?? Number.NaN);
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly summaryLink = ['/courses', this.courseSlug];
  readonly quizzesLink = ['/courses', this.courseSlug, 'quizzes'];
  readonly focusedQuestionId = signal<number | null>(this.parseId(this.route.snapshot.queryParamMap.get('questionId')));

  readonly reviewState = signal<StudioAttemptReview | null>(null);
  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly currentQuestionIndex = signal(0);

  readonly questionNavItems = computed(() =>
    this.reviewState()?.questions.map((question, index) => ({
      id: question.questionId,
      number: index + 1,
      preview: this.createQuestionPreview(question.prompt),
      active: index === this.currentQuestionIndex(),
      answeredCorrectly: question.answeredCorrectly
    })) ?? []
  );
  readonly currentQuestion = computed(() => this.reviewState()?.questions[this.currentQuestionIndex()] ?? null);
  readonly scorePercent = computed(() => {
    const review = this.reviewState();

    if (!review || !review.totalQuestions) {
      return 0;
    }

    return Math.round((review.correctAnswers / review.totalQuestions) * 100);
  });

  constructor() {
    this.coursesCatalogService.loadCourses();
    this.route.queryParamMap.subscribe((queryParamMap) => {
      this.focusedQuestionId.set(this.parseId(queryParamMap.get('questionId')));
      this.syncFocusedQuestion();
    });

    effect(() => {
      const course = this.currentCourse();

      if (!course || !Number.isFinite(this.attemptId)) {
        return;
      }

      this.studio.loadCourseContext(course.id);
      this.isLoading.set(true);
      this.loadError.set(null);

      this.studio.loadAttemptReview(course.id, this.attemptId).subscribe({
        next: (review) => {
          this.reviewState.set(review);
          this.syncFocusedQuestion();
          this.isLoading.set(false);
        },
        error: (error: unknown) => {
          this.reviewState.set(null);
          this.isLoading.set(false);
          this.loadError.set(this.resolveLoadError(error));
        }
      });
    });
  }

  openQuestion(questionIndex: number): void {
    this.currentQuestionIndex.set(questionIndex);
  }

  answerLabel(index: number): string {
    return String.fromCharCode(65 + index);
  }

  isSelectedAnswer(question: StudioAttemptReviewQuestion, answerId: number): boolean {
    return question.selectedAnswerId === answerId;
  }

  isCorrectAnswer(question: StudioAttemptReviewQuestion, answerId: number): boolean {
    return question.correctAnswerId === answerId;
  }

  private syncFocusedQuestion(): void {
    const review = this.reviewState();
    const focusedQuestionId = this.focusedQuestionId();

    if (!review?.questions.length) {
      this.currentQuestionIndex.set(0);
      return;
    }

    if (focusedQuestionId === null) {
      this.currentQuestionIndex.set(0);
      return;
    }

    const focusedQuestionIndex = review.questions.findIndex((question) => question.questionId === focusedQuestionId);
    this.currentQuestionIndex.set(focusedQuestionIndex >= 0 ? focusedQuestionIndex : 0);
  }

  private createQuestionPreview(prompt: string): string {
    const compactPrompt = prompt.replace(/\s+/g, ' ').trim();

    if (compactPrompt.length <= 72) {
      return compactPrompt;
    }

    return `${compactPrompt.slice(0, 69).trimEnd()}...`;
  }

  private resolveLoadError(error: unknown): string {
    return extractApiMessage(error) ?? (error instanceof HttpErrorResponse ? 'Unable to load this attempt review right now.' : 'Unable to load this attempt review right now.');
  }

  private parseId(value: string | null): number | null {
    const parsed = Number(value ?? Number.NaN);
    return Number.isFinite(parsed) ? parsed : null;
  }
}
