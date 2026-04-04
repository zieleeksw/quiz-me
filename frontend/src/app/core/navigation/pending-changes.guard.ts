import { CanDeactivateFn } from '@angular/router';

export interface PendingChangesAware {
  hasPendingChanges(): boolean;
  confirmDiscardChanges?(): boolean;
}

export const pendingChangesGuard: CanDeactivateFn<PendingChangesAware> = (component) => {
  if (!component.hasPendingChanges()) {
    return true;
  }

  return component.confirmDiscardChanges?.() ?? window.confirm(
    'You have unsaved changes. Click Cancel to stay here and save them first, or OK to leave without saving.'
  );
};
