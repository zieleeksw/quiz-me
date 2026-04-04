import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { catchError, finalize, forkJoin, map, of, tap } from 'rxjs';

type CategoryApiDto = {
  id: number;
  courseId: number;
  name: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

type QuestionCategoryApiDto = {
  id: number;
  name: string;
};

type QuestionAnswerApiDto = {
  id: number;
  displayOrder: number;
  content: string;
  correct: boolean;
};

type QuestionApiDto = {
  id: number;
  courseId: number;
  currentVersionNumber: number;
  createdAt: string;
  updatedAt: string;
  prompt: string;
  categories: QuestionCategoryApiDto[];
  answers: QuestionAnswerApiDto[];
};

type QuestionPageApiDto = {
  items: QuestionApiDto[];
  pageNumber: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

type QuestionVersionApiDto = {
  id: number;
  questionId: number;
  versionNumber: number;
  createdAt: string;
  prompt: string;
  categories: QuestionCategoryApiDto[];
  answers: QuestionAnswerApiDto[];
};

type SaveCategoryPayload = {
  name: string;
};

type SaveQuestionPayload = {
  prompt: string;
  answers: {
    content: string;
    correct: boolean;
  }[];
  categoryIds: number[];
};

export type StudioCategory = {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type StudioQuestionCategory = {
  id: number;
  name: string;
};

type QuestionOption = {
  id: number;
  displayOrder: number;
  content: string;
  correct: boolean;
};

type Question = {
  id: number;
  courseId: number;
  versionNumber: number;
  createdAt: string;
  updatedAt: string;
  prompt: string;
  categories: StudioQuestionCategory[];
  options: QuestionOption[];
  correctOptionIndex: number;
};

export type StudioQuestion = Question;

export type StudioQuestionVersion = {
  id: number;
  questionId: number;
  versionNumber: number;
  createdAt: string;
  prompt: string;
  categories: StudioQuestionCategory[];
  options: QuestionOption[];
  correctOptionIndex: number;
};

type QuizDefinition = {
  id: string;
  title: string;
  mode: 'manual' | 'random';
  questionIds: number[];
  randomCount: number | null;
};

type AttemptSummary = {
  id: string;
  quizTitle: string;
  correctAnswers: number;
  totalQuestions: number;
  finishedAt: string;
};

type ActiveAttempt = {
  sourceQuizId: string | null;
  quizTitle: string;
  questionIds: number[];
  currentIndex: number;
  answers: Record<number, number>;
  finished: boolean;
  result: {
    correctAnswers: number;
    totalQuestions: number;
  } | null;
};

@Injectable({ providedIn: 'root' })
export class CourseStudioService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;

  private readonly courseTitleState = signal('Spring Boot Associate Mastery Path');
  private readonly courseSubtitleState = signal(
    'One course can hold the full question bank, multiple targeted quizzes, and a random practice mode with shared aggregated stats.'
  );
  private readonly activeCourseIdState = signal<number | null>(null);
  private readonly loadingState = signal(false);
  private readonly loadedState = signal(false);
  private readonly loadErrorState = signal<string | null>(null);

  private readonly categoriesState = signal<StudioCategory[]>([]);
  private readonly questionsState = signal<Question[]>([]);
  private readonly questionPreviewItemsState = signal<Question[]>([]);
  private readonly questionPreviewPageNumberState = signal(0);
  private readonly questionPreviewPageSizeState = signal(5);
  private readonly questionPreviewTotalItemsState = signal(0);
  private readonly questionPreviewTotalPagesState = signal(0);
  private readonly questionPreviewLoadingState = signal(false);
  private readonly questionVersionsState = signal<Record<number, StudioQuestionVersion[]>>({});
  private readonly versionLoadingState = signal<number | null>(null);

  private readonly quizzesState = signal<QuizDefinition[]>([]);
  private readonly attemptsState = signal<AttemptSummary[]>([
    {
      id: this.createId('attempt'),
      quizTitle: 'Foundations Mock',
      correctAnswers: 14,
      totalQuestions: 20,
      finishedAt: 'Today'
    },
    {
      id: this.createId('attempt'),
      quizTitle: 'Random Drill',
      correctAnswers: 9,
      totalQuestions: 10,
      finishedAt: 'Yesterday'
    }
  ]);
  private readonly activeAttemptState = signal<ActiveAttempt | null>(null);

  readonly courseTitle = this.courseTitleState.asReadonly();
  readonly courseSubtitle = this.courseSubtitleState.asReadonly();
  readonly activeCourseId = this.activeCourseIdState.asReadonly();
  readonly isLoading = this.loadingState.asReadonly();
  readonly isLoaded = this.loadedState.asReadonly();
  readonly loadError = this.loadErrorState.asReadonly();
  readonly versionLoadingQuestionId = this.versionLoadingState.asReadonly();

  readonly categories = this.categoriesState.asReadonly();
  readonly questions = this.questionsState.asReadonly();
  readonly questionPreviewItems = this.questionPreviewItemsState.asReadonly();
  readonly questionPreviewPageNumber = this.questionPreviewPageNumberState.asReadonly();
  readonly questionPreviewPageSize = this.questionPreviewPageSizeState.asReadonly();
  readonly questionPreviewTotalItems = this.questionPreviewTotalItemsState.asReadonly();
  readonly questionPreviewTotalPages = this.questionPreviewTotalPagesState.asReadonly();
  readonly isQuestionPreviewLoading = this.questionPreviewLoadingState.asReadonly();
  readonly availableCategories = computed(() => this.categoriesState());
  readonly categorySummaries = computed(() =>
    this.categoriesState().map((category) => ({
      id: category.id,
      name: category.name,
      questionCount: this.questionsState().filter((question) =>
        question.categories.some((questionCategory) => questionCategory.id === category.id)
      ).length
    }))
  );

  readonly quizzes = computed(() =>
    this.quizzesState().map((quiz) => ({
      ...quiz,
      resolvedQuestionCount: this.resolveQuestionIdsForQuiz(quiz).length
    }))
  );
  readonly attempts = computed(() => this.attemptsState());
  readonly activeAttempt = computed(() => this.activeAttemptState());
  readonly totalQuestions = computed(() => this.questionsState().length);
  readonly totalQuizzes = computed(() => this.quizzesState().length);
  readonly totalAttempts = computed(() => this.attemptsState().length);
  readonly averageScore = computed(() => {
    const attempts = this.attemptsState();

    if (!attempts.length) {
      return 0;
    }

    const total = attempts.reduce((accumulator, attempt) => accumulator + attempt.correctAnswers / attempt.totalQuestions, 0);
    return Math.round((total / attempts.length) * 100);
  });

  readonly activeAttemptQuestions = computed(() => {
    const activeAttempt = this.activeAttemptState();

    if (!activeAttempt) {
      return [];
    }

    return activeAttempt.questionIds
      .map((questionId) => this.questionsState().find((question) => question.id === questionId))
      .filter((question): question is Question => Boolean(question));
  });

  readonly currentQuestion = computed(() => {
    const activeAttempt = this.activeAttemptState();
    const questions = this.activeAttemptQuestions();

    if (!activeAttempt || !questions.length) {
      return null;
    }

    return questions[activeAttempt.currentIndex] ?? null;
  });

  readonly currentAnswerIndex = computed(() => {
    const activeAttempt = this.activeAttemptState();
    const currentQuestion = this.currentQuestion();

    if (!activeAttempt || !currentQuestion) {
      return null;
    }

    return activeAttempt.answers[currentQuestion.id] ?? null;
  });

  loadCourseContext(courseId: number, force = false): void {
    if (this.loadingState()) {
      return;
    }

    if (this.activeCourseIdState() === courseId && this.loadedState() && !force) {
      return;
    }

    this.activeCourseIdState.set(courseId);
    this.loadingState.set(true);
    this.loadedState.set(false);
    this.loadErrorState.set(null);
    this.questionPreviewItemsState.set([]);
    this.questionPreviewPageNumberState.set(0);
    this.questionPreviewTotalItemsState.set(0);
    this.questionPreviewTotalPagesState.set(0);
    this.questionVersionsState.set({});
    this.activeAttemptState.set(null);

    forkJoin({
      categories: this.http.get<CategoryApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/categories`),
      questions: this.http.get<QuestionApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/questions`)
    })
      .pipe(
        map(({ categories, questions }) => ({
          categories: categories.map((category) => this.mapCategory(category)),
          questions: questions.map((question) => this.mapQuestion(question))
        })),
        tap(({ categories, questions }) => {
          this.categoriesState.set(categories);
          this.questionsState.set(questions);
          this.resetMockQuizState(questions);
          this.loadedState.set(true);
        }),
        catchError(() => {
          this.categoriesState.set([]);
          this.questionsState.set([]);
          this.resetMockQuizState([]);
          this.loadErrorState.set('Unable to load this course editor right now.');
          this.loadedState.set(true);
          return of(null);
        })
      )
      .subscribe({
        complete: () => {
          this.loadingState.set(false);
        }
      });
  }

  loadQuestionPreview(
    courseId: number,
    page = 0,
    size = 5,
    search = '',
    categoryId: number | null = null
  ) {
    this.questionPreviewLoadingState.set(true);

    const params = new URLSearchParams({
      page: String(page),
      size: String(size)
    });

    if (search.trim()) {
      params.set('search', search.trim());
    }

    if (categoryId !== null) {
      params.set('categoryId', String(categoryId));
    }

    return this.http.get<QuestionPageApiDto>(`${this.apiBaseUrl}/courses/${courseId}/questions/preview?${params.toString()}`).pipe(
      map((response) => ({
        ...response,
        items: response.items.map((question) => this.mapQuestion(question))
      })),
      tap((response) => {
        this.questionPreviewItemsState.set(response.items);
        this.questionPreviewPageNumberState.set(response.pageNumber);
        this.questionPreviewPageSizeState.set(response.pageSize);
        this.questionPreviewTotalItemsState.set(response.totalItems);
        this.questionPreviewTotalPagesState.set(response.totalPages);
      }),
      finalize(() => {
        this.questionPreviewLoadingState.set(false);
      })
    );
  }

  createCategory(name: string) {
    const courseId = this.requireActiveCourseId();

    return this.http.post<CategoryApiDto>(`${this.apiBaseUrl}/courses/${courseId}/categories`, { name } satisfies SaveCategoryPayload).pipe(
      map((category) => this.mapCategory(category)),
      tap((category) => {
        this.categoriesState.update((categories) => this.sortCategories([...categories, category]));
      })
    );
  }

  renameCategory(categoryId: number, name: string) {
    const courseId = this.requireActiveCourseId();

    return this.http
      .put<CategoryApiDto>(`${this.apiBaseUrl}/courses/${courseId}/categories/${categoryId}`, { name } satisfies SaveCategoryPayload)
      .pipe(
        map((category) => this.mapCategory(category)),
        tap((updatedCategory) => {
          this.categoriesState.update((categories) =>
            this.sortCategories(categories.map((category) => (category.id === updatedCategory.id ? updatedCategory : category)))
          );
          this.questionsState.update((questions) =>
            questions.map((question) => ({
              ...question,
              categories: question.categories.map((category) =>
                category.id === updatedCategory.id ? { id: updatedCategory.id, name: updatedCategory.name } : category
              )
            }))
          );
          this.questionPreviewItemsState.update((questions) =>
            questions.map((question) => ({
              ...question,
              categories: question.categories.map((category) =>
                category.id === updatedCategory.id ? { id: updatedCategory.id, name: updatedCategory.name } : category
              )
            }))
          );
          this.questionVersionsState.update((versionsMap) =>
            Object.fromEntries(
              Object.entries(versionsMap).map(([questionId, versions]) => [
                Number(questionId),
                versions.map((version) => ({
                  ...version,
                  categories: version.categories.map((category) =>
                    category.id === updatedCategory.id ? { id: updatedCategory.id, name: updatedCategory.name } : category
                  )
                }))
              ])
            )
          );
        })
      );
  }

  deleteCategory(categoryId: number) {
    const courseId = this.requireActiveCourseId();

    return this.http.delete<void>(`${this.apiBaseUrl}/courses/${courseId}/categories/${categoryId}`).pipe(
      tap(() => {
        this.categoriesState.update((categories) => categories.filter((category) => category.id !== categoryId));
        this.questionsState.update((questions) =>
          questions.map((question) => ({
            ...question,
            categories: question.categories.filter((category) => category.id !== categoryId)
          }))
        );
        this.questionPreviewItemsState.update((questions) =>
          questions.map((question) => ({
            ...question,
            categories: question.categories.filter((category) => category.id !== categoryId)
          }))
        );
      })
    );
  }

  createQuestion(payload: SaveQuestionPayload) {
    const courseId = this.requireActiveCourseId();

    return this.http.post<QuestionApiDto>(`${this.apiBaseUrl}/courses/${courseId}/questions`, payload).pipe(
      map((question) => this.mapQuestion(question)),
      tap((question) => {
        this.questionsState.update((questions) => [question, ...questions]);
        this.resetQuestionSelectionForMockQuizzes();
      })
    );
  }

  updateQuestion(questionId: number, payload: SaveQuestionPayload) {
    const courseId = this.requireActiveCourseId();

    return this.http.put<QuestionApiDto>(`${this.apiBaseUrl}/courses/${courseId}/questions/${questionId}`, payload).pipe(
      map((question) => this.mapQuestion(question)),
      tap((updatedQuestion) => {
        this.questionsState.update((questions) =>
          questions.map((question) => (question.id === updatedQuestion.id ? updatedQuestion : question))
        );
        this.questionVersionsState.update((versionsMap) => {
          const nextMap = { ...versionsMap };
          delete nextMap[questionId];
          return nextMap;
        });
      })
    );
  }

  loadQuestionVersions(questionId: number, force = false) {
    const courseId = this.requireActiveCourseId();

    if (this.questionVersionsState()[questionId] && !force) {
      return of(this.questionVersionsState()[questionId]);
    }

    this.versionLoadingState.set(questionId);

    return this.http.get<QuestionVersionApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/questions/${questionId}/versions`).pipe(
      map((versions) => versions.map((version) => this.mapQuestionVersion(version))),
      tap((versions) => {
        this.questionVersionsState.update((versionsMap) => ({
          ...versionsMap,
          [questionId]: versions
        }));
      }),
      catchError((error) => {
        this.questionVersionsState.update((versionsMap) => ({
          ...versionsMap,
          [questionId]: []
        }));
        throw error;
      }),
      finalize(() => {
        this.versionLoadingState.set(null);
      })
    );
  }

  getQuestionVersions(questionId: number): StudioQuestionVersion[] {
    return this.questionVersionsState()[questionId] ?? [];
  }

  addQuiz(payload: { title: string; mode: 'manual' | 'random'; questionIds: number[]; randomCount: number | null }): void {
    const quiz: QuizDefinition = {
      id: this.createId('quiz'),
      title: payload.title,
      mode: payload.mode,
      questionIds: payload.mode === 'manual' ? payload.questionIds : [],
      randomCount: payload.mode === 'random' ? payload.randomCount : null
    };

    this.quizzesState.update((quizzes) => [quiz, ...quizzes]);
  }

  deleteQuiz(quizId: string): void {
    this.quizzesState.update((quizzes) => quizzes.filter((quiz) => quiz.id !== quizId));

    if (this.activeAttemptState()?.sourceQuizId === quizId) {
      this.activeAttemptState.set(null);
    }
  }

  startQuiz(quizId: string): void {
    const quiz = this.quizzesState().find((entry) => entry.id === quizId);

    if (!quiz) {
      return;
    }

    const resolvedQuestionIds = this.resolveQuestionIdsForQuiz(quiz);

    if (!resolvedQuestionIds.length) {
      return;
    }

    this.activeAttemptState.set({
      sourceQuizId: quiz.id,
      quizTitle: quiz.title,
      questionIds: resolvedQuestionIds,
      currentIndex: 0,
      answers: {},
      finished: false,
      result: null
    });
  }

  startRandomPractice(randomCount: number): void {
    const questionIds = this.pickRandomQuestionIds(this.questionsState().map((question) => question.id), randomCount);

    if (!questionIds.length) {
      return;
    }

    this.activeAttemptState.set({
      sourceQuizId: null,
      quizTitle: `Random practice (${questionIds.length} questions)`,
      questionIds,
      currentIndex: 0,
      answers: {},
      finished: false,
      result: null
    });
  }

  selectAnswer(optionIndex: number): void {
    const currentQuestion = this.currentQuestion();

    if (!currentQuestion) {
      return;
    }

    this.activeAttemptState.update((activeAttempt) => {
      if (!activeAttempt || activeAttempt.finished) {
        return activeAttempt;
      }

      return {
        ...activeAttempt,
        answers: {
          ...activeAttempt.answers,
          [currentQuestion.id]: optionIndex
        }
      };
    });
  }

  goToNextQuestion(): void {
    this.activeAttemptState.update((activeAttempt) => {
      if (!activeAttempt || activeAttempt.finished) {
        return activeAttempt;
      }

      const nextIndex = Math.min(activeAttempt.currentIndex + 1, activeAttempt.questionIds.length - 1);

      return {
        ...activeAttempt,
        currentIndex: nextIndex
      };
    });
  }

  goToPreviousQuestion(): void {
    this.activeAttemptState.update((activeAttempt) => {
      if (!activeAttempt || activeAttempt.finished) {
        return activeAttempt;
      }

      const nextIndex = Math.max(activeAttempt.currentIndex - 1, 0);

      return {
        ...activeAttempt,
        currentIndex: nextIndex
      };
    });
  }

  finishAttempt(): void {
    const activeAttempt = this.activeAttemptState();

    if (!activeAttempt || activeAttempt.finished) {
      return;
    }

    const questions = this.activeAttemptQuestions();
    const correctAnswers = questions.reduce((accumulator, question) => {
      return accumulator + Number(activeAttempt.answers[question.id] === question.correctOptionIndex);
    }, 0);

    const result = {
      correctAnswers,
      totalQuestions: questions.length
    };

    this.activeAttemptState.set({
      ...activeAttempt,
      finished: true,
      result
    });

    this.attemptsState.update((attempts) => [
      {
        id: this.createId('attempt'),
        quizTitle: activeAttempt.quizTitle,
        correctAnswers,
        totalQuestions: questions.length,
        finishedAt: 'Just now'
      },
      ...attempts
    ]);
  }

  clearAttempt(): void {
    this.activeAttemptState.set(null);
  }

  questionUsageCount(questionId: number): number {
    return this.quizzesState().reduce((accumulator, quiz) => {
      if (quiz.mode === 'manual') {
        return accumulator + Number(quiz.questionIds.includes(questionId));
      }

      return accumulator;
    }, 0);
  }

  private mapCategory(category: CategoryApiDto): StudioCategory {
    return {
      id: category.id,
      name: category.name,
      active: category.active,
      createdAt: category.createdAt,
      updatedAt: category.updatedAt
    };
  }

  private mapQuestion(question: QuestionApiDto): Question {
    const options = [...question.answers].sort((left, right) => left.displayOrder - right.displayOrder);

    return {
      id: question.id,
      courseId: question.courseId,
      versionNumber: question.currentVersionNumber,
      createdAt: question.createdAt,
      updatedAt: question.updatedAt,
      prompt: question.prompt,
      categories: question.categories,
      options,
      correctOptionIndex: options.findIndex((option) => option.correct)
    };
  }

  private mapQuestionVersion(version: QuestionVersionApiDto): StudioQuestionVersion {
    const options = [...version.answers].sort((left, right) => left.displayOrder - right.displayOrder);

    return {
      id: version.id,
      questionId: version.questionId,
      versionNumber: version.versionNumber,
      createdAt: version.createdAt,
      prompt: version.prompt,
      categories: version.categories,
      options,
      correctOptionIndex: options.findIndex((option) => option.correct)
    };
  }

  private resolveQuestionIdsForQuiz(quiz: QuizDefinition): number[] {
    if (quiz.mode === 'manual') {
      return quiz.questionIds.filter((questionId) => this.questionsState().some((question) => question.id === questionId));
    }

    const questionIds = this.questionsState().map((question) => question.id);
    return this.pickRandomQuestionIds(questionIds, quiz.randomCount ?? questionIds.length);
  }

  private pickRandomQuestionIds(questionIds: number[], count: number): number[] {
    const shuffled = [...questionIds].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, Math.min(count, questionIds.length));
  }

  private resetMockQuizState(questions: Question[]): void {
    const questionIds = questions.map((question) => question.id);

    this.quizzesState.set([
      {
        id: this.createId('quiz'),
        title: 'Spring Boot Foundations',
        mode: 'manual',
        questionIds: questionIds.slice(0, 3),
        randomCount: null
      },
      {
        id: this.createId('quiz'),
        title: 'Random Checkpoint',
        mode: 'random',
        questionIds: [],
        randomCount: Math.min(3, questionIds.length || 3)
      }
    ]);
  }

  private resetQuestionSelectionForMockQuizzes(): void {
    this.quizzesState.update((quizzes) =>
      quizzes.map((quiz, index) => {
        if (quiz.mode === 'manual' && index === 0 && !quiz.questionIds.length) {
          return {
            ...quiz,
            questionIds: this.questionsState()
              .slice(0, 3)
              .map((question) => question.id)
          };
        }

        return quiz;
      })
    );
  }

  private sortCategories(categories: StudioCategory[]): StudioCategory[] {
    return [...categories].sort((left, right) => left.name.localeCompare(right.name));
  }

  private requireActiveCourseId(): number {
    const courseId = this.activeCourseIdState();

    if (!courseId) {
      throw new Error('Course context is missing.');
    }

    return courseId;
  }

  private createId(prefix: string): string {
    return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
  }
}
