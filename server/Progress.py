class Progress:
    def __init__(self):
        self.total = 0
        self.current = 0
        self.done = False

    def remaining_frames(self) -> int:
        return max(0, self.total - self.current)

    def remaining_percent(self) -> int:
        if self.total <= 0:
            return 100 if not self.done else 0
        return max(0, 100 - int((self.current / self.total) * 100))