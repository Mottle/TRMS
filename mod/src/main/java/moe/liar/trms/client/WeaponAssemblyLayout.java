package moe.liar.trms.client;

/** Pure integer layout calculation for the non-pausing weapon assembly screen. */
record WeaponAssemblyLayout(int cellSize, int originX, int originY, int gridWidth, int gridHeight) {
    static final int GRID_SIZE = 14;
    static final int HANDLE_LENGTH = 10;
    private static final int MAX_CELL_SIZE = 12;
    private static final int HORIZONTAL_PADDING = 16;
    private static final int VERTICAL_PADDING = 48;

    static WeaponAssemblyLayout forScreen(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Screen dimensions must be positive");
        }
        int availableWidth = Math.max(1, width - HORIZONTAL_PADDING);
        int availableHeight = Math.max(1, height - VERTICAL_PADDING);
        int cellSize = Math.max(1, Math.min(MAX_CELL_SIZE,
                Math.min(availableWidth / GRID_SIZE,
                        availableHeight / (GRID_SIZE + HANDLE_LENGTH + 2))));
        int gridWidth = GRID_SIZE * cellSize;
        int gridHeight = (GRID_SIZE + HANDLE_LENGTH + 2) * cellSize;
        return new WeaponAssemblyLayout(cellSize,
                (width - gridWidth) / 2,
                (height - gridHeight) / 2,
                gridWidth,
                gridHeight);
    }

    int cellLeft(int x) {
        return originX + (x - 1) * cellSize;
    }

    int cellTop(int z) {
        return originY + (z - 1) * cellSize;
    }

    int contentBottom() {
        return originY + gridHeight;
    }
}
