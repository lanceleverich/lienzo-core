
package com.ait.lienzo.client.core.shape.wires.handlers.impl;

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.event.AttributesChangedEvent;
import com.ait.lienzo.client.core.event.AttributesChangedHandler;
import com.ait.lienzo.client.core.shape.Attributes;
import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.client.core.shape.IDrawable;
import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.shape.wires.AlignAndDistribute;
import com.ait.lienzo.client.core.shape.wires.WiresUtils;
import com.ait.lienzo.client.core.shape.wires.handlers.AlignAndDistributeControl;
import com.ait.lienzo.client.core.types.BoundingBox;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.tooling.common.api.flow.Flows;
import com.ait.tooling.nativetools.client.collection.NFastStringSet;
import com.ait.tooling.nativetools.client.event.HandlerRegistrationManager;
import com.google.gwt.event.shared.HandlerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ait.lienzo.client.core.AttributeOp.any;

public class AlignAndDistributeControlImpl implements AlignAndDistributeControl
{
    protected AlignAndDistribute                                   m_alignAndDistribute;

    protected IPrimitive<?>                                        m_shape;

    protected BoundingBox                                          m_box;

    protected boolean                                              m_isDraggable;

    protected boolean                                              m_isDragging;

    protected HandlerRegistrationManager                           m_attrHandlerRegs;

    protected HandlerRegistration                                  m_dragEndHandlerReg;

    protected AlignAndDistribute.AlignAndDistributeMatchesCallback m_alignAndDistributeMatchesCallback;

    protected double                                               m_startLeft;

    protected double                                               m_startTop;

    protected double                                               m_left;

    protected double                                               m_hCenter;

    protected double                                               m_right;

    protected double                                               m_top;

    protected double                                               m_vCenter;

    protected double                                               m_bottom;

    protected Set<AlignAndDistribute.DistributionEntry>            m_horizontalDistEntries;

    protected Set<AlignAndDistribute.DistributionEntry>            m_verticalDistEntries;

    private boolean                                                indexed;

    private final Flows.BooleanOp                                  m_bboxOp;

    private final Flows.BooleanOp                                  m_tranOp;

    private double                                                 m_leftOffset;

    private double                                                 m_topOffset;

    public AlignAndDistributeControlImpl(IPrimitive<?> shape, AlignAndDistribute alignAndDistribute, AlignAndDistribute.AlignAndDistributeMatchesCallback alignAndDistributeMatchesCallback, List<Attribute> attributes)
    {
        m_shape = shape;

        m_alignAndDistribute = alignAndDistribute;

        m_alignAndDistributeMatchesCallback = alignAndDistributeMatchesCallback;

        // circles xy are in centre, where as others are top left.
        // For this reason we must use getBoundingBox, which uses BoundingPoints underneath, when ensures the shape x/y is now top left.
        // use this to determine an offset used for later get x/y
        m_box = AlignAndDistribute.getBoundingBox(shape);
        m_leftOffset = shape.getX() - m_box.getX();
        m_topOffset = shape.getY() - m_box.getY();

        Point2D absLoc = WiresUtils.getLocation(shape);

        double left = absLoc.getX() + m_leftOffset;
        double right = left + m_box.getWidth();
        double top = absLoc.getY() + m_topOffset;
        double bottom = top + m_box.getHeight();

        captureHorizontalPositions(left, right);
        captureVerticalPositions(top, bottom);

        m_alignAndDistribute.indexOn(this);

        if (m_shape.isDraggable())
        {
            dragOn();
        }
        m_attrHandlerRegs = new HandlerRegistrationManager();

        final ArrayList<Attribute> temp = new ArrayList<Attribute>(attributes);

        temp.add(Attribute.X);

        temp.add(Attribute.Y);

        final NFastStringSet seen = new NFastStringSet();

        final ArrayList<Attribute> list = new ArrayList<Attribute>();

        for (Attribute attribute : temp)
        {
            if (null != attribute)
            {
                if (false == seen.contains(attribute.getProperty()))
                {
                    list.add(attribute);

                    seen.add(attribute.getProperty());
                }
            }
        }
        m_bboxOp = any(list);

        addHandlers(m_shape, list);

        m_tranOp = any(Attribute.ROTATION, Attribute.SCALE, Attribute.SHEAR);
    }

