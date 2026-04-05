import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioAttemptReview, StudioQuiz } from './course-studio.service';

type QuizAttemptCard = {
  id: number;
  scorePercent: number;
  correctAnswers: number;
  totalQuestions: number;
  finishedAt: string;
};

type QuizQuestionPerformance = {
  questionId: number;
  prompt: string;
  attempts: number;
  correctCount: number;
  accuracyPercent: number;
};

@Component({
  selector: 'app-course-quiz-insights-page',
  imports: [RouterLink, DatePipe, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-quiz-insights-page.component.html',
  styleUrl: './course-quiz-insights-page.component.scss'
})
export class CourseQuizInsightsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);

  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly summaryLink = ['/courses', this.courseSlug];
  readonly questionInsightsLink = ['/courses', this.courseSlug, 'insights', 'questions'];

  readonly selectedQuizId = signal<number | null>(this.parseId(this.route.snapshot.paramMap.get('quizId')));
  readonly reviewAttempts = signal<StudioAttemptReview[]>([]);
  readonly isLoadingReviews = signal(false);
  readonly reviewLoadError = signal<string | null>(null);

  readonly availableQuizzes = computed(() => this.studio.quizzes());
  readonly activeQuiz = computed(() => {
    const selectedId = this.selectedQuizId();
    const quizzes = this.availableQuizzes();

    if (selectedId !== null) {
      return quizzes.find((quiz) => quiz.id === selectedId) ?? quizzes[0] ?? null;
    }

    return quizzes[0] ?? null;
  });
  readonly activeQuizTitle = computed(() => this.activeQuiz()?.title ?? this.currentCourse()?.title ?? this.studio.courseTitle());
  readonly activeQuizId = computed(() => this.activeQuiz()?.id ?? null);
  readonly filteredAttempts = computed<QuizAttemptCard[]>(() => {
    const activeQuiz = this.activeQuiz();

    if (!activeQuiz) {
      return [];
    }

    return this.studio.attempts()
      .filter((attempt) => attempt.quizId === activeQuiz.id)
      .map((attempt) => ({
        id: attempt.id,
        scorePercent: attempt.totalQuestions ? Math.round((attempt.correctAnswers / attempt.totalQuestions) * 100) : 0,
        correctAnswers: attempt.correctAnswers,
        totalQuestions: attempt.totalQuestions,
        finishedAt: attempt.finishedAt
      }));
  });
  readonly filteredReviewAttempts = computed(() => {
    const activeQuiz = this.activeQuiz();

    if (!activeQuiz) {
      return [];
    }

    return this.reviewAttempts().filter((attempt) => attempt.quizId === activeQuiz.id && attempt.questions.length > 0);
  });
  readonly quizAttemptCount = computed(() => this.filteredAttempts().length);
  readonly quizAverageScore = computed(() => {
    const attempts = this.filteredAttempts();

    if (!attempts.length) {
      return 0;
    }

    return Math.round(attempts.reduce((sum, attempt) => sum + attempt.scorePercent, 0) / attempts.length);
  });
  readonly quizBestScore = computed(() => this.filteredAttempts().reduce((best, attempt) => Math.max(best, attempt.scorePercent), 0));
  readonly quizInProgressCount = computed(() => {
    const activeQuiz = this.activeQuiz();

    if (!activeQuiz) {
      return 0;
    }

    return this.studio.quizSessions().filter((session) => session.quizId === activeQuiz.id).length;
  });
  readonly quizQuestionPerformance = computed<QuizQuestionPerformance[]>(() => {
    const buckets = new Map<number, { prompt: string; attempts: number; correctCount: number }>();

    for (const attempt of this.filteredReviewAttempts()) {
      for (const question of attempt.questions) {
        const bucket = buckets.get(question.questionId) ?? {
          prompt: question.prompt,
          attempts: 0,
          correctCount: 0
        };

        bucket.attempts += 1;
        bucket.correctCount += Number(question.answeredCorrectly);
        buckets.set(question.questionId, bucket);
      }
    }

    return [...buckets.entries()]
      .map(([questionId, bucket]) => ({
        questionId,
        prompt: bucket.prompt,
        attempts: bucket.attempts,
        correctCount: bucket.correctCount,
        accuracyPercent: bucket.attempts ? Math.round((bucket.correctCount / bucket.attempts) * 100) : 0
      }))
      .sort((left, right) => right.attempts - left.attempts || left.prompt.localeCompare(right.prompt));
  });

  constructor() {
    this.coursesCatalogService.loadCourses();
    this.route.paramMap.subscribe((paramMap) => {
      this.selectedQuizId.set(this.parseId(paramMap.get('quizId')));
    });

    effect(() => {
      const course = this.currentCourse();

      if (!course) {
        return;
      }

      this.studio.loadCourseContext(course.id);
      this.isLoadingReviews.set(true);
      this.reviewLoadError.set(null);

      this.studio.loadAttemptReviews(course.id).subscribe({
        next: (attempts) => {
          this.reviewAttempts.set(attempts);
          this.isLoadingReviews.set(false);
        },
        error: (error: unknown) => {
          this.reviewAttempts.set([]);
          this.isLoadingReviews.set(false);
          this.reviewLoadError.set(this.resolveLoadError(error));
        }
      });
    });
  }

  quizLink(quizId: number): unknown[] {
    return ['/courses', this.courseSlug, 'insights', 'quizzes', quizId];
  }

  attemptReviewLink(attemptId: number): unknown[] {
    return ['/courses', this.courseSlug, 'attempts', attemptId];
  }

  quizModeLabel(mode: 'manual' | 'random' | 'category'): string {
    if (mode === 'manual') {
      return 'Manual';
    }

    if (mode === 'random') {
      return 'Random';
    }

    return 'Category';
  }

  quizSummary(quiz: StudioQuiz): string {
    if (quiz.mode === 'manual') {
      return `Fixed set of ${quiz.resolvedQuestionCount} question${quiz.resolvedQuestionCount === 1 ? '' : 's'}.`;
    }

    if (quiz.mode === 'random') {
      const drawCount = quiz.randomCount ?? quiz.resolvedQuestionCount;
      return `Draws ${drawCount} random question${drawCount === 1 ? '' : 's'} from the course bank.`;
    }

    return `Includes every question from ${quiz.categories.length} selected categor${quiz.categories.length === 1 ? 'y' : 'ies'}.`;
  }

  private parseId(value: string | null): number | null {
    const parsed = Number(value ?? Number.NaN);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private resolveLoadError(error: unknown): string {
    return extractApiMessage(error) ?? (error instanceof HttpErrorResponse ? 'Unable to load detailed quiz stats right now.' : 'Unable to load detailed quiz stats right now.');
  }
}
