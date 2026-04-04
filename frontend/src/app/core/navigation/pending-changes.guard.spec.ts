import { pendingChangesGuard } from './pending-changes.guard';

describe('pendingChangesGuard', () => {
  it('should allow navigation when there are no pending changes', () => {
    const result = pendingChangesGuard(
      {
        hasPendingChanges: () => false
      },
      {} as never,
      {} as never,
      {} as never
    );

    expect(result).toBeTrue();
  });

  it('should ask the component whether to discard changes when there are pending changes', () => {
    const result = pendingChangesGuard(
      {
        hasPendingChanges: () => true,
        confirmDiscardChanges: () => false
      },
      {} as never,
      {} as never,
      {} as never
    );

    expect(result).toBeFalse();
  });
});