    private final AttributesChangedHandler ShapeAttributesChangedHandler = new AttributesChangedHandler()
    {
        @Override
        public void onAttributesChanged(AttributesChangedEvent event)
        {
            refresh(event.evaluate(m_tranOp), event.evaluate(m_bboxOp));
        }
    };

    public void addHandlers(IDrawable<?> drawable, ArrayList<Attribute> list)
    {
        for (Attribute attribute : list)
        {
            m_attrHandlerRegs.register(drawable.addAttributesChangedHandler(attribute, ShapeAttributesChangedHandler));
        }
        m_attrHandlerRegs.register(drawable.addAttributesChangedHandler(Attribute.ROTATION, ShapeAttributesChangedHandler));
        m_attrHandlerRegs.register(drawable.addAttributesChangedHandler(Attribute.SCALE, ShapeAttributesChangedHandler));
        m_attrHandlerRegs.register(drawable.addAttributesChangedHandler(Attribute.SHEAR, ShapeAttributesChangedHandler));
    }

    public boolean isIndexed()
    {
        return indexed;
    }

    public void setIndexed(boolean indexed)
    {
        this.indexed = indexed;
    }

    public Set<AlignAndDistribute.DistributionEntry> getHorizontalDistributionEntries()
    {
        if (m_horizontalDistEntries == null)
        {
            m_horizontalDistEntries = new HashSet<AlignAndDistribute.DistributionEntry>();
        }
        return m_horizontalDistEntries;
    }

    public Set<AlignAndDistribute.DistributionEntry> getVerticalDistributionEntries()
    {
        if (m_verticalDistEntries == null)
        {
            m_verticalDistEntries = new HashSet<AlignAndDistribute.DistributionEntry>();
        }
        return m_verticalDistEntries;
    }

    public IPrimitive<?> getShape()
    {
        return m_shape;
    }

    /**
     * This is a cached BoundingBox
     * @return
     */
    public BoundingBox getBoundingBox()
    {
        return m_box;
    }

    public double getLeft()
    {
        return m_left;
    }

    public double getHorizontalCenter()
    {
        return m_hCenter;
    }

    public double getRight()
    {
        return m_right;
    }

    public double getTop()
    {
        return m_top;
    }

    public double getVerticalCenter()
    {
        return m_vCenter;
    }

    public double getBottom()
    {
        return m_bottom;
    }

    public void capturePositions(double left, double right, double top, double bottom)
    {
        if (left != m_left || right != m_right)
        {
            captureHorizontalPositions(left, right);
        }

        if (top != m_top || bottom != m_bottom)
        {
            captureVerticalPositions(top, bottom);
        }
    }

    public void captureHorizontalPositions(double left, double right)
    {
        double width = m_box.getWidth();
        m_left = left;
        m_hCenter = m_left + (width / 2);
        m_right = right;
    }

    public void captureVerticalPositions(double top, double bottom)
    {
        double height = m_box.getHeight();
        m_top = top;
        m_vCenter = (m_top + (height / 2));
        m_bottom = bottom;
    }

