import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioAttemptReview, StudioQuestion } from './course-studio.service';

type QuestionAttemptRow = {
  attemptId: number;
  quizTitle: string;
  finishedAt: string;
  answeredCorrectly: boolean;
};

@Component({
  selector: 'app-course-question-insights-page',
  imports: [RouterLink, DatePipe, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-question-insights-page.component.html',
  styleUrl: './course-question-insights-page.component.scss'
})
export class CourseQuestionInsightsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);

  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly summaryLink = ['/courses', this.courseSlug];
  readonly quizInsightsLink = ['/courses', this.courseSlug, 'insights', 'quizzes'];

  readonly selectedQuestionId = signal<number | null>(this.parseId(this.route.snapshot.paramMap.get('questionId')));
  readonly reviewAttempts = signal<StudioAttemptReview[]>([]);
  readonly isLoadingReviews = signal(false);
  readonly reviewLoadError = signal<string | null>(null);

  readonly availableQuestions = computed(() => this.studio.questions());
  readonly activeQuestion = computed<StudioQuestion | null>(() => {
    const selectedId = this.selectedQuestionId();
    const questions = this.availableQuestions();

    if (selectedId !== null) {
      return questions.find((question) => question.id === selectedId) ?? questions[0] ?? null;
    }

    return questions[0] ?? null;
  });
  readonly questionAttemptRows = computed<QuestionAttemptRow[]>(() => {
    const activeQuestion = this.activeQuestion();

    if (!activeQuestion) {
      return [];
    }

    return this.reviewAttempts()
      .map((attempt) => {
        const question = attempt.questions.find((entry) => entry.questionId === activeQuestion.id);

        if (!question) {
          return null;
        }

        return {
          attemptId: attempt.id,
          quizTitle: attempt.quizTitle,
          finishedAt: attempt.finishedAt,
          answeredCorrectly: question.answeredCorrectly
        };
      })
      .filter((row): row is QuestionAttemptRow => Boolean(row));
  });
  readonly questionSeenCount = computed(() => this.questionAttemptRows().length);
  readonly questionCorrectCount = computed(() => this.questionAttemptRows().filter((row) => row.answeredCorrectly).length);
  readonly questionIncorrectCount = computed(() => this.questionAttemptRows().filter((row) => !row.answeredCorrectly).length);
  readonly questionAccuracyPercent = computed(() => {
    const seenCount = this.questionSeenCount();

    if (!seenCount) {
      return 0;
    }

    return Math.round((this.questionCorrectCount() / seenCount) * 100);
  });

  constructor() {
    this.coursesCatalogService.loadCourses();
    this.route.paramMap.subscribe((paramMap) => {
      this.selectedQuestionId.set(this.parseId(paramMap.get('questionId')));
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
          this.reviewAttempts.set(attempts.filter((attempt) => attempt.questions.length > 0));
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

  questionLink(questionId: number): unknown[] {
    return ['/courses', this.courseSlug, 'insights', 'questions', questionId];
  }

  attemptReviewLink(attemptId: number): unknown[] {
    return ['/courses', this.courseSlug, 'attempts', attemptId];
  }

  private parseId(value: string | null): number | null {
    const parsed = Number(value ?? Number.NaN);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private resolveLoadError(error: unknown): string {
    return extractApiMessage(error) ?? (error instanceof HttpErrorResponse ? 'Unable to load detailed question stats right now.' : 'Unable to load detailed question stats right now.');
  }
}
