import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioQuestion } from './course-studio.service';

@Component({
  selector: 'app-course-quiz-editor-page',
  imports: [ReactiveFormsModule, RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-quiz-editor-page.component.html',
  styleUrl: './course-quiz-editor-page.component.scss'
})
export class CourseQuizEditorPageComponent {
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
  readonly quizMessage = signal('');
  readonly formInitializedForQuizId = signal<string | null>(null);

  readonly quizForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(4)]],
    mode: ['manual' as 'manual' | 'random', [Validators.required]],
    randomCount: [10, [Validators.required, Validators.min(1)]]
  });

  readonly filteredQuestions = computed(() => {
    const normalizedSearch = this.quizBankSearch().trim().toLowerCase();
    const activeCategory = this.quizCategoryFilter();

    return this.studio.questions().filter((question) => {
      const matchesCategory =
        activeCategory === 'All' || question.categories.some((category) => category.id === activeCategory);
      const matchesSearch =
        !normalizedSearch ||
        question.prompt.toLowerCase().includes(normalizedSearch) ||
        question.categories.some((category) => category.name.toLowerCase().includes(normalizedSearch));

      return matchesCategory && matchesSearch;
    });
  });

  readonly selectedQuestions = computed(() =>
    this.selectedQuestionIds()
      .map((questionId) => this.studio.questions().find((question) => question.id === questionId))
      .filter((question): question is StudioQuestion => Boolean(question))
  );

  readonly canCreateManualQuiz = computed(() => this.selectedQuestionIds().length > 0);

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
  }

  setQuizBankSearch(value: string): void {
    this.quizBankSearch.set(value);
  }

  setQuizCategoryFilter(category: number | 'All'): void {
    this.quizCategoryFilter.set(category);
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
    this.quizMessage.set('Quiz added to the course preview.');
  }
}
