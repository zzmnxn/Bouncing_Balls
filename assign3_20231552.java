package practice;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class assign3_20231552 extends Frame implements ActionListener {
    List<Ball> balls = new ArrayList<>();
    private final BounceBackground canvas;

    public assign3_20231552() {
        canvas = new BounceBackground(balls);
        add("Center", canvas);

        Panel controlPanel = new Panel();
        Button start = new Button("Start");
        Button close = new Button("Close");

        controlPanel.add(start);
        controlPanel.add(close);

        start.addActionListener(this);
        close.addActionListener(this);

        add("South", controlPanel);
    }

    public void actionPerformed(ActionEvent evt) {
        String action = evt.getActionCommand();
        if ("Start".equals(action)) {
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(i * 72); // 각 공의 초기 방향 (0, 72, 144, 216, 288도)
                int xSpeed = (int) (Math.cos(angle) * 2); // X 초기속도 2로 고정
                int ySpeed = (int) (Math.sin(angle) * 2); // Y 초기속도 2로 고정

                Ball ball = new Ball(canvas, xSpeed, ySpeed, balls, 16);
                balls.add(ball);
                ball.start();
            }
        } else if ("Close".equals(action)) {
            System.exit(0);
        }
    }


    public static void main(String[] args) {
        Frame f = new assign3_20231552();
        f.setSize(300, 300);
        centerWindow(f);
        f.addWindowListener(new WindowDestroyer());
        f.setVisible(true);
    }
    private static void centerWindow(Window frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // 화면 해상도 가져오기
        Dimension frameSize = frame.getSize();
        int x = (screenSize.width - frameSize.width) / 2; // 화면 중앙의 X 좌표
        int y = (screenSize.height - frameSize.height) / 2; // 화면 중앙의 Y 좌표
        frame.setLocation(x, y);
    }
}

class BounceBackground extends Canvas {
    private final List<Ball> balls;  //생성된 ball들 넣을 리스트

    public BounceBackground(List<Ball> balls) {
        this.balls = balls;
    }

    @Override
    public void paint(Graphics g) {
        synchronized (balls) {
            for (Ball b : balls) {
                b.draw(g);
            }
        }
        //////////////////////////////////////////////////
        // 마지막에 공이 너무 작아 확인할 수 없어, 공의 개수 프린트하여 check
        /*g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("Balls left: " + balls.size(), 10, 20); */
    }

    @Override
    public void update(Graphics g) {
        synchronized (balls) {
            g.clearRect(0, 0, getWidth(), getHeight());
            paint(g);
        }
    }
}

class CollisionHandler {
    public static boolean checkC(Ball b1, Ball b2) {
        int dfx = b1.getX() - b2.getX();
        int dfy = b1.getY() - b2.getY();
        int distanceSquared = dfx * dfx + dfy * dfy;
        int radiusSum = b1.getSize() / 2 + b2.getSize() / 2;
        return distanceSquared <= radiusSum * radiusSum;
    }

    public static void handleC(Ball b1, Ball b2, List<Ball> balls, Canvas box) {
        int newSize = b1.getSize() / 2;
//새로운 공 생성
        Ball[] newBalls = {
                new Ball(box, newSpeed(-b1.getDx()), newSpeed(-b1.getDy()), balls, newSize, b1.getX(), b1.getY()),
                new Ball(box, newSpeed(-b2.getDx()), newSpeed(-b2.getDy()), balls, newSize, b2.getX(), b2.getY())
        }; ////반대 방향으로 가게 함/////
        synchronized (balls) {
            //새로운 공 추가 , 기존 공 제거
            balls.add(newBalls[0]);
            balls.add(newBalls[1]);
            balls.remove(b1);
            balls.remove(b2);
        }

        for (Ball newBall : newBalls) {
            newBall.start();
        }
    }
    // 랜덤 속도 생성
    private static int newSpeed(int dir) {
        Random random = new Random();
        int speed = random.nextInt(5) + 2; // 2부터 4까지 랜덤 값 생성
        //부딪히면 초기속도보다 빠르거나 같게 분열되는 것 표현하고 싶었음
        return dir<0 ? -speed : speed;
    }

}

class Ball extends Thread {
    private final Canvas box;
    private final List<Ball> balls;
    private int x, y;
    private int dx, dy;
    private final int size;
    //x, y 공의 현재 위치 나타냄
    //dx, dy 공의 방향 , 속력
    private double movedDist = 0;
    private static final double MIN_DIST = 25;

    public Ball(Canvas box, int dx, int dy, List<Ball> balls, int size) {
        this.box = box;
        this.balls = balls;
        this.size = size; //공의 크기

        Dimension dim = box.getSize();
        this.x = dim.width / 2 - size / 2;
        this.y = dim.height / 2 - size / 2;
        this.dx = dx;
        this.dy = dy;
    }

    public Ball(Canvas box, int dx, int dy, List<Ball> balls, int size, int x, int y) {
        this(box, dx, dy, balls, size);
        this.x = x;
        this.y = y;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                //위치 업데이트 및 경계 충돌 확인
                updatePos();
                checkBorder();


                if (movedDist > MIN_DIST) {
                    synchronized (balls) {
                        for (Ball other : balls) {
                            if (other != this && CollisionHandler.checkC(this, other)) {
                                CollisionHandler.handleC(this, other, balls, box);
                                return;
                            }
                        }
                    }
                }

                // 공의 크기가 1 이하일 경우 제거
                if (size <= 1) {
                    removeBall();
                    break; // 스레드 종료
                }

                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            // 스레드 종료 처리
        }
    }


    private void updatePos() {
        x += dx;
        y += dy;
        movedDist+= Math.sqrt(dx * dx + dy * dy);
        box.repaint();
    }

    private void removeBall() {
        synchronized (balls) {
            balls.remove(this);
            // 모든 공이 제거되면 프로그램 종료
            if (balls.isEmpty()) {
                System.exit(0);
            }
        }
    }

    private void checkBorder() {
        Dimension dim = box.getSize();
        if (x < 0 || x + size >= dim.width) {
            dx = -dx;
        }
        if (y < 0 || y + size >= dim.height) {
            dy = -dy;
        }
    }

    public void draw(Graphics g) {
        g.setColor(new Color(200, 180, 255)); //  보라색
        g.fillOval(x, y, size, size);

    }

    public int getSize() {
        return size;
    }
    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }


    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
}

// WindowDestroyer
class WindowDestroyer extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }
}
