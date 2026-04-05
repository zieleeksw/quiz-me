import { Component, computed, effect, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService } from './course-studio.service';

@Component({
  selector: 'app-course-studio-page',
  imports: [RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-studio-page.component.html',
  styleUrl: './course-studio-page.component.scss'
})
export class CourseStudioPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);

  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly summaryLink = ['/courses', this.courseSlug];
  readonly editorLink = ['/courses', this.courseSlug, 'editor'];

  constructor() {
    this.coursesCatalogService.loadCourses();

    effect(() => {
      const course = this.currentCourse();

      if (course) {
        this.studio.loadCourseContext(course.id);
      }
    });
  }

  quizSummary(quiz: {
    mode: 'manual' | 'random' | 'category';
    resolvedQuestionCount: number;
    randomCount: number | null;
    categories: { name: string }[];
  }): string {
    if (quiz.mode === 'manual') {
      return `This quiz uses a fixed set of ${this.formatQuestionCount(quiz.resolvedQuestionCount)}.`;
    }

    if (quiz.mode === 'random') {
      const randomCount = quiz.randomCount ?? quiz.resolvedQuestionCount;
      return `This quiz draws ${this.formatQuestionCount(randomCount)} from the full course bank each time it starts.`;
    }

    const categoryNames = quiz.categories.map((category) => category.name);

    if (!categoryNames.length) {
      return 'This quiz includes every question from the selected categories.';
    }

    return `This quiz includes every question from: ${this.formatNameList(categoryNames)}.`;
  }

  quizOrderSummary(quiz: { questionOrder: 'fixed' | 'random'; answerOrder: 'fixed' | 'random' }): string {
    return `${this.describeQuestionOrder(quiz.questionOrder)} and ${this.describeAnswerOrder(quiz.answerOrder)}.`;
  }

  quizTypeLabel(mode: 'manual' | 'random' | 'category'): string {
    if (mode === 'manual') {
      return 'Manual';
    }

    if (mode === 'random') {
      return 'Random';
    }

    return 'Category';
  }

  quizPlayLink(quizId: number): string[] {
    return ['/courses', this.courseSlug, 'quizzes', String(quizId), 'play'];
  }

  quizActionLabel(quizId: number): string {
    return this.studio.hasResumableAttemptForQuiz(quizId) ? 'Resume Quiz' : 'Start Quiz';
  }

  private describeQuestionOrder(order: 'fixed' | 'random'): string {
    return order === 'random' ? 'Questions are shown in random order' : 'Questions stay in the saved order';
  }

  private describeAnswerOrder(order: 'fixed' | 'random'): string {
    return order === 'random' ? 'answers are shown in random order' : 'answers stay in the saved order';
  }

  private formatQuestionCount(count: number): string {
    return `${count} question${count === 1 ? '' : 's'}`;
  }

  private formatNameList(names: string[]): string {
    if (names.length <= 1) {
      return names[0] ?? '';
    }

    if (names.length === 2) {
      return `${names[0]} and ${names[1]}`;
    }

    return `${names.slice(0, -1).join(', ')}, and ${names[names.length - 1]}`;
  }
}
