package tool.xfy9326.floatpicture.Services;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FloatingControlDockingTest {
    @Test
    public void nearestEdgeIncludesTopAndBottom() {
        assertEquals(FloatingControlDocking.EDGE_TOP,
                FloatingControlDocking.nearestEdge(400, 8, 44, 44, 1080, 1920, -1));
        assertEquals(FloatingControlDocking.EDGE_BOTTOM,
                FloatingControlDocking.nearestEdge(400, 1860, 44, 44, 1080, 1920, -1));
    }

    @Test
    public void exactCornerKeepsCurrentEdge() {
        assertEquals(FloatingControlDocking.EDGE_TOP,
                FloatingControlDocking.nearestEdge(0, 0, 44, 44, 1080, 1920,
                        FloatingControlDocking.EDGE_TOP));
    }

    @Test
    public void rotatingToNinetyMovesRightEdgeToTopWithoutChangingPhysicalPlace() {
        assertEquals(FloatingControlDocking.EDGE_TOP,
                FloatingControlDocking.rotateEdge(
                        FloatingControlDocking.EDGE_RIGHT, SurfaceRotation.ZERO, SurfaceRotation.NINETY));
        assertArrayEquals(new int[]{1000, 0},
                FloatingControlDocking.rotatePosition(
                        1036, 1000, 44, 44, 1080, 1920,
                        SurfaceRotation.ZERO, SurfaceRotation.NINETY));
    }

    @Test
    public void rotatingBackRestoresTheOriginalRightEdgePosition() {
        assertEquals(FloatingControlDocking.EDGE_RIGHT,
                FloatingControlDocking.rotateEdge(
                        FloatingControlDocking.EDGE_TOP, SurfaceRotation.NINETY, SurfaceRotation.ZERO));
        assertArrayEquals(new int[]{1036, 1000},
                FloatingControlDocking.rotatePosition(
                        1000, 0, 44, 44, 1920, 1080,
                        SurfaceRotation.NINETY, SurfaceRotation.ZERO));
    }

    @Test
    public void halfTurnMovesRightEdgeToLeft() {
        assertEquals(FloatingControlDocking.EDGE_LEFT,
                FloatingControlDocking.rotateEdge(
                        FloatingControlDocking.EDGE_RIGHT, SurfaceRotation.ZERO, SurfaceRotation.ONE_EIGHTY));
        assertArrayEquals(new int[]{0, 876},
                FloatingControlDocking.rotatePosition(
                        1036, 1000, 44, 44, 1080, 1920,
                        SurfaceRotation.ZERO, SurfaceRotation.ONE_EIGHTY));
    }

    @Test
    public void rotationAccountsForStatusBarAndLandscapeNavigationBar() {
        // Portrait frame begins below a 72px status bar. In landscape a 90px
        // navigation bar is on the left and a 48px status bar is at the top.
        assertArrayEquals(new int[]{282, -48},
                FloatingControlDocking.rotatePositionInFrame(
                        948, 300, 132, 132, 1080, 2400, 0, 1,
                        0, 72, 90, 48));
        assertArrayEquals(new int[]{948, 300},
                FloatingControlDocking.rotatePositionInFrame(
                        282, -48, 132, 132, 2400, 1080, 1, 0,
                        90, 48, 0, 72));
    }

    @Test
    public void allRotationPairsReturnToTheSamePositionAndEdge() {
        for (int from = 0; from < 4; from++) {
            int displayWidth = from % 2 == 0 ? 1080 : 2400;
            int displayHeight = from % 2 == 0 ? 2400 : 1080;
            for (int to = 0; to < 4; to++) {
                int[] rotated = FloatingControlDocking.rotatePositionInFrame(
                        250, 400, 132, 132, displayWidth, displayHeight, from, to,
                        0, 72, 90, 48);
                boolean swapped = (from + to) % 2 != 0;
                assertArrayEquals(new int[]{250, 400},
                        FloatingControlDocking.rotatePositionInFrame(
                                rotated[0], rotated[1], 132, 132,
                                swapped ? displayHeight : displayWidth,
                                swapped ? displayWidth : displayHeight, to, from,
                                90, 48, 0, 72));
                for (int edge = 0; edge < 4; edge++) {
                    assertEquals(edge, FloatingControlDocking.rotateEdge(
                            FloatingControlDocking.rotateEdge(edge, from, to), to, from));
                }
            }
        }
    }

    @Test
    public void clippingAtSystemBarDoesNotOverwritePhysicalAnchor() {
        FloatingControlDocking.Anchor anchor = new FloatingControlDocking.Anchor(
                0, 0, 132, 1080, 2400, 0, FloatingControlDocking.EDGE_TOP, 0, 72);
        for (int repeat = 0; repeat < 5; repeat++) {
            int[] rotated = anchor.position(1, 90, 48);
            // The displayed position can be clipped to the new safe frame. The
            // next rotation must still use the user's original physical anchor.
            rotated[0] = Math.max(0, rotated[0]);
            rotated[1] = Math.max(0, rotated[1]);
            assertArrayEquals(new int[]{0, 0}, anchor.position(0, 0, 72));
            assertEquals(FloatingControlDocking.EDGE_TOP, anchor.edge(0));
        }
    }

    private static final class SurfaceRotation {
        private static final int ZERO = 0;
        private static final int NINETY = 1;
        private static final int ONE_EIGHTY = 2;
    }
}
