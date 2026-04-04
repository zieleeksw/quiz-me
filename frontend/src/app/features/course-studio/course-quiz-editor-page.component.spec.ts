import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseQuizEditorPageComponent } from './course-quiz-editor-page.component';
import { CourseStudioService } from './course-studio.service';

describe('CourseQuizEditorPageComponent', () => {
  function createStudioServiceMock() {
    type QuizMock = {
      id: string;
      title: string;
      mode: 'manual' | 'random';
      questionIds: number[];
      randomCount: number | null;
      resolvedQuestionCount: number;
    };

    const questions = signal([
      {
        id: 1,
        courseId: 7,
        versionNumber: 1,
        createdAt: '',
        updatedAt: '',
        prompt: 'Question 1',
        categories: [{ id: 1, name: 'HTTP' }],
        options: [
          { id: 101, displayOrder: 0, content: 'A', correct: true },
          { id: 102, displayOrder: 1, content: 'B', correct: false }
        ],
        correctOptionIndex: 0
      },
      {
        id: 2,
        courseId: 7,
        versionNumber: 1,
        createdAt: '',
        updatedAt: '',
        prompt: 'Question 2',
        categories: [{ id: 1, name: 'HTTP' }],
        options: [
          { id: 201, displayOrder: 0, content: 'A', correct: true },
          { id: 202, displayOrder: 1, content: 'B', correct: false }
        ],
        correctOptionIndex: 0
      },
      {
        id: 3,
        courseId: 7,
        versionNumber: 1,
        createdAt: '',
        updatedAt: '',
        prompt: 'Question 3',
        categories: [{ id: 1, name: 'HTTP' }],
        options: [
          { id: 301, displayOrder: 0, content: 'A', correct: true },
          { id: 302, displayOrder: 1, content: 'B', correct: false }
        ],
        correctOptionIndex: 0
      },
      {
        id: 4,
        courseId: 7,
        versionNumber: 1,
        createdAt: '',
        updatedAt: '',
        prompt: 'Question 4',
        categories: [{ id: 1, name: 'HTTP' }],
        options: [
          { id: 401, displayOrder: 0, content: 'A', correct: true },
          { id: 402, displayOrder: 1, content: 'B', correct: false }
        ],
        correctOptionIndex: 0
      },
      {
        id: 5,
        courseId: 7,
        versionNumber: 1,
        createdAt: '',
        updatedAt: '',
        prompt: 'Question 5',
        categories: [{ id: 1, name: 'HTTP' }],
        options: [
          { id: 501, displayOrder: 0, content: 'A', correct: true },
          { id: 502, displayOrder: 1, content: 'B', correct: false }
        ],
        correctOptionIndex: 0
      },
      {
        id: 6,
        courseId: 7,
        versionNumber: 1,
        createdAt: '',
        updatedAt: '',
        prompt: 'Question 6',
        categories: [{ id: 1, name: 'HTTP' }],
        options: [
          { id: 601, displayOrder: 0, content: 'A', correct: true },
          { id: 602, displayOrder: 1, content: 'B', correct: false }
        ],
        correctOptionIndex: 0
      },
      {
        id: 7,
        courseId: 7,
        versionNumber: 1,
        createdAt: '',
        updatedAt: '',
        prompt: 'Question 7',
        categories: [{ id: 1, name: 'HTTP' }],
        options: [
          { id: 701, displayOrder: 0, content: 'A', correct: true },
          { id: 702, displayOrder: 1, content: 'B', correct: false }
        ],
        correctOptionIndex: 0
      }
    ]);
    const quizzes = signal<QuizMock[]>([
      {
        id: 'quiz-1',
        title: 'Manual quiz',
        mode: 'manual' as const,
        questionIds: [1, 2, 3, 4, 5, 6],
        randomCount: null,
        resolvedQuestionCount: 6
      }
    ]);

    return {
      loadCourseContext: jasmine.createSpy('loadCourseContext'),
      findQuizById: jasmine.createSpy('findQuizById').and.callFake((quizId: string) => quizzes().find((quiz) => quiz.id === quizId) ?? null),
      updateQuiz: jasmine.createSpy('updateQuiz').and.callFake((quizId: string, payload: { title: string; mode: 'manual' | 'random'; questionIds: number[]; randomCount: number | null }) => {
        quizzes.update((currentQuizzes) =>
          currentQuizzes.map((quiz) =>
            quiz.id === quizId
              ? {
                  ...quiz,
                  title: payload.title,
                  mode: payload.mode,
                  questionIds: payload.mode === 'manual' ? payload.questionIds : [],
                  randomCount: payload.mode === 'random' ? payload.randomCount : null,
                  resolvedQuestionCount: payload.mode === 'manual' ? payload.questionIds.length : currentQuizzes.length
                }
              : quiz
          )
        );
      }),
      questions,
      categories: signal([{ id: 1, name: 'HTTP', active: true, createdAt: '', updatedAt: '' }]),
      isLoading: signal(false),
      isLoaded: signal(true),
      loadError: signal<string | null>(null),
      questionPreviewItems: signal([]),
      questionPreviewPageNumber: signal(0),
      questionPreviewTotalPages: signal(0),
      questionUsageCount: jasmine.createSpy('questionUsageCount').and.returnValue(1)
    };
  }

  function createCoursesCatalogServiceMock() {
    return {
      loadCourses: jasmine.createSpy('loadCourses'),
      findBySlug: jasmine.createSpy('findBySlug').and.returnValue({
        id: 7,
        slug: '7-spring',
        title: 'Spring Security Associate',
        subtitle: 'Security course',
        createdAt: '',
        ownerUserId: 1,
        questionCount: 7,
        quizCount: 1,
        progressPercent: 0
      })
    };
  }

  it('should treat a saved manual quiz as clean even when the question count changes', () => {
    const studioMock = createStudioServiceMock();
    const catalogMock = createCoursesCatalogServiceMock();

    TestBed.configureTestingModule({
      imports: [CourseQuizEditorPageComponent],
      providers: [
        { provide: CourseStudioService, useValue: studioMock },
        { provide: CoursesCatalogService, useValue: catalogMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ courseSlug: '7-spring', quizId: 'quiz-1' })
            }
          }
        }
      ]
    });

    const fixture = TestBed.createComponent(CourseQuizEditorPageComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    component.toggleQuestionSelection(7);

    expect(component.hasQuizChanges()).toBeTrue();

    component.createQuiz();

    expect(studioMock.updateQuiz).toHaveBeenCalledOnceWith('quiz-1', {
      title: 'Manual quiz',
      mode: 'manual',
      questionIds: [1, 2, 3, 4, 5, 6, 7],
      randomCount: null
    });
    expect(component.hasQuizChanges()).toBeFalse();
  });
});
