import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';
import { CourseStudioService, StudioQuestion } from './course-studio.service';

@Component({
  selector: 'app-course-studio-editor-page',
  imports: [ReactiveFormsModule, RouterLink, ActionButtonComponent, WorkspaceTopbarComponent],
  templateUrl: './course-studio-editor-page.component.html',
  styleUrl: './course-studio-editor-page.component.scss'
})
export class CourseStudioEditorPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  readonly studio = inject(CourseStudioService);

  readonly selectedQuestionIds = signal<string[]>([]);
  readonly bankSearch = signal('');
  readonly activeCategoryFilter = signal<string>('All');
  readonly selectedComposerCategories = signal<string[]>(['Core']);
  readonly newComposerCategory = signal('');

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
    this.newComposerCategory.set('');
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
    this.selectedComposerCategories.update((categories) =>
      categories.includes(category) ? categories.filter((entry) => entry !== category) : [...categories, category]
    );
  }

  addNewComposerCategory(): void {
    const trimmed = this.newComposerCategory().trim();

    if (!trimmed) {
      return;
    }

    const alreadySelected = this.selectedComposerCategories().some(
      (category) => category.toLowerCase() === trimmed.toLowerCase()
    );

    if (!alreadySelected) {
      this.selectedComposerCategories.update((categories) => [...categories, trimmed]);
    }

    this.newComposerCategory.set('');
  }

  removeComposerCategory(category: string): void {
    this.selectedComposerCategories.update((categories) => categories.filter((entry) => entry !== category));
  }

  setNewComposerCategory(value: string): void {
    this.newComposerCategory.set(value);
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
}
