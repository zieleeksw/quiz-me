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

type QuizCategoryApiDto = {
  id: number;
  name: string;
};

type QuizApiDto = {
  id: number;
  courseId: number;
  active: boolean;
  currentVersionNumber: number;
  createdAt: string;
  updatedAt: string;
  title: string;
  mode: 'manual' | 'random' | 'category';
  randomCount: number | null;
  questionOrder: 'fixed' | 'random';
  answerOrder: 'fixed' | 'random';
  questionIds: number[];
  categories: QuizCategoryApiDto[];
};

type QuizVersionApiDto = {
  id: number;
  quizId: number;
  versionNumber: number;
  createdAt: string;
  title: string;
  mode: 'manual' | 'random' | 'category';
  randomCount: number | null;
  questionOrder: 'fixed' | 'random';
  answerOrder: 'fixed' | 'random';
  questionIds: number[];
  categories: QuizCategoryApiDto[];
};

type SaveQuizPayload = {
  title: string;
  mode: 'manual' | 'random' | 'category';
  randomCount: number | null;
  questionOrder: 'fixed' | 'random';
  answerOrder: 'fixed' | 'random';
  questionIds: number[];
  categoryIds: number[];
};

type QuizAttemptApiDto = {
  id: number;
  courseId: number;
  quizId: number;
  userId: number;
  quizTitle: string;
  correctAnswers: number;
  totalQuestions: number;
  finishedAt: string;
};

type QuizSessionApiDto = {
  id: number;
  courseId: number;
  quizId: number;
  userId: number;
  quizTitle: string;
  questionIds: number[];
  currentIndex: number;
  answers: Record<number, number>;
  updatedAt: string;
};

type SubmitQuizAttemptPayload = {
  answers: {
    questionId: number;
    answerId: number;
  }[];
};

type UpdateQuizSessionPayload = {
  currentIndex: number;
  answers: {
    questionId: number;
    answerId: number;
  }[];
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
  id: number;
  courseId: number;
  active: boolean;
  currentVersionNumber: number;
  createdAt: string;
  updatedAt: string;
  title: string;
  mode: 'manual' | 'random' | 'category';
  questionIds: number[];
  randomCount: number | null;
  questionOrder: 'fixed' | 'random';
  answerOrder: 'fixed' | 'random';
  categories: StudioQuestionCategory[];
};

export type StudioQuiz = QuizDefinition & {
  resolvedQuestionCount: number;
};

export type StudioQuizVersion = {
  id: number;
  quizId: number;
  versionNumber: number;
  createdAt: string;
  title: string;
  mode: 'manual' | 'random' | 'category';
  questionIds: number[];
  randomCount: number | null;
  questionOrder: 'fixed' | 'random';
  answerOrder: 'fixed' | 'random';
  categories: StudioQuestionCategory[];
};

type AttemptSummary = {
  id: number;
  quizId: number;
  quizTitle: string;
  correctAnswers: number;
  totalQuestions: number;
  finishedAt: string;
};

