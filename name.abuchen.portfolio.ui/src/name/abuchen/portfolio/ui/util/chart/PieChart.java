package name.abuchen.portfolio.ui.util.chart;

import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.ICircularSeries;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.model.Node;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.charts.IPieChart;

public class PieChart extends Chart // NOSONAR
{
    private PieChartToolTip tooltip;
    private IPieChart.ChartType chartType;
    private ILabelProvider labelProvider;

    public PieChart(Composite parent, IPieChart.ChartType chartType)
    {
        this(parent, chartType, new DefaultLabelProvider());
    }

    public PieChart(Composite parent, IPieChart.ChartType chartType, ILabelProvider labelProvider)
    {
        super(parent, SWT.NONE);
        this.chartType = chartType;
        this.labelProvider = labelProvider;

        setData(UIConstants.CSS.CLASS_NAME, "chart"); //$NON-NLS-1$ 

        if (IPieChart.ChartType.DONUT == chartType) {
            addListener(SWT.Paint, new Listener()
            {
                @Override
                public void handleEvent(Event event)
                {
                    // Set color in root node to background color
                    if (getSeriesSet().getSeries().length > 0) {
                        ICircularSeries<?> cs = (ICircularSeries<?>) getSeriesSet().getSeries()[0];
                        cs.getRootNode().setColor(getPlotArea().getBackground());
                    }
                }
            });
        }

        getPlotArea().addCustomPaintListener(new ICustomPaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                renderLabels(e);
            }
        });

        getLegend().setVisible(true);
        tooltip = new PieChartToolTip(this);
    }

    public PieChartToolTip getToolTip()
    {
        return tooltip;
    }

    /**
     * Allow to override pie slide label
     * @param labelProvider
     */
    public void setLabelProvider(ILabelProvider labelProvider)
    {
        if (labelProvider == null) {
            return;
        }
        this.labelProvider = labelProvider;
    }

    protected void renderLabels(PaintEvent e)
    {
        for(ISeries<?> series : getSeriesSet().getSeries()) {
            if(series instanceof ICircularSeries) {
                IAxis xAxis = (IAxis)getAxisSet().getXAxis(series.getXAxisId());
                IAxis yAxis = (IAxis)getAxisSet().getYAxis(series.getYAxisId());

                List<Node> nodes = ((ICircularSeries<?>)series).getRootNode().getChildren();
                if (!nodes.isEmpty()) {
                    for(Node node : nodes) {
                        renderNodeLabel(node, (ICircularSeries<?>) series, e.gc, xAxis, yAxis);
                    }
                }
            }
        }
        
    }

    protected void renderNodeLabel(Node node, ICircularSeries<?> series, GC gc, IAxis xAxis, IAxis yAxis)
    {
        // children drawn first as parent overrides it's section of drawing
        if(!node.getChildren().isEmpty()) {
            for(Node childNode : node.getChildren()) {
                renderNodeLabel(childNode, series, gc, xAxis, yAxis);
            }
        }
        if(node.isVisible() == false)
            return;

        int level = node.getLevel() - series.getRootNode().getLevel() + (chartType == IPieChart.ChartType.PIE ? 0 : 1);

        Font oldFont = gc.getFont();
        gc.setForeground(Colors.WHITE);

        int angleStart = node.getAngleBounds().x,
            angleWidth = (int) (node.getAngleBounds().y * 0.5);

        Point outerEnd = calcPixelCoord(xAxis, yAxis, level * 0.85, angleStart+angleWidth);

        FontDescriptor boldDescriptor = FontDescriptor.createFrom(gc.getFont()).setHeight(9);
        gc.setFont(boldDescriptor.createFont(getDisplay()));

        String label = labelProvider.getLabel(node);
        if (label != null) {
            Point textSize = gc.textExtent(label);
            gc.drawString(label, outerEnd.x - (textSize.x/2) , outerEnd.y - (textSize.y/2), true);
        }
        gc.setFont(oldFont);
    }

    private Point calcPixelCoord(IAxis xAxis, IAxis yAxis, double level, int angle)
    {
        double xCoordinate = level * Math.cos(Math.toRadians(angle));
        double yCoordinate = level * Math.sin(Math.toRadians(angle));
        int xPixelCoordinate = xAxis.getPixelCoordinate(xCoordinate);
        int yPixelCoordinate = yAxis.getPixelCoordinate(yCoordinate);
        return new Point(xPixelCoordinate, yPixelCoordinate);
    }

    public interface ILabelProvider
    {
        public String getLabel(Node node);
    }

    private static class DefaultLabelProvider implements ILabelProvider
    {
        @Override
        public String getLabel(Node node)
        {
            String percentString = null;
            double percent =  ((double)node.getAngleBounds().y / 360);
            if (percent > 0.025) {
                percentString = Values.Percent2.format(percent);
            }
            return percentString;
        }
    }

    public static final class PieColors
    {
        private static final int SIZE = 11;
        private static final float STEP = 360.0f / (float) SIZE;

        private static final float HUE = 262.3f;
        private static final float SATURATION = 0.464f;
        private static final float BRIGHTNESS = 0.886f;

        private int nextSlice = 0;

        public Color next()
        {
            float brightness = Math.min(1.0f, BRIGHTNESS + (0.05f * (nextSlice / (float) SIZE)));
            return Colors.getColor(new RGB((HUE + (STEP * nextSlice++)) % 360f, SATURATION, brightness));

        }
    }
}
