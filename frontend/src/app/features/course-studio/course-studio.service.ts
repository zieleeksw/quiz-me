import { Injectable, computed, signal } from '@angular/core';

type Question = {
  id: string;
  categories: string[];
  prompt: string;
  options: string[];
  correctOptionIndex: number;
};

type Category = {
  id: string;
  name: string;
};

type QuizDefinition = {
  id: string;
  title: string;
  mode: 'manual' | 'random';
  questionIds: string[];
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
  questionIds: string[];
  currentIndex: number;
  answers: Record<string, number>;
  finished: boolean;
  result: {
    correctAnswers: number;
    totalQuestions: number;
  } | null;
};

type CategoryMutationResult =
  | { ok: true; name: string }
  | { ok: false; reason: 'empty' | 'duplicate' | 'not-found' };

type CategoryDeleteResult =
  | {
      ok: true;
      reassignedQuestionCount: number;
      fallbackCategoryName: string | null;
    }
  | { ok: false; reason: 'not-found' };

export type StudioQuestion = Question;
export type StudioQuiz = QuizDefinition;

@Injectable({ providedIn: 'root' })
export class CourseStudioService {
  private readonly courseTitleState = signal('Spring Boot Associate Mastery Path');
  private readonly courseSubtitleState = signal(
    'One course can hold the full question bank, multiple targeted quizzes, and a random practice mode with shared aggregated stats.'
  );

  private readonly questionsState = signal<Question[]>([
    {
      id: this.createId('question'),
      categories: ['Core', 'Bootstrapping'],
      prompt: 'Which annotation marks the main Spring Boot entry point class?',
      options: ['@SpringBootApplication', '@EnableAutoConfiguration', '@BootApplication', '@ConfigurationProperties'],
      correctOptionIndex: 0
    },
    {
      id: this.createId('question'),
      categories: ['Web', 'Controllers'],
      prompt: 'Which stereotype should be used for a REST endpoint class?',
      options: ['@Service', '@Repository', '@RestController', '@ComponentScan'],
      correctOptionIndex: 2
    },
    {
      id: this.createId('question'),
      categories: ['Security', 'HTTP'],
      prompt: 'Which header usually carries a Bearer token in an HTTP request?',
      options: ['Content-Type', 'Authorization', 'Accept', 'X-Session-Id'],
      correctOptionIndex: 1
    },
    {
      id: this.createId('question'),
      categories: ['Data', 'Persistence'],
      prompt: 'Which dependency is typically used for Spring Data JPA support?',
      options: [
        'spring-boot-starter-validation',
        'spring-boot-starter-security',
        'spring-boot-starter-data-jpa',
        'spring-boot-starter-actuator'
      ],
      correctOptionIndex: 2
    }
  ]);

  private readonly categoriesState = signal<Category[]>(this.createInitialCategories(this.questionsState()));

