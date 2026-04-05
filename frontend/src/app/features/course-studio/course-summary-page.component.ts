import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService } from './course-studio.service';

type SummarySegment = {
  label: string;
  value: number;
  percent: number;
  color: string;
  note: string;
};

type CategoryBar = {
  id: number;
  name: string;
  questionCount: number;
  fillPercent: number;
};

type AttemptCard = {
  id: number;
  quizTitle: string;
  correctAnswers: number;
  totalQuestions: number;
  finishedAt: string;
  scorePercent: number;
};

type QuizInsightPreview = {
  id: number;
  title: string;
  attempts: number;
  averageScore: number;
};

@Component({
  selector: 'app-course-summary-page',
  imports: [RouterLink, DatePipe, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-summary-page.component.html',
  styleUrl: './course-summary-page.component.scss'
})
export class CourseSummaryPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);
  private readonly chartPalette = ['#d96b43', '#226c63', '#c4932c', '#7a8f3b', '#1f4f8c', '#b76c2d'];

  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly quizzesLink = ['/courses', this.courseSlug, 'quizzes'];
  readonly editorLink = ['/courses', this.courseSlug, 'editor'];
  readonly questionInsightsLink = ['/courses', this.courseSlug, 'insights', 'questions'];

  readonly quizFormatSegments = computed<SummarySegment[]>(() => {
    const quizzes = this.studio.quizzes();
    const total = quizzes.length;
    const counts = [
      { label: 'Manual', value: quizzes.filter((quiz) => quiz.mode === 'manual').length, note: 'Saved question sets with a fixed scope.' },
      { label: 'Random', value: quizzes.filter((quiz) => quiz.mode === 'random').length, note: 'Fresh draws from the full course bank.' },
      { label: 'Category', value: quizzes.filter((quiz) => quiz.mode === 'category').length, note: 'Every question from selected categories.' }
    ];

    return counts
      .filter((entry) => entry.value > 0)
      .map((entry, index) => ({
        ...entry,
        percent: total ? Math.round((entry.value / total) * 100) : 0,
        color: this.chartPalette[index % this.chartPalette.length]
      }));
  });

  readonly quizCoverageSegments = computed<SummarySegment[]>(() => {
    const buckets = new Map<string, number>();

    for (const quiz of this.studio.quizzes()) {
      if (quiz.mode === 'category' && quiz.categories.length) {
        const distributedWeight = 1 / quiz.categories.length;

        for (const category of quiz.categories) {
          buckets.set(category.name, (buckets.get(category.name) ?? 0) + distributedWeight);
        }

        continue;
      }

      buckets.set('Whole Course', (buckets.get('Whole Course') ?? 0) + 1);
    }

    const total = [...buckets.values()].reduce((sum, value) => sum + value, 0);

    return [...buckets.entries()]
      .sort((left, right) => right[1] - left[1])
      .map(([label, value], index) => ({
        label,
        value,
        percent: total ? Math.round((value / total) * 100) : 0,
        color: this.chartPalette[index % this.chartPalette.length],
        note:
          label === 'Whole Course'
            ? 'Manual and random quizzes pull from the whole bank.'
            : 'Category quizzes share their weight across selected categories.'
      }));
  });

  readonly questionCategoryBars = computed<CategoryBar[]>(() => {
    const categories = this.studio.categorySummaries().filter((category) => category.questionCount > 0);
    const maxQuestionCount = Math.max(...categories.map((category) => category.questionCount), 0);

    return categories
      .sort((left, right) => right.questionCount - left.questionCount || left.name.localeCompare(right.name))
      .map((category) => ({
        ...category,
        fillPercent: maxQuestionCount ? Math.round((category.questionCount / maxQuestionCount) * 100) : 0
      }));
  });

  readonly recentAttempts = computed<AttemptCard[]>(() =>
    [...this.studio.attempts()]
      .sort((left, right) => new Date(right.finishedAt).getTime() - new Date(left.finishedAt).getTime())
      .slice(0, 6)
      .map((attempt) => ({
        ...attempt,
        scorePercent: attempt.totalQuestions ? Math.round((attempt.correctAnswers / attempt.totalQuestions) * 100) : 0
      }))
  );
  readonly quizInsightPreviews = computed<QuizInsightPreview[]>(() =>
    this.studio.quizzes().map((quiz) => {
      const attempts = this.studio.attempts().filter((attempt) => attempt.quizId === quiz.id);

      return {
        id: quiz.id,
        title: quiz.title,
        attempts: attempts.length,
        averageScore: attempts.length
          ? Math.round(attempts.reduce((sum, attempt) => sum + (attempt.totalQuestions ? (attempt.correctAnswers / attempt.totalQuestions) * 100 : 0), 0) / attempts.length)
          : 0
      };
    })
  );

  readonly bestAttemptScore = computed(() =>
    this.studio.attempts().reduce((best, attempt) => Math.max(best, attempt.totalQuestions ? Math.round((attempt.correctAnswers / attempt.totalQuestions) * 100) : 0), 0)
  );
  readonly inProgressSessions = computed(() => this.studio.quizSessions().length);
  readonly averageQuestionsPerQuiz = computed(() => {
    const quizzes = this.studio.quizzes();

    if (!quizzes.length) {
      return 0;
    }

    const totalQuestions = quizzes.reduce((sum, quiz) => sum + quiz.resolvedQuestionCount, 0);
    return Math.round((totalQuestions / quizzes.length) * 10) / 10;
  });
  readonly completionShare = computed(() => {
    const completedAttempts = this.studio.totalAttempts();
    const activeSessions = this.studio.quizSessions().length;
    const totalRuns = completedAttempts + activeSessions;

    if (!totalRuns) {
      return 0;
    }

    return Math.round((completedAttempts / totalRuns) * 100);
  });
  readonly quizFormatChartBackground = computed(() => this.buildConicGradient(this.quizFormatSegments()));
  readonly quizCoverageChartBackground = computed(() => this.buildConicGradient(this.quizCoverageSegments()));

  constructor() {
    this.coursesCatalogService.loadCourses();

    effect(() => {
      const course = this.currentCourse();

      if (course) {
        this.studio.loadCourseContext(course.id);
      }
    });
  }

  attemptReviewLink(attemptId: number): unknown[] {
    return ['/courses', this.courseSlug, 'attempts', attemptId];
  }

  quizInsightsLink(quizId: number): unknown[] {
    return ['/courses', this.courseSlug, 'insights', 'quizzes', quizId];
  }

  private buildConicGradient(segments: SummarySegment[]): string {
    if (!segments.length) {
      return 'conic-gradient(rgba(49, 65, 95, 0.14) 0deg 360deg)';
    }

    let startAngle = 0;
    const stops = segments.map((segment) => {
      const degrees = (segment.value / segments.reduce((sum, item) => sum + item.value, 0)) * 360;
      const endAngle = startAngle + degrees;
      const stop = `${segment.color} ${startAngle}deg ${endAngle}deg`;

      startAngle = endAngle;
      return stop;
    });

    return `conic-gradient(${stops.join(', ')})`;
  }
}