    public void updateIndex()
    {

        // circles xy are in centre, where as others are top left.
        // For this reason we must use getBoundingBox, which uses BoundingPoints underneath, when ensures the shape x/y is now top left.
        // However getBoundingBox here is still relative to parent, so must offset against parent absolute xy
        Point2D absLoc = WiresUtils.getLocation(m_shape);

        double left = absLoc.getX() + m_leftOffset;
        double right = left + m_box.getWidth();
        double top = absLoc.getY() + m_topOffset;
        double bottom = top + m_box.getHeight();

        boolean leftChanged = left != m_left;
        boolean rightChanged = right != m_right;
        boolean topChanged = top != m_top;
        boolean bottomChanged = bottom != m_bottom;

        if (!leftChanged && !rightChanged && !topChanged && !bottomChanged)
        {
            // this can happen when the event batching triggers after a drag has stopped, but the event change was due to the dragging.
            // @dean REVIEW
            return;
        }

        //BoundingBox box = AlignAndDistribute.getBoundingBox(m_shape);
        updateIndex(leftChanged, rightChanged, topChanged, bottomChanged, left, right, top, bottom);
    }

    public void updateIndex(boolean leftChanged, boolean rightChanged, boolean topChanged, boolean bottomChanged, double left, double right, double top, double bottom)
    {
        if (leftChanged || rightChanged)
        {
            m_alignAndDistribute.removeHorizontalDistIndex(this);

            boolean hCenterChanged = (left + (m_box.getWidth() / 2) != m_hCenter);

            if (leftChanged)
            {
                m_alignAndDistribute.removeLeftAlignIndexEntry(this, m_left);
            }

            if (hCenterChanged)
            {
                m_alignAndDistribute.removeHCenterAlignIndexEntry(this, m_hCenter);
            }

            if (rightChanged)
            {
                m_alignAndDistribute.removeRightAlignIndexEntry(this, m_right);
            }

            captureHorizontalPositions(left, right);
            if (leftChanged)
            {
                m_alignAndDistribute.addLeftAlignIndexEntry(this, m_left);
            }

            if (hCenterChanged)
            {
                m_alignAndDistribute.addHCenterAlignIndexEntry(this, m_hCenter);
            }

            if (rightChanged)
            {
                m_alignAndDistribute.addRightAlignIndexEntry(this, m_right);
            }

            m_alignAndDistribute.buildHorizontalDistIndex(this);
        }

        if (topChanged || bottomChanged)
        {
            m_alignAndDistribute.removeVerticalDistIndex(this);

            boolean vCenterChanged = (top + (m_box.getHeight() / 2) != m_vCenter);

            if (topChanged)
            {
                m_alignAndDistribute.removeTopAlignIndexEntry(this, m_top);
            }

            if (vCenterChanged)
            {
                m_alignAndDistribute.removeVCenterAlignIndexEntry(this, m_vCenter);
            }

            if (bottomChanged)
            {
                m_alignAndDistribute.removeBottomAlignIndexEntry(this, m_bottom);
            }

            captureVerticalPositions(top, bottom);
            if (topChanged)
            {
                m_alignAndDistribute.addTopAlignIndexEntry(this, m_top);
            }

            if (vCenterChanged)
            {
                m_alignAndDistribute.addVCenterAlignIndexEntry(this, m_vCenter);
            }

            if (bottomChanged)
            {
                m_alignAndDistribute.addBottomAlignIndexEntry(this, m_bottom);
            }

            m_alignAndDistribute.buildVerticalDistIndex(this);
        }
    }

    public void dragOn()
    {
        m_isDraggable = true;
    }

    public void draggOff()
    {
        m_isDraggable = false;
    }

    public boolean isDraggable()
    {
        return m_isDraggable;
    }

