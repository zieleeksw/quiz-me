import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseQuestionEditorPageComponent } from './course-question-editor-page.component';
import { CourseStudioService } from './course-studio.service';

describe('CourseQuestionEditorPageComponent', () => {
  function createStudioServiceMock() {
    return {
      loadCourseContext: jasmine.createSpy('loadCourseContext'),
      createQuestion: jasmine.createSpy('createQuestion').and.returnValue(of({ id: 11 })),
      updateQuestion: jasmine.createSpy('updateQuestion').and.returnValue(of({ id: 21 })),
      loadQuestionVersions: jasmine.createSpy('loadQuestionVersions').and.returnValue(of([])),
      getQuestionVersions: jasmine.createSpy('getQuestionVersions').and.returnValue([]),
      categories: signal([
        { id: 1, name: 'HTTP', active: true, createdAt: '', updatedAt: '' },
        { id: 2, name: 'Security', active: true, createdAt: '', updatedAt: '' }
      ]),
      questions: signal([
        {
          id: 21,
          courseId: 7,
          versionNumber: 1,
          createdAt: '',
          updatedAt: '',
          prompt: 'Which annotation is typically used to expose an HTTP endpoint class?',
          categories: [{ id: 1, name: 'HTTP' }],
          options: [
            { id: 101, displayOrder: 0, content: '@RestController', correct: true },
            { id: 102, displayOrder: 1, content: '@Repository', correct: false }
          ],
          correctOptionIndex: 0
        }
      ]),
      versionLoadingQuestionId: signal<number | null>(null),
      isLoading: signal(false),
      isLoaded: signal(true),
      activeCourseId: signal(7),
      loadError: signal<string | null>(null)
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
        questionCount: 1,
        quizCount: 0,
        progressPercent: 0
      })
    };
  }

  it('should send exactly one correct answer when correctOptionIndex is provided as a string', () => {
    const studioMock = createStudioServiceMock();
    const catalogMock = createCoursesCatalogServiceMock();

    TestBed.configureTestingModule({
      imports: [CourseQuestionEditorPageComponent],
      providers: [
        { provide: CourseStudioService, useValue: studioMock },
        { provide: CoursesCatalogService, useValue: catalogMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ courseSlug: '7-spring' })
            }
          }
        }
      ]
    });

    const fixture = TestBed.createComponent(CourseQuestionEditorPageComponent);
    const component = fixture.componentInstance;

    component.selectedComposerCategoryIds.set([1]);
    component.questionForm.setValue({
      prompt: 'Which annotation is typically used to expose an HTTP endpoint class?',
      optionA: '@RestController',
      optionB: '@Repository',
      optionC: '',
      optionD: '',
      correctOptionIndex: '1' as never
    });

    component.saveQuestion();

    expect(studioMock.createQuestion).toHaveBeenCalledOnceWith({
      prompt: 'Which annotation is typically used to expose an HTTP endpoint class?',
      answers: [
        { content: '@RestController', correct: false },
        { content: '@Repository', correct: true }
      ],
      categoryIds: [1]
    });
  });

  it('should treat changing only the correct answer as a meaningful edit', () => {
    const studioMock = createStudioServiceMock();
    const catalogMock = createCoursesCatalogServiceMock();

    TestBed.configureTestingModule({
      imports: [CourseQuestionEditorPageComponent],
      providers: [
        { provide: CourseStudioService, useValue: studioMock },
        { provide: CoursesCatalogService, useValue: catalogMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ courseSlug: '7-spring', questionId: '21' })
            }
          }
        }
      ]
    });

    const fixture = TestBed.createComponent(CourseQuestionEditorPageComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    component.questionForm.patchValue({
      correctOptionIndex: 1
    });

    expect(component.hasQuestionChanges()).toBeTrue();
    expect(component.isSaveDisabled()).toBeFalse();
  });
});
