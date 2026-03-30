import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CoursesCatalogService } from '../dashboard/courses-catalog.service';
import { CourseStudioService, StudioQuestion } from './course-studio.service';

@Component({
  selector: 'app-course-studio-editor-page',
  imports: [ReactiveFormsModule, RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-studio-editor-page.component.html',
  styleUrl: './course-studio-editor-page.component.scss'
})
export class CourseStudioEditorPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly coursesCatalogService = inject(CoursesCatalogService);
  readonly studio = inject(CourseStudioService);
  readonly courseSlug = this.route.snapshot.paramMap.get('courseSlug') ?? 'spring-boot-associate';
  readonly currentCourse = computed(() => this.coursesCatalogService.findBySlug(this.courseSlug));
  readonly studioLink = ['/courses', this.courseSlug];

  readonly selectedQuestionIds = signal<string[]>([]);
  readonly bankSearch = signal('');
  readonly activeCategoryFilter = signal<string>('All');
  readonly selectedComposerCategories = signal<string[]>(this.studio.availableCategories().slice(0, 1));
  readonly newManagedCategory = signal('');
  readonly categoryMessage = signal('');
  readonly editingCategory = signal<string | null>(null);
  readonly editingCategoryDraft = signal('');

  readonly questionForm = this.formBuilder.nonNullable.group({
    prompt: ['', [Validators.required, Validators.minLength(12)]],
    optionA: ['', [Validators.required]],
    optionB: ['', [Validators.required]],
    optionC: ['', [Validators.required]],
    optionD: ['', [Validators.required]],
    correctOptionIndex: [0, [Validators.required]]
  });

  readonly quizForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(4)]],
    mode: ['manual' as 'manual' | 'random', [Validators.required]],
    randomCount: [10, [Validators.min(1)]]
  });

  readonly filteredQuestions = computed(() => {
    const normalizedSearch = this.bankSearch().trim().toLowerCase();
    const activeCategory = this.activeCategoryFilter();

    return this.studio.questions().filter((question) => {
      const matchesCategory = activeCategory === 'All' || question.categories.includes(activeCategory);
      const matchesSearch =
        !normalizedSearch ||
        question.prompt.toLowerCase().includes(normalizedSearch) ||
        question.categories.some((category) => category.toLowerCase().includes(normalizedSearch));

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
  }

  addQuestion(): void {
    if (this.questionForm.invalid || !this.selectedComposerCategories().length) {
      this.questionForm.markAllAsTouched();
      return;
    }

    const value = this.questionForm.getRawValue();

    this.studio.addQuestion({
      categories: this.selectedComposerCategories(),
      prompt: value.prompt,
      options: [value.optionA, value.optionB, value.optionC, value.optionD],
      correctOptionIndex: value.correctOptionIndex
    });

    this.questionForm.reset({
      prompt: '',
      optionA: '',
      optionB: '',
      optionC: '',
      optionD: '',
      correctOptionIndex: 0
    });
  }

  createQuiz(): void {
    if (this.quizForm.invalid) {
      this.quizForm.markAllAsTouched();
      return;
    }

    const value = this.quizForm.getRawValue();

    if (value.mode === 'manual' && !this.selectedQuestionIds().length) {
      return;
    }

    this.studio.addQuiz({
      title: value.title,
      mode: value.mode,
      questionIds: this.selectedQuestionIds(),
      randomCount: value.mode === 'random' ? value.randomCount : null
    });

    this.quizForm.reset({
      title: '',
      mode: 'manual',
      randomCount: value.randomCount
    });
    this.selectedQuestionIds.set([]);
  }

  toggleQuestionSelection(questionId: string): void {
    this.selectedQuestionIds.update((selectedQuestionIds) =>
      selectedQuestionIds.includes(questionId)
        ? selectedQuestionIds.filter((id) => id !== questionId)
        : [...selectedQuestionIds, questionId]
    );
  }

  isQuestionSelected(questionId: string): boolean {
    return this.selectedQuestionIds().includes(questionId);
  }

  deleteQuestion(questionId: string): void {
    this.studio.deleteQuestion(questionId);
    this.selectedQuestionIds.update((selectedQuestionIds) => selectedQuestionIds.filter((id) => id !== questionId));
  }

  setBankSearch(value: string): void {
    this.bankSearch.set(value);
  }

  setCategoryFilter(category: string): void {
    this.activeCategoryFilter.set(category);
  }

  toggleComposerCategory(category: string): void {
    this.categoryMessage.set('');
    this.selectedComposerCategories.update((categories) =>
      categories.includes(category) ? categories.filter((entry) => entry !== category) : [...categories, category]
    );
  }

  removeComposerCategory(category: string): void {
    this.categoryMessage.set('');
    this.selectedComposerCategories.update((categories) => categories.filter((entry) => entry !== category));
  }

  setNewManagedCategory(value: string): void {
    this.newManagedCategory.set(value);
    this.categoryMessage.set('');
  }

  createCategory(): void {
    const result = this.studio.addCategory(this.newManagedCategory());

    if (!result.ok) {
      this.categoryMessage.set(this.resolveCategoryMutationMessage(result.reason));
      return;
    }

    this.newManagedCategory.set('');
    this.categoryMessage.set(`Category "${result.name}" is ready to use.`);
    this.selectedComposerCategories.update((categories) =>
      categories.includes(result.name) ? categories : [...categories, result.name]
    );
  }

  startCategoryEdit(category: string): void {
    this.editingCategory.set(category);
    this.editingCategoryDraft.set(category);
    this.categoryMessage.set('');
  }

  setEditingCategoryDraft(value: string): void {
    this.editingCategoryDraft.set(value);
    this.categoryMessage.set('');
  }

  saveCategoryEdit(category: string): void {
    const result = this.studio.renameCategory(category, this.editingCategoryDraft());

    if (!result.ok) {
      this.categoryMessage.set(this.resolveCategoryMutationMessage(result.reason));
      return;
    }

    if (this.activeCategoryFilter() === category) {
      this.activeCategoryFilter.set(result.name);
    }

    this.selectedComposerCategories.update((categories) =>
      categories.map((entry) => (entry === category ? result.name : entry))
    );
    this.editingCategory.set(null);
    this.editingCategoryDraft.set('');
    this.categoryMessage.set(`Category renamed to "${result.name}".`);
  }

  cancelCategoryEdit(): void {
    this.editingCategory.set(null);
    this.editingCategoryDraft.set('');
  }

  deleteCategory(category: string): void {
    const result = this.studio.deleteCategory(category);

    if (!result.ok) {
      this.categoryMessage.set('That category is no longer available.');
      return;
    }

    if (this.activeCategoryFilter() === category) {
      this.activeCategoryFilter.set('All');
    }

    this.selectedComposerCategories.update((categories) => categories.filter((entry) => entry !== category));

    if (!this.selectedComposerCategories().length && this.studio.availableCategories().length) {
      this.selectedComposerCategories.set(this.studio.availableCategories().slice(0, 1));
    }

    if (this.editingCategory() === category) {
      this.cancelCategoryEdit();
    }

    if (result.reassignedQuestionCount > 0 && result.fallbackCategoryName) {
      this.categoryMessage.set(
        `Category removed. ${result.reassignedQuestionCount} question${result.reassignedQuestionCount === 1 ? '' : 's'} moved to "${result.fallbackCategoryName}".`
      );
      return;
    }

    this.categoryMessage.set(`Category "${category}" removed.`);
  }

  clearSelectedQuestions(): void {
    this.selectedQuestionIds.set([]);
  }

  removeSelectedQuestion(questionId: string): void {
    this.selectedQuestionIds.update((selectedQuestionIds) => selectedQuestionIds.filter((id) => id !== questionId));
  }

  trackCategory(index: number, category: string): string {
    return `${index}-${category}`;
  }

  private resolveCategoryMutationMessage(reason: 'empty' | 'duplicate' | 'not-found'): string {
    if (reason === 'empty') {
      return 'Category name cannot be empty.';
    }

    if (reason === 'duplicate') {
      return 'That category already exists in this course.';
    }

    return 'That category is no longer available.';
  }
}
