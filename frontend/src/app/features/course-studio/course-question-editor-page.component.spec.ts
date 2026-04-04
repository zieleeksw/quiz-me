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
          ]
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

  it('should send every checked answer as correct when saving a question', () => {
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
    component.questionForm.patchValue({
      prompt: 'Which annotations can expose or map an HTTP endpoint in Spring MVC?'
    });
    component.answersArray().at(0).controls.content.setValue('@RestController');
    component.answersArray().at(0).controls.correct.setValue(true);
    component.answersArray().at(1).controls.content.setValue('@GetMapping');
    component.answersArray().at(1).controls.correct.setValue(true);

    component.saveQuestion();

    expect(studioMock.createQuestion).toHaveBeenCalledOnceWith({
      prompt: 'Which annotations can expose or map an HTTP endpoint in Spring MVC?',
      answers: [
        { content: '@RestController', correct: true },
        { content: '@GetMapping', correct: true }
      ],
      categoryIds: [1]
    });
  });

  it('should treat changing only the correct answers as a meaningful edit', () => {
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

    component.answersArray().at(1).controls.correct.setValue(true);

    expect(component.hasQuestionChanges()).toBeTrue();
    expect(component.isSaveDisabled()).toBeFalse();
  });

  it('should start with two answers and allow adding more', () => {
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

    expect(component.answersArray().length).toBe(2);

    component.addAnswer();

    expect(component.answersArray().length).toBe(3);
    expect(component.answerLabel(2)).toBe('C');
  });

  it('should not report pending changes for a fresh new question form', () => {
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

    expect(component.hasQuestionChanges()).toBeFalse();
    expect(component.hasPendingChanges()).toBeFalse();
  });

  it('should shift remaining answers up after removing an empty middle answer', () => {
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

    component.addAnswer();
    component.addAnswer();
    component.answersArray().at(0).controls.content.setValue('1');
    component.answersArray().at(1).controls.content.setValue('2');
    component.answersArray().at(2).controls.content.setValue('');
    component.answersArray().at(3).controls.content.setValue('4');

    fixture.detectChanges();
    component.removeAnswer(2);
    fixture.detectChanges();

    const inputs = Array.from(fixture.nativeElement.querySelectorAll('.studio-answer-builder input')) as HTMLInputElement[];
    const labels = Array.from(fixture.nativeElement.querySelectorAll('.studio-answer-builder label') as NodeListOf<HTMLLabelElement>).map((label) =>
      label.textContent?.trim()
    );

    expect(component.answersArray().length).toBe(3);
    expect(component.answersArray().at(2).controls.content.value).toBe('4');
    expect(labels).toEqual(['Option A', 'Option B', 'Option C']);
    expect(inputs.map((input) => input.value)).toEqual(['1', '2', '4']);
  });
});
