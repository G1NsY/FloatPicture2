package tool.xfy9326.floatpicture.Services;

/** Pure positioning helpers for the floating controller's four-edge docking. */
final class FloatingControlDocking {
    static final int EDGE_LEFT = 0;
    static final int EDGE_TOP = 1;
    static final int EDGE_RIGHT = 2;
    static final int EDGE_BOTTOM = 3;

    private FloatingControlDocking() {
    }

    /** User-selected physical anchor, retained when a system bar temporarily clips it. */
    static final class Anchor {
        private final int x, y, size, displayWidth, displayHeight, rotation, edge;

        Anchor(int x, int y, int size, int displayWidth, int displayHeight,
               int rotation, int edge, int frameLeft, int frameTop) {
            this.x = x + frameLeft;
            this.y = y + frameTop;
            this.size = size;
            this.displayWidth = displayWidth;
            this.displayHeight = displayHeight;
            this.rotation = rotation;
            this.edge = edge;
        }

        int[] position(int newRotation, int frameLeft, int frameTop) {
            return rotatePositionInFrame(x, y, size, size, displayWidth, displayHeight,
                    rotation, newRotation, 0, 0, frameLeft, frameTop);
        }

        int edge(int newRotation) {
            return rotateEdge(edge, rotation, newRotation);
        }
    }

    static boolean isValidEdge(int edge) {
        return edge >= EDGE_LEFT && edge <= EDGE_BOTTOM;
    }

    static int nearestEdge(
            int x,
            int y,
            int width,
            int height,
            int displayWidth,
            int displayHeight,
            int currentEdge) {
        int[] distances = {
                Math.max(0, x),
                Math.max(0, y),
                Math.max(0, displayWidth - x - width),
                Math.max(0, displayHeight - y - height)
        };
        int nearest = EDGE_LEFT;
        for (int edge = EDGE_TOP; edge <= EDGE_BOTTOM; edge++) {
            if (distances[edge] < distances[nearest]) {
                nearest = edge;
            }
        }
        // Keeping the current edge on an exact corner tie prevents visual flicker.
        if (isValidEdge(currentEdge) && distances[currentEdge] == distances[nearest]) {
            return currentEdge;
        }
        return nearest;
    }

    static int rotateEdge(int edge, int oldRotation, int newRotation) {
        if (!isValidEdge(edge)) {
            return EDGE_LEFT;
        }
        // Android's display rotation describes how the coordinate space turns from
        // the natural orientation. To keep a window at the same physical place,
        // its coordinates must be transformed in the opposite direction.
        int quarterTurns = (oldRotation - newRotation + 4) % 4;
        return (edge + quarterTurns) % 4;
    }

    /** Convert window-frame coordinates through physical display coordinates. */
    static int[] rotatePositionInFrame(
            int x, int y, int width, int height,
            int oldDisplayWidth, int oldDisplayHeight, int oldRotation, int newRotation,
            int oldFrameLeft, int oldFrameTop, int newFrameLeft, int newFrameTop) {
        int[] position = rotatePosition(x + oldFrameLeft, y + oldFrameTop, width, height,
                oldDisplayWidth, oldDisplayHeight, oldRotation, newRotation);
        position[0] -= newFrameLeft;
        position[1] -= newFrameTop;
        return position;
    }

    /**
     * Rotates the center of a window with the display so it stays by the same physical edge.
     * The returned coordinates are the window's new top-left position.
     */
    static int[] rotatePosition(
            int x,
            int y,
            int width,
            int height,
            int oldDisplayWidth,
            int oldDisplayHeight,
            int oldRotation,
            int newRotation) {
        int quarterTurns = (oldRotation - newRotation + 4) % 4;
        int centerX2 = x * 2 + width;
        int centerY2 = y * 2 + height;
        int rotatedCenterX2;
        int rotatedCenterY2;
        switch (quarterTurns) {
            case 1 -> {
                rotatedCenterX2 = oldDisplayHeight * 2 - centerY2;
                rotatedCenterY2 = centerX2;
            }
            case 2 -> {
                rotatedCenterX2 = oldDisplayWidth * 2 - centerX2;
                rotatedCenterY2 = oldDisplayHeight * 2 - centerY2;
            }
            case 3 -> {
                rotatedCenterX2 = centerY2;
                rotatedCenterY2 = oldDisplayWidth * 2 - centerX2;
            }
            default -> {
                rotatedCenterX2 = centerX2;
                rotatedCenterY2 = centerY2;
            }
        }
        return new int[]{
                Math.round((rotatedCenterX2 - width) / 2f),
                Math.round((rotatedCenterY2 - height) / 2f)
        };
    }
}
