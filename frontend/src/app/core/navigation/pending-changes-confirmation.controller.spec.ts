import { PendingChangesConfirmationController } from './pending-changes-confirmation.controller';

describe('PendingChangesConfirmationController', () => {
  it('should reuse the same discard decision until the next user interaction', () => {
    const controller = new PendingChangesConfirmationController('Unsaved changes');
    const confirmSpy = spyOn(window, 'confirm').and.returnValues(false, true);

    expect(controller.confirmDiscardChanges()).toBeFalse();
    expect(controller.confirmDiscardChanges()).toBeFalse();
    expect(confirmSpy).toHaveBeenCalledTimes(1);

    controller.clearAttemptDecision();

    expect(controller.confirmDiscardChanges()).toBeTrue();
    expect(confirmSpy).toHaveBeenCalledTimes(2);
  });
});
