package bsu.rfe.java.group6.lab5.KurganMaria;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JPanel;

public class GraphicsDisplay extends JPanel {

    private ArrayList<Double[]> graphicsData;
    private ArrayList<Double[]> originalData;
    private int selectedMarker = -1;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private double scaleX;
    private double scaleY;

    private double[][] viewport = new double[2][2];
    private ArrayList<double[][]> undoHistory = new ArrayList(5);
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean clockRotate = false;
    private boolean antiClockRotate = false;

    private Font axisFont;
    private Font labelsFont;

    private BasicStroke axisStroke;
    //private BasicStroke graphicsStroke;
    private BasicStroke markerStroke;
    private BasicStroke gridStroke;
    private BasicStroke selectionStroke;
    private static DecimalFormat formatter=(DecimalFormat)NumberFormat.getInstance();

    private boolean scaleMode = false;
    private boolean changeMode = false;
    private double[] originalPoint = new double[2];
    private Rectangle2D.Double selectionRect = new Rectangle2D.Double();

    public GraphicsDisplay ()	{
        setBackground(Color.WHITE);
		/*graphicsStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f,
				new float [] {4,1,1,1,2,1,1,1,4}, 0.0f);*/
        axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);
        markerStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 5.0f, null, 0.0f);
        selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[] { 10, 10 }, 0.0F);
        gridStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 5.0f, new float [] {5,5}, 2.0f);
        axisFont = new Font("Serif", Font.BOLD, 36);
        labelsFont = new java.awt.Font("Serif",0,10);
        addMouseMotionListener(new MouseMotionHandler());
        addMouseListener(new MouseHandler());
    }

    public void showGraphics(ArrayList<Double[]> graphicsData)	{
        this.graphicsData = graphicsData;


        this.originalData = new ArrayList(graphicsData.size());
        for (Double[] point : graphicsData) {
            Double[] newPoint = new Double[2];
            newPoint[0] = new Double(point[0].doubleValue());
            newPoint[1] = new Double(point[1].doubleValue());
            this.originalData.add(newPoint);
        }
        this.minX = ((Double[])graphicsData.get(0))[0].doubleValue();
        this.maxX = ((Double[])graphicsData.get(graphicsData.size() - 1))[0].doubleValue();
        this.minY = ((Double[])graphicsData.get(0))[1].doubleValue();
        this.maxY = this.minY;

        for (int i = 1; i < graphicsData.size(); i++) {
            if (((Double[])graphicsData.get(i))[1].doubleValue() < this.minY) {
                this.minY = ((Double[])graphicsData.get(i))[1].doubleValue();
            }
            if (((Double[])graphicsData.get(i))[1].doubleValue() > this.maxY) {
                this.maxY = ((Double[])graphicsData.get(i))[1].doubleValue();
            }
        }

        zoomToRegion(minX, maxY, maxX, minY);

    }

    public void zoomToRegion(double x1,double y1,double x2,double y2)	{
        this.viewport[0][0]=x1;
        this.viewport[0][1]=y1;
        this.viewport[1][0]=x2;
        this.viewport[1][1]=y2;
        this.repaint();
    }
    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    protected Point2D.Double xyToPoint(double x, double y) {
        double deltaX = x - viewport[0][0];
        double deltaY = viewport[0][1] - y;
        return new Point2D.Double(deltaX*scaleX, deltaY*scaleY);
    }

    protected double[] translatePointToXY(int x, int y) {
        return new double[] {
                this.viewport[0][0] + x / this.scaleX, this.viewport[0][1] - y / this.scaleY
        };
    }

    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
        Point2D.Double dest = new Point2D.Double();
        dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
        return dest;
    }

    protected void paintGraphics (Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        // Линии
        Double currentX = null;
        Double currentY = null;
        for (Double[] point : this.graphicsData) {
            if ((currentX != null) && (currentY != null)) {
                canvas.draw(new Line2D.Double(xyToPoint(currentX.doubleValue() + 0.1, currentY.doubleValue() + 0.1),
                        xyToPoint(point[0].doubleValue() + 0.1, point[1].doubleValue() + 0.1)));
            }
            currentX = point[0];
            currentY = point[1];

        }
    }

    protected void paintAxis(Graphics2D canvas) {
        // Оси
        canvas.setStroke(axisStroke);

        canvas.setColor(Color.BLACK);

        canvas.setPaint(Color.BLACK);

        canvas.setFont(axisFont);

        FontRenderContext context = canvas.getFontRenderContext();

        if (minX <= 0.0 && maxX >= 0.0) {
            canvas.draw(new Line2D.Double(xyToPoint(0.1, maxY-0.1), xyToPoint(0.1, minY-0.1)));

            GeneralPath arrow = new GeneralPath();

            Point2D.Double lineEnd = xyToPoint(0.1, maxY-0.1);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());

            arrow.lineTo(arrow.getCurrentPoint().getX() + 5, arrow.getCurrentPoint().getY() + 20);

            arrow.lineTo(arrow.getCurrentPoint().getX() - 10, arrow.getCurrentPoint().getY());


            arrow.closePath();
            canvas.draw(arrow);
            canvas.fill(arrow);

            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0.1, maxY-0.1);

            canvas.drawString("y", (float) labelPos.getX() + 10, (float) (labelPos.getY() - bounds.getY()));

        }

        canvas.draw(new Line2D.Double(xyToPoint(0, minY+0.1), xyToPoint(maxX, minY+0.1)));

        GeneralPath arrow = new GeneralPath();

        Point2D.Double lineEnd = xyToPoint(maxX, minY+0.1);
        arrow.moveTo(lineEnd.getX(), lineEnd.getY());

        arrow.lineTo(arrow.getCurrentPoint().getX() - 20, arrow.getCurrentPoint().getY() - 5);

        arrow.lineTo(arrow.getCurrentPoint().getX(), arrow.getCurrentPoint().getY() + 10);


        arrow.closePath();
        canvas.draw(arrow);
        canvas.fill(arrow);

        Rectangle2D bounds = axisFont.getStringBounds("x", context);
        Point2D.Double labelPos = xyToPoint(maxX, minY+0.1);

        canvas.drawString("x", (float) (labelPos.getX() - bounds.getWidth() - 10), (float) (labelPos.getY() + bounds.getY()));

    }

    protected void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        canvas.setPaint(Color.RED);
        GeneralPath lastMarker = null;
        int i = -1;
        for (Double[] point : graphicsData) {
            i++;

            if(isSpecialPoint(point[1]) == true)
                canvas.setColor(Color.GREEN);
            else
                canvas.setColor(Color.RED);

            // Маркеры
            GeneralPath star = new GeneralPath();
            Point2D.Double center = xyToPoint(point[0] + 0.1, point[1] + 0.1);
            star.moveTo(center.getX(), center.getY());
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY()-5);
            star.moveTo(star.getCurrentPoint().getX() - 3, star.getCurrentPoint().getY());
            star.lineTo(star.getCurrentPoint().getX() + 6, star.getCurrentPoint().getY());
            star.moveTo(center.getX(), center.getY());
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY()+5);
            star.moveTo(star.getCurrentPoint().getX() - 3, star.getCurrentPoint().getY());
            star.lineTo(star.getCurrentPoint().getX() + 6, star.getCurrentPoint().getY());
            star.moveTo(center.getX(), center.getY());
            star.lineTo(star.getCurrentPoint().getX() - 5, star.getCurrentPoint().getY());
            star.moveTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY()-3);
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY()+6);
            star.moveTo(center.getX(), center.getY());
            star.lineTo(star.getCurrentPoint().getX() + 5, star.getCurrentPoint().getY());
            star.moveTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY()-3);
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY()+6);
            if (i == this.selectedMarker) {
                lastMarker = star;
            }
            else {
                canvas.draw(star);
                canvas.fill(star);
            }
        }

        if (lastMarker != null) {
            canvas.setColor(Color.BLUE);
            canvas.setPaint(Color.BLUE);
            canvas.draw(lastMarker);
            canvas.fill(lastMarker);
        }
    }

    protected boolean isSpecialPoint(double y) {
        //раскраска маркеров по условию
        int Yint = (int)y;
        boolean flag = false;

        for(int i = 0; i < Yint; i++) {
            if (Yint%2 == 0) {
                if (Yint >= 10)
                    Yint /= 10;
                else {
                    flag = true;
                    break;}
            }
            else {
                flag = false;
                break;
            }
        }

        return flag;
    }



    private void paintLabels(Graphics2D canvas) {
        // Подписи координат и сетки
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.labelsFont);
        FontRenderContext context=canvas.getFontRenderContext();
        double labelYPos;
        double labelXPos;
        if (!(viewport[1][1] >= 0 || viewport[0][1] <= 0))
            labelYPos = 0;
        else labelYPos = viewport[1][1];
        if (!(viewport[0][0] >= 0 || viewport[1][0] <= 0.0D))
            labelXPos=0;
        else labelXPos = viewport[0][0];
        double pos = viewport[0][0];
        double step = (viewport[1][0] - viewport[0][0]) / 10;
        if (selectedMarker >= 0) {
            Point2D.Double point = xyToPoint(((Double[])graphicsData.get(selectedMarker))[0].doubleValue()+ 0.1,
                    ((Double[])graphicsData.get(selectedMarker))[1].doubleValue() + 0.1);
            String label = "X=" + formatter.format(((Double[])graphicsData.get(selectedMarker))[0]) +
                    ", Y=" + formatter.format(((Double[])graphicsData.get(selectedMarker))[1]);
            Rectangle2D bounds = labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLACK);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        scaleX=this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
        scaleY=this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
        if ((this.graphicsData == null) || (this.graphicsData.size() == 0)) return;


        Graphics2D canvas = (Graphics2D) g;
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Font oldFont = canvas.getFont();
        Paint oldPaint = canvas.getPaint();

        if (showAxis) {
            paintAxis(canvas);
            paintLabels(canvas);
        }
        paintGraphics(canvas);
        if (showMarkers) paintMarkers(canvas);

        paintSelection(canvas);
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);

    }

    private void paintSelection(Graphics2D canvas) {
        if (!scaleMode) return;
        canvas.setStroke(selectionStroke);
        canvas.setColor(Color.BLACK);
        canvas.draw(selectionRect);
    }




    // Сбрасываем изменения
    public void reset() {
        showGraphics(this.originalData);
    }

    //Приближаем
    protected int findSelectedPoint(int x, int y)
    {
        if (graphicsData == null) return -1;
        int pos = 0;
        for (Double[] point : graphicsData) {
            Point2D.Double screenPoint = xyToPoint(point[0].doubleValue()+0.1, point[1].doubleValue()+0.1);
            double distance = (screenPoint.getX() - x) * (screenPoint.getX() - x) + (screenPoint.getY() - y) * (screenPoint.getY() - y);
            if (distance < 100) return pos;
            pos++;
        }	    return -1;
    }




    public class MouseHandler extends MouseAdapter {
        public MouseHandler() {
        }
        public void mouseClicked(MouseEvent ev) {
            if (ev.getButton() == 3) {
                if (undoHistory.size() > 0)
                {
                    viewport = ((double[][])undoHistory.get(undoHistory.size() - 1));

                    undoHistory.remove(undoHistory.size() - 1);
                } else {
                    zoomToRegion(minX, maxY, maxX, minY);
                }
                repaint();
            }
        }

        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() != 1) return;
            selectedMarker = findSelectedPoint(ev.getX(), ev.getY());
            originalPoint = translatePointToXY(ev.getX(), ev.getY());
            if (selectedMarker >= 0) {
                changeMode = true;
                setCursor(Cursor.getPredefinedCursor(8));
            } else {
                scaleMode = true;
                setCursor(Cursor.getPredefinedCursor(5));
                selectionRect.setFrame(ev.getX(), ev.getY(), 1.0D, 1.0D);
            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() != 1) return;

            setCursor(Cursor.getPredefinedCursor(0));
            if (changeMode) {
                changeMode = false;
            } else {
                scaleMode = false;
                double[] finalPoint = translatePointToXY(ev.getX(), ev.getY());
                undoHistory.add(viewport);
                viewport = new double[2][2];
                zoomToRegion(originalPoint[0], originalPoint[1], finalPoint[0], finalPoint[1]);
                repaint();
            }
        }
    }

    // Оброботчик движения мыши
    public class MouseMotionHandler implements MouseMotionListener {

        public void mouseDragged(MouseEvent ev) {
            if (changeMode) {
                //Добавить поворот (при)
                double[] currentPoint = translatePointToXY(ev.getX(), ev.getY());
                double newY = ((Double[])graphicsData.get(selectedMarker))[1].doubleValue() +
                        (currentPoint[1] - ((Double[])graphicsData.get(selectedMarker))[1].doubleValue());
                if (newY > viewport[0][1]) {
                    newY = viewport[0][1];
                }
                if (newY < viewport[1][1]) {
                    newY = viewport[1][1];
                }
                ((Double[])graphicsData.get(selectedMarker))[1] = Double.valueOf(newY);
                repaint();
            } else {
                double width = ev.getX() - selectionRect.getX();
                if (width < 5.0D) {
                    width = 5.0D;
                }
                double height = ev.getY() - selectionRect.getY();
                if (height < 5.0D) {
                    height = 5.0D;
                }
                selectionRect.setFrame(selectionRect.getX(), selectionRect.getY(), width, height);
                repaint();
            }
        }

        //перемещения мыши
        public void mouseMoved(MouseEvent ev) {
            selectedMarker = findSelectedPoint(ev.getX(), ev.getY());
            if (selectedMarker >= 0)
                setCursor(Cursor.getPredefinedCursor(8));
            else {
                setCursor(Cursor.getPredefinedCursor(0));
            }
            repaint();
        }

    }

}