  private readonly quizzesState = signal<QuizDefinition[]>([
    {
      id: this.createId('quiz'),
      title: 'Spring Boot Foundations',
      mode: 'manual',
      questionIds: [],
      randomCount: null
    },
    {
      id: this.createId('quiz'),
      title: 'Random Checkpoint',
      mode: 'random',
      questionIds: [],
      randomCount: 3
    }
  ]);

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
  readonly questions = this.questionsState.asReadonly();
  readonly availableCategories = computed(() =>
    this.categoriesState()
      .map((category) => category.name)
      .sort((left, right) => left.localeCompare(right))
  );
  readonly categorySummaries = computed(() =>
    this.availableCategories().map((categoryName) => ({
      name: categoryName,
      questionCount: this.questionsState().filter((question) => question.categories.includes(categoryName)).length
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

  constructor() {
    const currentQuestions = this.questionsState();

    this.quizzesState.update((quizzes) =>
      quizzes.map((quiz, index) => {
        if (index === 0) {
          return {
            ...quiz,
            questionIds: currentQuestions.slice(0, 3).map((question) => question.id)
          };
        }

        return quiz;
      })
    );
  }

  addQuestion(payload: { categories: string[]; prompt: string; options: string[]; correctOptionIndex: number }): void {
    this.ensureManagedCategories(payload.categories);

    const newQuestion: Question = {
      id: this.createId('question'),
      categories: payload.categories,
      prompt: payload.prompt,
      options: payload.options,
      correctOptionIndex: payload.correctOptionIndex
    };

    this.questionsState.update((questions) => [newQuestion, ...questions]);
  }

  addCategory(name: string): CategoryMutationResult {
    const trimmedName = name.trim();

    if (!trimmedName) {
      return { ok: false, reason: 'empty' };
    }

    if (this.hasCategory(trimmedName)) {
      return { ok: false, reason: 'duplicate' };
    }

    this.categoriesState.update((categories) => [...categories, { id: this.createId('category'), name: trimmedName }]);
    return { ok: true, name: trimmedName };
  }

  renameCategory(currentName: string, nextName: string): CategoryMutationResult {
    const trimmedNextName = nextName.trim();
    const resolvedCurrentName = this.resolveManagedCategoryName(currentName);

    if (!resolvedCurrentName) {
      return { ok: false, reason: 'not-found' };
    }

    if (!trimmedNextName) {
      return { ok: false, reason: 'empty' };
    }

    if (
      !this.isSameCategoryName(resolvedCurrentName, trimmedNextName) &&
      this.hasCategory(trimmedNextName)
    ) {
      return { ok: false, reason: 'duplicate' };
    }

    this.categoriesState.update((categories) =>
      categories.map((category) =>
        this.isSameCategoryName(category.name, resolvedCurrentName) ? { ...category, name: trimmedNextName } : category
      )
    );
    this.questionsState.update((questions) =>
      questions.map((question) => ({
        ...question,
        categories: question.categories.map((category) =>
          this.isSameCategoryName(category, resolvedCurrentName) ? trimmedNextName : category
        )
      }))
    );

    return { ok: true, name: trimmedNextName };
  }

  deleteCategory(categoryName: string): CategoryDeleteResult {
    const resolvedCategoryName = this.resolveManagedCategoryName(categoryName);

    if (!resolvedCategoryName) {
      return { ok: false, reason: 'not-found' };
    }

    let reassignedQuestionCount = 0;
    let fallbackCategoryName: string | null = null;

    this.categoriesState.update((categories) =>
      categories.filter((category) => !this.isSameCategoryName(category.name, resolvedCategoryName))
    );
    this.questionsState.update((questions) =>
      questions.map((question) => {
        const remainingCategories = question.categories.filter(
          (category) => !this.isSameCategoryName(category, resolvedCategoryName)
        );

        if (remainingCategories.length === question.categories.length) {
          return question;
        }

        if (remainingCategories.length) {
          return {
            ...question,
            categories: remainingCategories
          };
        }

        const fallbackName = this.ensureFallbackCategory();
        fallbackCategoryName = fallbackName;
        reassignedQuestionCount += 1;

        return {
          ...question,
          categories: [fallbackName]
        };
      })
    );

    return {
      ok: true,
      reassignedQuestionCount,
      fallbackCategoryName
    };
  }

  deleteQuestion(questionId: string): void {
    this.questionsState.update((questions) => questions.filter((question) => question.id !== questionId));
    this.quizzesState.update((quizzes) =>
      quizzes.map((quiz) => ({
        ...quiz,
        questionIds: quiz.questionIds.filter((id) => id !== questionId)
      }))
    );

    if (this.activeAttemptState()?.questionIds.includes(questionId)) {
      this.activeAttemptState.set(null);
    }
  }

  addQuiz(payload: { title: string; mode: 'manual' | 'random'; questionIds: string[]; randomCount: number | null }): void {
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

  questionUsageCount(questionId: string): number {
    return this.quizzesState().reduce((accumulator, quiz) => {
      if (quiz.mode === 'manual') {
        return accumulator + Number(quiz.questionIds.includes(questionId));
      }

      return accumulator;
    }, 0);
  }

  private resolveQuestionIdsForQuiz(quiz: QuizDefinition): string[] {
    if (quiz.mode === 'manual') {
      return quiz.questionIds.filter((questionId) => this.questionsState().some((question) => question.id === questionId));
    }

    const questionIds = this.questionsState().map((question) => question.id);
    return this.pickRandomQuestionIds(questionIds, quiz.randomCount ?? questionIds.length);
  }

  private pickRandomQuestionIds(questionIds: string[], count: number): string[] {
    const shuffled = [...questionIds].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, Math.min(count, questionIds.length));
  }

  private createId(prefix: string): string {
    return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
  }

  private createInitialCategories(questions: Question[]): Category[] {
    return [...new Set(questions.flatMap((question) => question.categories))]
      .sort((left, right) => left.localeCompare(right))
      .map((categoryName) => ({
        id: this.createId('category'),
        name: categoryName
      }));
  }

  private ensureManagedCategories(categoryNames: string[]): void {
    const missingCategoryNames = categoryNames.filter((categoryName) => !this.hasCategory(categoryName));

    if (!missingCategoryNames.length) {
      return;
    }

    this.categoriesState.update((categories) => [
      ...categories,
      ...missingCategoryNames.map((categoryName) => ({
        id: this.createId('category'),
        name: categoryName.trim()
      }))
    ]);
  }

  private ensureFallbackCategory(): string {
    const fallbackName = 'Uncategorized';

    if (!this.hasCategory(fallbackName)) {
      this.categoriesState.update((categories) => [...categories, { id: this.createId('category'), name: fallbackName }]);
    }

    return fallbackName;
  }

  private resolveManagedCategoryName(candidate: string): string | null {
    const managedCategory = this.categoriesState().find((category) => this.isSameCategoryName(category.name, candidate));
    return managedCategory?.name ?? null;
  }

  private hasCategory(candidate: string): boolean {
    return this.categoriesState().some((category) => this.isSameCategoryName(category.name, candidate));
  }

  private isSameCategoryName(left: string, right: string): boolean {
    return left.trim().toLowerCase() === right.trim().toLowerCase();
  }
}
