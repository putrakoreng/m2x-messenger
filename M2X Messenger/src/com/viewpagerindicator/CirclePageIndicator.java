/*
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viewpagerindicator;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.sir_m2x.messenger.R;

/**
 * Draws circles (one for each view). The current view position is filled and
 * others are only stroked.
 */
public class CirclePageIndicator extends View implements PageIndicator {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private float mRadius;
    private final Paint mPaintStroke;
    private final Paint mPaintFill;
    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mListener;
    private int mCurrentPage;
    private int mSnapPage;
    private int mCurrentOffset;
    private int mScrollState;
    private int mPageSize;
    private int mOrientation;
    private boolean mCentered;
    private boolean mSnap;

    private static final int INVALID_POINTER = -1;

    private int mTouchSlop;
    private float mLastMotionX = -1;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDragging;


    public CirclePageIndicator(final Context context) {
        this(context, null);
    }

    public CirclePageIndicator(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.vpiCirclePageIndicatorStyle);
    }

    public CirclePageIndicator(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        //Load defaults from resources
        final Resources res = getResources();
        final int defaultFillColor = res.getColor(R.color.default_circle_indicator_fill_color);
        final int defaultOrientation = res.getInteger(R.integer.default_circle_indicator_orientation);
        final int defaultStrokeColor = res.getColor(R.color.default_circle_indicator_stroke_color);
        final float defaultStrokeWidth = res.getDimension(R.dimen.default_circle_indicator_stroke_width);
        final float defaultRadius = res.getDimension(R.dimen.default_circle_indicator_radius);
        final boolean defaultCentered = res.getBoolean(R.bool.default_circle_indicator_centered);
        final boolean defaultSnap = res.getBoolean(R.bool.default_circle_indicator_snap);

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CirclePageIndicator, defStyle, R.style.Widget_CirclePageIndicator);

        this.mCentered = a.getBoolean(R.styleable.CirclePageIndicator_centered, defaultCentered);
        this.mOrientation = a.getInt(R.styleable.CirclePageIndicator_orientation, defaultOrientation);
        this.mPaintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaintStroke.setStyle(Style.STROKE);
        this.mPaintStroke.setColor(a.getColor(R.styleable.CirclePageIndicator_strokeColor, defaultStrokeColor));
        this.mPaintStroke.setStrokeWidth(a.getDimension(R.styleable.CirclePageIndicator_strokeWidth, defaultStrokeWidth));
        this.mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.mPaintFill.setStyle(Style.FILL);
        this.mPaintFill.setColor(a.getColor(R.styleable.CirclePageIndicator_fillColor, defaultFillColor));
        this.mRadius = a.getDimension(R.styleable.CirclePageIndicator_radius, defaultRadius);
        this.mSnap = a.getBoolean(R.styleable.CirclePageIndicator_snap, defaultSnap);

        a.recycle();

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }


    public void setCentered(final boolean centered) {
        this.mCentered = centered;
        invalidate();
    }

    public boolean isCentered() {
        return this.mCentered;
    }

    public void setFillColor(final int fillColor) {
        this.mPaintFill.setColor(fillColor);
        invalidate();
    }

    public int getFillColor() {
        return this.mPaintFill.getColor();
    }

    public void setOrientation(final int orientation) {
        switch (orientation) {
            case HORIZONTAL:
            case VERTICAL:
                this.mOrientation = orientation;
                updatePageSize();
                requestLayout();
                break;

            default:
                throw new IllegalArgumentException("Orientation must be either HORIZONTAL or VERTICAL.");
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setStrokeColor(final int strokeColor) {
        this.mPaintStroke.setColor(strokeColor);
        invalidate();
    }

    public int getStrokeColor() {
        return this.mPaintStroke.getColor();
    }

    public void setStrokeWidth(final float strokeWidth) {
        this.mPaintStroke.setStrokeWidth(strokeWidth);
        invalidate();
    }

    public float getStrokeWidth() {
        return this.mPaintStroke.getStrokeWidth();
    }

    public void setRadius(final float radius) {
        this.mRadius = radius;
        invalidate();
    }

    public float getRadius() {
        return this.mRadius;
    }

    public void setSnap(final boolean snap) {
        this.mSnap = snap;
        invalidate();
    }

    public boolean isSnap() {
        return this.mSnap;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if (this.mViewPager == null)
			return;
        final int count = this.mViewPager.getAdapter().getCount();
        if (count == 0)
			return;

        int longSize;
        int longPaddingBefore;
        int longPaddingAfter;
        int shortPaddingBefore;
        if (this.mOrientation == HORIZONTAL) {
            longSize = getWidth();
            longPaddingBefore = getPaddingLeft();
            longPaddingAfter = getPaddingRight();
            shortPaddingBefore = getPaddingTop();
        } else {
            longSize = getHeight();
            longPaddingBefore = getPaddingTop();
            longPaddingAfter = getPaddingBottom();
            shortPaddingBefore = getPaddingLeft();
        }

        final float threeRadius = this.mRadius * 3;
        final float shortOffset = shortPaddingBefore + this.mRadius;
        float longOffset = longPaddingBefore + this.mRadius;
        if (this.mCentered)
			longOffset += ((longSize - longPaddingBefore - longPaddingAfter) / 2.0f) - ((count * threeRadius) / 2.0f);

        float dX;
        float dY;

        //Draw stroked circles
        for (int iLoop = 0; iLoop < count; iLoop++) {
            float drawLong = longOffset + (iLoop * threeRadius);
            if (this.mOrientation == HORIZONTAL) {
                dX = drawLong;
                dY = shortOffset;
            } else {
                dX = shortOffset;
                dY = drawLong;
            }
            canvas.drawCircle(dX, dY, this.mRadius, this.mPaintStroke);
        }

        //Draw the filled circle according to the current scroll
        float cx = (this.mSnap ? this.mSnapPage : this.mCurrentPage) * threeRadius;
        if (!this.mSnap && (this.mPageSize != 0))
			cx += (this.mCurrentOffset * 1.0f / this.mPageSize) * threeRadius;
        if (this.mOrientation == HORIZONTAL) {
            dX = longOffset + cx;
            dY = shortOffset;
        } else {
            dX = shortOffset;
            dY = longOffset + cx;
        }
        canvas.drawCircle(dX, dY, this.mRadius, this.mPaintFill);
    }

    @Override
	public boolean onTouchEvent(final android.view.MotionEvent ev) {
        if ((this.mViewPager == null) || (this.mViewPager.getAdapter().getCount() == 0))
			return false;

        final int action = ev.getAction();

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                this.mLastMotionX = ev.getX();
                break;

            case MotionEvent.ACTION_MOVE: {
                final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                final float x = MotionEventCompat.getX(ev, activePointerIndex);
                final float deltaX = x - this.mLastMotionX;

                if (!this.mIsDragging)
					if (Math.abs(deltaX) > this.mTouchSlop)
						this.mIsDragging = true;

                if (this.mIsDragging) {
                    if (!this.mViewPager.isFakeDragging())
						this.mViewPager.beginFakeDrag();

                    this.mLastMotionX = x;

                    this.mViewPager.fakeDragBy(deltaX);
                }

                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!this.mIsDragging) {
                    final int count = this.mViewPager.getAdapter().getCount();
                    final int width = getWidth();
                    final float halfWidth = width / 2f;
                    final float sixthWidth = width / 6f;

                    if ((this.mCurrentPage > 0) && (ev.getX() < halfWidth - sixthWidth)) {
                        this.mViewPager.setCurrentItem(this.mCurrentPage - 1);
                        return true;
                    } else if ((this.mCurrentPage < count - 1) && (ev.getX() > halfWidth + sixthWidth)) {
                        this.mViewPager.setCurrentItem(this.mCurrentPage + 1);
                        return true;
                    }
                }

                this.mIsDragging = false;
                this.mActivePointerId = INVALID_POINTER;
                if (this.mViewPager.isFakeDragging()) this.mViewPager.endFakeDrag();
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                final float x = MotionEventCompat.getX(ev, index);
                this.mLastMotionX = x;
                this.mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);
                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                if (pointerId == this.mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                this.mLastMotionX = MotionEventCompat.getX(ev, MotionEventCompat.findPointerIndex(ev, this.mActivePointerId));
                break;
        }

        return true;
    };

    @Override
    public void setViewPager(final ViewPager view) {
        if (view.getAdapter() == null)
			throw new IllegalStateException("ViewPager does not have adapter instance.");
        this.mViewPager = view;
        this.mViewPager.setOnPageChangeListener(this);
        updatePageSize();
        invalidate();
    }

    private void updatePageSize() {
        if (this.mViewPager != null)
			this.mPageSize = (this.mOrientation == HORIZONTAL) ? this.mViewPager.getWidth() : this.mViewPager.getHeight();
    }

    @Override
    public void setViewPager(final ViewPager view, final int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void setCurrentItem(final int item) {
        if (this.mViewPager == null)
			throw new IllegalStateException("ViewPager has not been bound.");
        this.mViewPager.setCurrentItem(item);
        this.mCurrentPage = item;
        invalidate();
    }

    @Override
    public void notifyDataSetChanged() {
        invalidate();
    }

    @Override
    public void onPageScrollStateChanged(final int state) {
        this.mScrollState = state;

        if (this.mListener != null)
			this.mListener.onPageScrollStateChanged(state);
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
        this.mCurrentPage = position;
        this.mCurrentOffset = positionOffsetPixels;
        updatePageSize();
        invalidate();

        if (this.mListener != null)
			this.mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(final int position) {
        if (this.mSnap || this.mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            this.mCurrentPage = position;
            this.mSnapPage = position;
            invalidate();
        }

        if (this.mListener != null)
			this.mListener.onPageSelected(position);
    }

    @Override
    public void setOnPageChangeListener(final ViewPager.OnPageChangeListener listener) {
        this.mListener = listener;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (this.mOrientation == HORIZONTAL)
			setMeasuredDimension(measureLong(widthMeasureSpec), measureShort(heightMeasureSpec));
		else
			setMeasuredDimension(measureShort(widthMeasureSpec), measureLong(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     *
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureLong(final int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if ((specMode == MeasureSpec.EXACTLY) || (this.mViewPager == null))
			//We were told how big to be
            result = specSize;
		else {
            //Calculate the width according the views count
            final int count = this.mViewPager.getAdapter().getCount();
            result = (int)(getPaddingLeft() + getPaddingRight()
                    + (count * 2 * this.mRadius) + (count - 1) * this.mRadius + 1);
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST)
				result = Math.min(result, specSize);
        }
        return result;
    }

    /**
     * Determines the height of this view
     *
     * @param measureSpec
     *            A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureShort(final int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY)
			//We were told how big to be
            result = specSize;
		else {
            //Measure the height
            result = (int)(2 * this.mRadius + getPaddingTop() + getPaddingBottom() + 1);
            //Respect AT_MOST value if that was what is called for by measureSpec
            if (specMode == MeasureSpec.AT_MOST)
				result = Math.min(result, specSize);
        }
        return result;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mCurrentPage = savedState.currentPage;
        this.mSnapPage = savedState.currentPage;
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPage = this.mCurrentPage;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPage;

        public SavedState(final Parcelable superState) {
            super(superState);
        }

        private SavedState(final Parcel in) {
            super(in);
            this.currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.currentPage);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(final Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(final int size) {
                return new SavedState[size];
            }
        };
    }
}
