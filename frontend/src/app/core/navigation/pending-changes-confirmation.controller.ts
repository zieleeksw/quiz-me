export class PendingChangesConfirmationController {
  private static readonly BEFORE_UNLOAD_SUPPRESSION_MS = 1500;

  private skipBeforeUnloadUntil = 0;
  private lastBeforeUnloadAt = 0;
  private discardDecisionForCurrentAttempt: boolean | null = null;

  constructor(private readonly message: string) {}

  confirmDiscardChanges(): boolean {
    if (Date.now() - this.lastBeforeUnloadAt < PendingChangesConfirmationController.BEFORE_UNLOAD_SUPPRESSION_MS) {
      return false;
    }

    if (this.discardDecisionForCurrentAttempt !== null) {
      return this.discardDecisionForCurrentAttempt;
    }

    this.skipBeforeUnloadUntil = Date.now() + PendingChangesConfirmationController.BEFORE_UNLOAD_SUPPRESSION_MS;
    const shouldLeave = window.confirm(this.message);

    this.discardDecisionForCurrentAttempt = shouldLeave;

    return shouldLeave;
  }

  clearAttemptDecision(): void {
    this.discardDecisionForCurrentAttempt = null;
  }

  handleBeforeUnload(event: BeforeUnloadEvent, hasPendingChanges: boolean): void {
    if (Date.now() < this.skipBeforeUnloadUntil || !hasPendingChanges) {
      return;
    }

    this.lastBeforeUnloadAt = Date.now();
    event.preventDefault();
    event.returnValue = true;
  }
}