type ActiveAttempt = {
  sessionId: number | null;
  sourceQuizId: number | null;
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
    'One course can hold the full question bank, manual quizzes, random draws, and category-based variants with shared aggregated stats.'
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
  private readonly questionVersionLoadingState = signal<number | null>(null);

  private readonly quizzesState = signal<QuizDefinition[]>([]);
  private readonly quizVersionsState = signal<Record<number, StudioQuizVersion[]>>({});
  private readonly quizVersionLoadingState = signal<number | null>(null);
  private readonly quizSessionsState = signal<QuizSessionApiDto[]>([]);
  private readonly attemptsState = signal<AttemptSummary[]>([]);
  private readonly attemptSubmittingState = signal(false);
  private readonly attemptSubmitErrorState = signal<string | null>(null);
  private readonly activeAttemptState = signal<ActiveAttempt | null>(null);

  readonly courseTitle = this.courseTitleState.asReadonly();
  readonly courseSubtitle = this.courseSubtitleState.asReadonly();
  readonly activeCourseId = this.activeCourseIdState.asReadonly();
  readonly isLoading = this.loadingState.asReadonly();
  readonly isLoaded = this.loadedState.asReadonly();
  readonly loadError = this.loadErrorState.asReadonly();
  readonly versionLoadingQuestionId = this.questionVersionLoadingState.asReadonly();
  readonly versionLoadingQuizId = this.quizVersionLoadingState.asReadonly();
  readonly isSubmittingAttempt = this.attemptSubmittingState.asReadonly();
  readonly attemptSubmitError = this.attemptSubmitErrorState.asReadonly();

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

  readonly allQuizzes = computed<StudioQuiz[]>(() =>
    this.quizzesState().map((quiz) => ({
      ...quiz,
      resolvedQuestionCount: this.resolveQuestionIdsForQuiz(quiz).length
    }))
  );
  readonly quizzes = computed<StudioQuiz[]>(() => this.allQuizzes().filter((quiz) => quiz.active));
  readonly quizSessions = this.quizSessionsState.asReadonly();
  readonly attempts = computed(() => this.attemptsState());
  readonly activeAttempt = computed(() => this.activeAttemptState());
  readonly totalQuestions = computed(() => this.questionsState().length);
  readonly totalQuizzes = computed(() => this.quizzes().length);
  readonly totalArchivedQuizzes = computed(() => this.allQuizzes().filter((quiz) => !quiz.active).length);
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

  readonly currentAnswerId = computed(() => {
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
    this.quizVersionsState.set({});
    this.questionVersionLoadingState.set(null);
    this.quizVersionLoadingState.set(null);
    this.quizSessionsState.set([]);
    this.attemptsState.set([]);
    this.attemptSubmittingState.set(false);
    this.attemptSubmitErrorState.set(null);
    this.activeAttemptState.set(null);

    forkJoin({
      categories: this.http.get<CategoryApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/categories`),
      questions: this.http.get<QuestionApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/questions`),
      quizzes: this.http.get<QuizApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/quizzes`),
      sessions: this.http.get<QuizSessionApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/sessions`),
      attempts: this.http.get<QuizAttemptApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/attempts`)
    })
      .pipe(
        map(({ categories, questions, quizzes, sessions, attempts }) => ({
          categories: categories.map((category) => this.mapCategory(category)),
          questions: questions.map((question) => this.mapQuestion(question)),
          quizzes: quizzes.map((quiz) => this.mapQuiz(quiz)),
          sessions,
          attempts: attempts.map((attempt) => this.mapAttempt(attempt))
        })),
        tap(({ categories, questions, quizzes, sessions, attempts }) => {
          this.categoriesState.set(categories);
          this.questionsState.set(questions);
          this.quizzesState.set(quizzes);
          this.quizSessionsState.set(sessions);
          this.attemptsState.set(attempts);
          this.loadedState.set(true);
        }),
        catchError(() => {
          this.categoriesState.set([]);
          this.questionsState.set([]);
          this.quizzesState.set([]);
          this.quizSessionsState.set([]);
          this.attemptsState.set([]);
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
          this.quizzesState.update((quizzes) =>
            quizzes.map((quiz) => ({
              ...quiz,
              categories: quiz.categories.map((category) =>
                category.id === updatedCategory.id ? { id: updatedCategory.id, name: updatedCategory.name } : category
              )
            }))
          );
          this.quizVersionsState.update((versionsMap) =>
            Object.fromEntries(
              Object.entries(versionsMap).map(([quizId, versions]) => [
                Number(quizId),
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
        this.quizzesState.update((quizzes) =>
          quizzes.map((quiz) => ({
            ...quiz,
            categories: quiz.categories.filter((category) => category.id !== categoryId)
          }))
        );
        this.quizVersionsState.update((versionsMap) =>
          Object.fromEntries(
            Object.entries(versionsMap).map(([quizId, versions]) => [
              Number(quizId),
              versions.map((version) => ({
                ...version,
                categories: version.categories.filter((category) => category.id !== categoryId)
              }))
            ])
          )
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

    this.questionVersionLoadingState.set(questionId);

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
        this.questionVersionLoadingState.set(null);
      })
    );
  }

  getQuestionVersions(questionId: number): StudioQuestionVersion[] {
    return this.questionVersionsState()[questionId] ?? [];
  }

  createQuiz(payload: SaveQuizPayload) {
    const courseId = this.requireActiveCourseId();

    return this.http.post<QuizApiDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes`, payload).pipe(
      map((quiz) => this.mapQuiz(quiz)),
      tap((quiz) => {
        this.quizzesState.update((quizzes) => [quiz, ...quizzes]);
      })
    );
  }

  updateQuiz(
    quizId: number,
    payload: SaveQuizPayload
  ) {
    const courseId = this.requireActiveCourseId();

    return this.http.put<QuizApiDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}`, payload).pipe(
      map((quiz) => this.mapQuiz(quiz)),
      tap((updatedQuiz) => {
        this.quizzesState.update((quizzes) =>
          quizzes.map((quiz) => (quiz.id === quizId ? updatedQuiz : quiz))
        );
        this.quizVersionsState.update((versionsMap) => {
          const nextMap = { ...versionsMap };
          delete nextMap[quizId];
          return nextMap;
        });
      })
    );
  }

  loadQuizVersions(quizId: number, force = false) {
    const courseId = this.requireActiveCourseId();

    if (this.quizVersionsState()[quizId] && !force) {
      return of(this.quizVersionsState()[quizId]);
    }

    this.quizVersionLoadingState.set(quizId);

    return this.http.get<QuizVersionApiDto[]>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}/versions`).pipe(
      map((versions) => versions.map((version) => this.mapQuizVersion(version))),
      tap((versions) => {
        this.quizVersionsState.update((versionsMap) => ({
          ...versionsMap,
          [quizId]: versions
        }));
      }),
      catchError((error) => {
        this.quizVersionsState.update((versionsMap) => ({
          ...versionsMap,
          [quizId]: []
        }));
        throw error;
      }),
      finalize(() => {
        this.quizVersionLoadingState.set(null);
      })
    );
  }

  getQuizVersions(quizId: number): StudioQuizVersion[] {
    return this.quizVersionsState()[quizId] ?? [];
  }

  findQuizById(quizId: number): StudioQuiz | null {
    return this.allQuizzes().find((quiz) => quiz.id === quizId) ?? null;
  }

  archiveQuiz(quizId: number) {
    const courseId = this.requireActiveCourseId();

    return this.http.delete<void>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}`).pipe(
      tap(() => {
        this.quizzesState.update((quizzes) =>
          quizzes.map((quiz) =>
            quiz.id === quizId
              ? {
                  ...quiz,
                  active: false
                }
              : quiz
          )
        );
        this.quizSessionsState.update((sessions) => sessions.filter((session) => session.quizId !== quizId));

        if (this.activeAttemptState()?.sourceQuizId === quizId) {
          this.activeAttemptState.set(null);
        }
      })
    );
  }

  startQuiz(quizId: number): void {
    const quiz = this.quizzesState().find((entry) => entry.id === quizId && entry.active);

    if (!quiz) {
      return;
    }

    this.attemptSubmitErrorState.set(null);
    this.attemptSubmittingState.set(false);
    const courseId = this.requireActiveCourseId();

    this.http.post<QuizSessionApiDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quiz.id}/session`, {}).subscribe({
      next: (session) => {
        this.upsertQuizSession(session);
        this.activeAttemptState.set(this.mapSessionToActiveAttempt(session));
      },
      error: () => {
        this.attemptSubmitErrorState.set('Unable to start or resume this quiz right now. Please try again.');
      }
    });
  }

  startRandomPractice(randomCount: number): void {
    const questionIds = this.pickRandomQuestionIds(this.questionsState().map((question) => question.id), randomCount);

    if (!questionIds.length) {
      return;
    }

    this.activeAttemptState.set({
      sessionId: null,
      sourceQuizId: null,
      quizTitle: `Random practice (${questionIds.length} questions)`,
      questionIds,
      currentIndex: 0,
      answers: {},
      finished: false,
      result: null
    });
    this.attemptSubmitErrorState.set(null);
    this.attemptSubmittingState.set(false);
  }

  selectAnswer(answerId: number): void {
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
          [currentQuestion.id]: answerId
        }
      };
    });
    this.persistActiveQuizSession();
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
    this.persistActiveQuizSession();
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
    this.persistActiveQuizSession();
  }

  goToQuestion(questionIndex: number): void {
    this.activeAttemptState.update((activeAttempt) => {
      if (!activeAttempt || activeAttempt.finished) {
        return activeAttempt;
      }

      const nextIndex = Math.max(0, Math.min(questionIndex, activeAttempt.questionIds.length - 1));

      return {
        ...activeAttempt,
        currentIndex: nextIndex
      };
    });
    this.persistActiveQuizSession();
  }

  finishAttempt(): void {
    const activeAttempt = this.activeAttemptState();

    if (!activeAttempt || activeAttempt.finished || this.attemptSubmittingState()) {
      return;
    }

    const questions = this.activeAttemptQuestions();
    const hasAnsweredEveryQuestion = questions.every((question) => activeAttempt.answers[question.id] !== undefined);

    if (!hasAnsweredEveryQuestion) {
      return;
    }

    if (activeAttempt.sourceQuizId === null) {
      const correctAnswers = questions.reduce((accumulator, question) => {
        const selectedAnswerId = activeAttempt.answers[question.id];
        const correctAnswerId = question.options[question.correctOptionIndex]?.id;
        return accumulator + Number(selectedAnswerId === correctAnswerId);
      }, 0);

      this.completeAttempt({
        id: Date.now(),
        quizId: activeAttempt.sourceQuizId ?? -1,
        quizTitle: activeAttempt.quizTitle,
        correctAnswers,
        totalQuestions: questions.length,
        finishedAt: new Date().toISOString()
      });
      return;
    }

    const courseId = this.requireActiveCourseId();
    const payload = {
      answers: questions.map((question) => ({
        questionId: question.id,
        answerId: activeAttempt.answers[question.id]
      }))
    } satisfies SubmitQuizAttemptPayload;

    this.attemptSubmittingState.set(true);
    this.attemptSubmitErrorState.set(null);

    this.http
      .post<QuizAttemptApiDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${activeAttempt.sourceQuizId}/attempts`, payload)
      .pipe(
        map((attempt) => this.mapAttempt(attempt)),
        finalize(() => {
          this.attemptSubmittingState.set(false);
        })
      )
      .subscribe({
        next: (attempt) => {
          this.completeAttempt(attempt);
        },
        error: () => {
          this.attemptSubmitErrorState.set('Unable to save this quiz result right now. Please try again.');
        }
      });
  }

  clearAttempt(): void {
    this.attemptSubmittingState.set(false);
    this.attemptSubmitErrorState.set(null);
    this.activeAttemptState.set(null);
  }

  hasResumableAttemptForQuiz(quizId: number): boolean {
    return this.quizSessionsState().some((session) => session.quizId === quizId);
  }

  questionUsageCount(questionId: number): number {
    return this.quizzesState().reduce((accumulator, quiz) => {
      if (quiz.active && quiz.mode === 'manual') {
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

  private mapQuiz(quiz: QuizApiDto): QuizDefinition {
    return {
      id: quiz.id,
      courseId: quiz.courseId,
      active: quiz.active,
      currentVersionNumber: quiz.currentVersionNumber,
      createdAt: quiz.createdAt,
      updatedAt: quiz.updatedAt,
      title: quiz.title,
      mode: quiz.mode,
      questionIds: [...quiz.questionIds],
      randomCount: quiz.randomCount,
      questionOrder: quiz.questionOrder,
      answerOrder: quiz.answerOrder,
      categories: [...quiz.categories]
    };
  }

  private mapQuizVersion(version: QuizVersionApiDto): StudioQuizVersion {
    return {
      id: version.id,
      quizId: version.quizId,
      versionNumber: version.versionNumber,
      createdAt: version.createdAt,
      title: version.title,
      mode: version.mode,
      questionIds: [...version.questionIds],
      randomCount: version.randomCount,
      questionOrder: version.questionOrder,
      answerOrder: version.answerOrder,
      categories: [...version.categories]
    };
  }

  private mapAttempt(attempt: QuizAttemptApiDto): AttemptSummary {
    return {
      id: attempt.id,
      quizId: attempt.quizId,
      quizTitle: attempt.quizTitle,
      correctAnswers: attempt.correctAnswers,
      totalQuestions: attempt.totalQuestions,
      finishedAt: attempt.finishedAt
    };
  }

  private mapSessionToActiveAttempt(session: QuizSessionApiDto): ActiveAttempt {
    return {
      sessionId: session.id,
      sourceQuizId: session.quizId,
      quizTitle: session.quizTitle,
      questionIds: [...session.questionIds],
      currentIndex: session.currentIndex,
      answers: { ...session.answers },
      finished: false,
      result: null
    };
  }

  private resolveQuestionIdsForQuiz(quiz: QuizDefinition): number[] {
    if (quiz.mode === 'manual') {
      const manualQuestionIds = quiz.questionIds.filter((questionId) => this.questionsState().some((question) => question.id === questionId));
      return quiz.questionOrder === 'random' ? this.pickRandomQuestionIds(manualQuestionIds, manualQuestionIds.length) : manualQuestionIds;
    }

    if (quiz.mode === 'random') {
      const courseQuestionIds = this.questionsState().map((question) => question.id);
      const selectedQuestionIds = this.pickRandomQuestionIds(courseQuestionIds, quiz.randomCount ?? courseQuestionIds.length);

      return quiz.questionOrder === 'random'
        ? this.pickRandomQuestionIds(selectedQuestionIds, selectedQuestionIds.length)
        : courseQuestionIds.filter((questionId) => selectedQuestionIds.includes(questionId));
    }

    const categoryQuestionIds = this.questionsState()
      .filter((question) => question.categories.some((category) => quiz.categories.some((quizCategory) => quizCategory.id === category.id)))
      .map((question) => question.id);

    return quiz.questionOrder === 'random'
      ? this.pickRandomQuestionIds(categoryQuestionIds, categoryQuestionIds.length)
      : categoryQuestionIds;
  }

  private pickRandomQuestionIds(questionIds: number[], count: number): number[] {
    const shuffled = [...questionIds].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, Math.min(count, questionIds.length));
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

  private completeAttempt(attempt: AttemptSummary): void {
    this.activeAttemptState.update((activeAttempt) => {
      if (!activeAttempt) {
        return activeAttempt;
      }

      return {
        ...activeAttempt,
        finished: true,
        result: {
          correctAnswers: attempt.correctAnswers,
          totalQuestions: attempt.totalQuestions
        }
      };
    });

    this.quizSessionsState.update((sessions) =>
      sessions.filter((session) => session.quizId !== this.activeAttemptState()?.sourceQuizId)
    );
    this.attemptsState.update((attempts) => [attempt, ...attempts]);
  }

  private persistActiveQuizSession(): void {
    const activeAttempt = this.activeAttemptState();

    if (
      !activeAttempt ||
      activeAttempt.finished ||
      activeAttempt.sourceQuizId === null ||
      activeAttempt.sessionId === null
    ) {
      return;
    }

    const courseId = this.requireActiveCourseId();
    const payload = {
      currentIndex: activeAttempt.currentIndex,
      answers: Object.entries(activeAttempt.answers).map(([questionId, answerId]) => ({
        questionId: Number(questionId),
        answerId
      }))
    } satisfies UpdateQuizSessionPayload;

    this.http
      .put<QuizSessionApiDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${activeAttempt.sourceQuizId}/session`, payload)
      .subscribe({
        next: (session) => {
          this.upsertQuizSession(session);
        }
      });
  }

  private upsertQuizSession(session: QuizSessionApiDto): void {
    this.quizSessionsState.update((sessions) => {
      const otherSessions = sessions.filter((entry) => entry.id !== session.id);
      return [session, ...otherSessions];
    });
  }
}