    private final boolean hasComplexTransformAttributes()
    {
        final Attributes attr = AlignAndDistribute.getAttributes(m_shape);

        if (attr.hasComplexTransformAttributes())
        {
            final double r = attr.getRotation();

            if (r != 0)
            {
                return true;
            }
            final Point2D scale = attr.getScale();

            if (null != scale)
            {
                if ((scale.getX() != 1) || (scale.getY() != 1))
                {
                    return true;
                }
            }
            final Point2D shear = attr.getShear();

            if (null != shear)
            {
                if ((shear.getX() != 0) || (shear.getY() != 0))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void refresh()
    {
        refresh(true, true);
    }

    @Override
    public void refresh(boolean transforms, boolean attributes)
    {

        if (m_isDragging)
        {
            // ignore attribute changes while dragging
            return;
        }
        if (transforms)
        {
            boolean hasTransformations = hasComplexTransformAttributes();

            if (indexed && hasTransformations)
            {
                // Indexing cannot be done on transformed shapes
                // it's cheaper to just check if the attributes exist on the shape, than it is to test for attributes on the event
                m_alignAndDistribute.indexOff(this);
            }
            else if (!indexed && !hasTransformations)
            {
                // Indexing was turned off, but there are no more transformations, so turn it back on again
                m_alignAndDistribute.indexOn(this);
            }
        }
        boolean isDraggable = m_shape.isDraggable();

        if (!m_isDraggable && isDraggable)
        {
            // was off, now on
            dragOn();
        }
        else if (m_isDraggable && !isDraggable)
        {
            // was on, now on off
            draggOff();
        }
        if (indexed && attributes)
        {
            updateIndex();
        }
    }

    @Override
    public void dragStart()
    {
        // shapes being dragged must be removed from the index, so that they don't snap to themselves
        // Also removes all nested shapes.
        m_startLeft = m_left;
        m_startTop = m_top;

        m_isDragging = true;
        iterateAndRemoveIndex(m_shape);
    }

    @Override
    public void remove()
    {
        this.removeHandlerRegistrations();
    }

    public void iterateAndRemoveIndex(IPrimitive<?> prim)
    {
        indexOff(prim);
        if (prim instanceof Group)
        {
            for (IPrimitive<?> child : prim.asGroup().getChildNodes())
            {
                if (child instanceof Group)
                {
                    iterateAndRemoveIndex(child.asGroup());
                }
                else
                {
                    indexOff(child);
                }
            }
        }
    }

    public void indexOff(IPrimitive<?> child)
    {
        AlignAndDistributeControl handler = m_alignAndDistribute.getControlForShape(child.uuid());
        if (handler != null && handler.isIndexed())
        {
            m_alignAndDistribute.indexOffWithoutChangingStatus(handler);
        }
    }

    public static class ShapePair
    {
        private Group             parent;

        private IPrimitive<?>     child;

        AlignAndDistributeControl handler;

        public ShapePair(Group group, IPrimitive<?> child, AlignAndDistributeControl handler)
        {
            this.parent = group;
            this.child = child;
            this.handler = handler;
        }
    }

    public void removeChildrenIfIndexed(IPrimitive<?> prim, List<ShapePair> pairs)
    {
        for (IPrimitive<?> child : prim.asGroup().getChildNodes())
        {
            AlignAndDistributeControl handler = m_alignAndDistribute.getControlForShape(child.uuid());
            if (handler != null)
            {
                ShapePair pair = new ShapePair(prim.asGroup(), child, handler);
                pairs.add(pair);
                prim.asGroup().remove(child);
            }
            if (child instanceof Group)
            {
                removeChildrenIfIndexed(child.asGroup(), pairs);
            }
        }
    }

    private void indexOn(IPrimitive<?> shape)
    {
        AlignAndDistributeControl handler = m_alignAndDistribute.getControlForShape(shape.uuid());
        indexOn(handler);
    }

    private void indexOn(AlignAndDistributeControl handler)
    {
        if (handler != null && handler.isIndexed())
        {
            m_alignAndDistribute.indexOnWithoutChangingStatus(handler);
            handler.updateIndex();
        }
    }

    @Override
    public boolean dragAdjust(Point2D dxy)
    {
        if (!indexed)
        {
            // ignore adjustment if indexing is off
            return false;
        }

        double left = m_startLeft + dxy.getX();
        double top = m_startTop + dxy.getY();
        double width = m_box.getWidth();
        double height = m_box.getHeight();
        capturePositions(left, left + width, top, top + height);

        AlignAndDistribute.AlignAndDistributeMatches matches = m_alignAndDistribute.findNearestMatches(this, m_left, m_hCenter, m_right, m_top, m_vCenter, m_bottom);

        boolean recapture = false;

        if (m_alignAndDistribute.isSnap())
        {

            double xOffset = m_startLeft;
            double yOffset = m_startTop;

            // Adjust horizontal
            if (matches.getLeftList() != null)
            {
                dxy.setX(matches.getLeftPos() - xOffset);
                recapture = true;
            }
            else if (matches.getHorizontalCenterList() != null)
            {
                dxy.setX((matches.getHorizontalCenterPos() - (width / 2)) - xOffset);
                recapture = true;
            }
            else if (matches.getRightList() != null)
            {
                dxy.setX((matches.getRightPos() - width) - xOffset);
                recapture = true;
            }

            // Adjust Vertical
            if (matches.getTopList() != null)
            {
                dxy.setY(matches.getTopPos() - yOffset);
                recapture = true;
            }
            else if (matches.getVerticalCenterList() != null)
            {
                dxy.setY((matches.getVerticalCenterPos() - (height / 2)) - yOffset);
                recapture = true;
            }
            else if (matches.getBottomList() != null)
            {
                dxy.setY((matches.getBottomPos() - height) - yOffset);
                recapture = true;
            }

            // Adjust horizontal distribution
            if (matches.getLeftDistList() != null)
            {
                dxy.setX(matches.getLeftDistList().getFirst().getPoint() - width - xOffset);
                recapture = true;
            }
            else if (matches.getRightDistList() != null)
            {
                dxy.setX(matches.getRightDistList().getFirst().getPoint() - xOffset);
                recapture = true;
            }
            else if (matches.getHorizontalCenterDistList() != null)
            {
                dxy.setX(matches.getHorizontalCenterDistList().getFirst().getPoint() - (width / 2) - xOffset);
                recapture = true;
            }

            // Adjust vertical distribution
            if (matches.getTopDistList() != null)
            {
                dxy.setY(matches.getTopDistList().getFirst().getPoint() - height - yOffset);
                recapture = true;
            }
            else if (matches.getBottomDistList() != null)
            {
                dxy.setY(matches.getBottomDistList().getFirst().getPoint() - yOffset);
                recapture = true;
            }
            else if (matches.getVerticalCenterDistList() != null)
            {
                dxy.setY(matches.getVerticalCenterDistList().getFirst().getPoint() - (height / 2) - yOffset);
                recapture = true;
            }

            // it was adjusted, so recapture points
            if (recapture)
            {
                // can't use the original left and top vars, as they are before adjustment snap
                left = m_startLeft + dxy.getX();
                top = m_startTop + dxy.getY();
                width = m_box.getWidth();
                height = m_box.getHeight();
                capturePositions(left, left + width, top, top + height);
            }
        }

        if (m_alignAndDistribute.isDrawGuideLines())
        {
            m_alignAndDistributeMatchesCallback.call(matches);
        }

        return recapture;
    }

    @Override
    public void dragEnd()
    {
        m_isDragging = false;

        m_alignAndDistributeMatchesCallback.dragEnd();

        // We do not want the nested indexed shapes to impact the bounding box
        // so remove them, they will be added once the index has been made.
        List<ShapePair> pairs = new ArrayList<ShapePair>();
        removeChildrenIfIndexed(m_shape, pairs);

        indexOn(m_shape);

        // re-add the children, index before it adds the next nested child
        for (ShapePair pair : pairs)
        {
            pair.parent.add(pair.child);
            indexOn(pair.handler);
        }
    }

    private void removeDragHandlerRegistrations()
    {
        if (null != m_dragEndHandlerReg)
        {
            m_dragEndHandlerReg.removeHandler();

            m_dragEndHandlerReg = null;
        }
    }

    public void removeHandlerRegistrations()
    {
        if (null != m_attrHandlerRegs)
        {
            m_attrHandlerRegs.destroy();

            m_attrHandlerRegs = null;
        }
        removeDragHandlerRegistrations();
    }
}