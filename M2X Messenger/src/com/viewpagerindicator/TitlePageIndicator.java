/*
 * Copyright (C) 2011 Jake Wharton
 * Copyright (C) 2011 Patrik Akerfeldt
 * Copyright (C) 2011 Francisco Figueiredo Jr.
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

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.sir_m2x.messenger.R;

/**
 * A TitlePageIndicator is a PageIndicator which displays the title of left view
 * (if exist), the title of the current select view (centered) and the title of
 * the right view (if exist). When the user scrolls the ViewPager then titles are
 * also scrolled.
 */
public class TitlePageIndicator extends View implements PageIndicator {
    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the underline be fully faded. A value of 0.25 means that
     * halfway between the center of the screen and an edge.
     */
    private static final float SELECTION_FADE_PERCENTAGE = 0.25f;

    /**
     * Percentage indicating what percentage of the screen width away from
     * center should the selected text bold turn off. A value of 0.05 means
     * that 10% between the center and an edge.
     */
    private static final float BOLD_FADE_PERCENTAGE = 0.05f;

    /**
     * Interface for a callback when the center item has been clicked.
     */
    public static interface OnCenterItemClickListener {
        /**
         * Callback when the center item has been clicked.
         *
         * @param position Position of the current center item.
         */
        public void onCenterItemClick(int position);
    }

    public enum IndicatorStyle {
        None(0), Triangle(1), Underline(2);

        public final int value;

        private IndicatorStyle(final int value) {
            this.value = value;
        }

        public static IndicatorStyle fromValue(final int value) {
            for (IndicatorStyle style : IndicatorStyle.values())
				if (style.value == value)
					return style;
            return null;
        }
    }

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mListener;
    private TitleProvider mTitleProvider;
    private int mCurrentPage;
    private int mCurrentOffset;
    private int mScrollState;
    private final Paint mPaintText = new Paint();
    private boolean mBoldText;
    private int mColorText;
    private int mColorSelected;
    private Path mPath;
    private final Paint mPaintFooterLine = new Paint();
    private IndicatorStyle mFooterIndicatorStyle;
    private final Paint mPaintFooterIndicator = new Paint();
    private float mFooterIndicatorHeight;
    private float mFooterIndicatorUnderlinePadding;
    private float mFooterPadding;
    private float mTitlePadding;
    private float mTopPadding;
    /** Left and right side padding for not active view titles. */
    private float mClipPadding;
    private float mFooterLineHeight;

    private static final int INVALID_POINTER = -1;

    private int mTouchSlop;
    private float mLastMotionX = -1;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDragging;

    private OnCenterItemClickListener mCenterItemClickListener;


    public TitlePageIndicator(final Context context) {
        this(context, null);
    }

    public TitlePageIndicator(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.vpiTitlePageIndicatorStyle);
    }

    public TitlePageIndicator(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        //Load defaults from resources
        final Resources res = getResources();
        final int defaultFooterColor = res.getColor(R.color.default_title_indicator_footer_color);
        final float defaultFooterLineHeight = res.getDimension(R.dimen.default_title_indicator_footer_line_height);
        final int defaultFooterIndicatorStyle = res.getInteger(R.integer.default_title_indicator_footer_indicator_style);
        final float defaultFooterIndicatorHeight = res.getDimension(R.dimen.default_title_indicator_footer_indicator_height);
        final float defaultFooterIndicatorUnderlinePadding = res.getDimension(R.dimen.default_title_indicator_footer_indicator_underline_padding);
        final float defaultFooterPadding = res.getDimension(R.dimen.default_title_indicator_footer_padding);
        final int defaultSelectedColor = res.getColor(R.color.default_title_indicator_selected_color);
        final boolean defaultSelectedBold = res.getBoolean(R.bool.default_title_indicator_selected_bold);
        final int defaultTextColor = res.getColor(R.color.default_title_indicator_text_color);
        final float defaultTextSize = res.getDimension(R.dimen.default_title_indicator_text_size);
        final float defaultTitlePadding = res.getDimension(R.dimen.default_title_indicator_title_padding);
        final float defaultClipPadding = res.getDimension(R.dimen.default_title_indicator_clip_padding);
        final float defaultTopPadding = res.getDimension(R.dimen.default_title_indicator_top_padding);

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TitlePageIndicator, defStyle, R.style.Widget_TitlePageIndicator);

        //Retrieve the colors to be used for this view and apply them.
        this.mFooterLineHeight = a.getDimension(R.styleable.TitlePageIndicator_footerLineHeight, defaultFooterLineHeight);
        this.mFooterIndicatorStyle = IndicatorStyle.fromValue(a.getInteger(R.styleable.TitlePageIndicator_footerIndicatorStyle, defaultFooterIndicatorStyle));
        this.mFooterIndicatorHeight = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorHeight, defaultFooterIndicatorHeight);
        this.mFooterIndicatorUnderlinePadding = a.getDimension(R.styleable.TitlePageIndicator_footerIndicatorUnderlinePadding, defaultFooterIndicatorUnderlinePadding);
        this.mFooterPadding = a.getDimension(R.styleable.TitlePageIndicator_footerPadding, defaultFooterPadding);
        this.mTopPadding = a.getDimension(R.styleable.TitlePageIndicator_topPadding, defaultTopPadding);
        this.mTitlePadding = a.getDimension(R.styleable.TitlePageIndicator_titlePadding, defaultTitlePadding);
        this.mClipPadding = a.getDimension(R.styleable.TitlePageIndicator_clipPadding, defaultClipPadding);
        this.mColorSelected = a.getColor(R.styleable.TitlePageIndicator_selectedColor, defaultSelectedColor);
        this.mColorText = a.getColor(R.styleable.TitlePageIndicator_textColor, defaultTextColor);
        this.mBoldText = a.getBoolean(R.styleable.TitlePageIndicator_selectedBold, defaultSelectedBold);

        final float textSize = a.getDimension(R.styleable.TitlePageIndicator_textSize, defaultTextSize);
        final int footerColor = a.getColor(R.styleable.TitlePageIndicator_footerColor, defaultFooterColor);
        this.mPaintText.setTextSize(textSize);
        this.mPaintText.setAntiAlias(true);
        this.mPaintFooterLine.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mPaintFooterLine.setStrokeWidth(this.mFooterLineHeight);
        this.mPaintFooterLine.setColor(footerColor);
        this.mPaintFooterIndicator.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mPaintFooterIndicator.setColor(footerColor);

        a.recycle();

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }


    public int getFooterColor() {
        return this.mPaintFooterLine.getColor();
    }

    public void setFooterColor(final int footerColor) {
        this.mPaintFooterLine.setColor(footerColor);
        this.mPaintFooterIndicator.setColor(footerColor);
        invalidate();
    }

    public float getFooterLineHeight() {
        return this.mFooterLineHeight;
    }

    public void setFooterLineHeight(final float footerLineHeight) {
        this.mFooterLineHeight = footerLineHeight;
        this.mPaintFooterLine.setStrokeWidth(this.mFooterLineHeight);
        invalidate();
    }

    public float getFooterIndicatorHeight() {
        return this.mFooterIndicatorHeight;
    }

    public void setFooterIndicatorHeight(final float footerTriangleHeight) {
        this.mFooterIndicatorHeight = footerTriangleHeight;
        invalidate();
    }

    public float getFooterIndicatorPadding() {
        return this.mFooterPadding;
    }

    public void setFooterIndicatorPadding(final float footerIndicatorPadding) {
        this.mFooterPadding = footerIndicatorPadding;
        invalidate();
    }

    public IndicatorStyle getFooterIndicatorStyle() {
        return this.mFooterIndicatorStyle;
    }

    public void setFooterIndicatorStyle(final IndicatorStyle indicatorStyle) {
        this.mFooterIndicatorStyle = indicatorStyle;
        invalidate();
    }

    public int getSelectedColor() {
        return this.mColorSelected;
    }

    public void setSelectedColor(final int selectedColor) {
        this.mColorSelected = selectedColor;
        invalidate();
    }

    public boolean isSelectedBold() {
        return this.mBoldText;
    }

    public void setSelectedBold(final boolean selectedBold) {
        this.mBoldText = selectedBold;
        invalidate();
    }

    public int getTextColor() {
        return this.mColorText;
    }

    public void setTextColor(final int textColor) {
        this.mPaintText.setColor(textColor);
        this.mColorText = textColor;
        invalidate();
    }

    public float getTextSize() {
        return this.mPaintText.getTextSize();
    }

    public void setTextSize(final float textSize) {
        this.mPaintText.setTextSize(textSize);
        invalidate();
    }

    public float getTitlePadding() {
        return this.mTitlePadding;
    }

    public void setTitlePadding(final float titlePadding) {
        this.mTitlePadding = titlePadding;
        invalidate();
    }

    public float getTopPadding() {
        return this.mTopPadding;
    }

    public void setTopPadding(final float topPadding) {
        this.mTopPadding = topPadding;
        invalidate();
    }

    public float getClipPadding() {
        return this.mClipPadding;
    }

    public void setClipPadding(final float clipPadding) {
        this.mClipPadding = clipPadding;
        invalidate();
    }

    public void setTypeface(final Typeface typeface) {
        this.mPaintText.setTypeface(typeface);
        invalidate();
    }

    public Typeface getTypeface() {
        return this.mPaintText.getTypeface();
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

        //Calculate views bounds
        ArrayList<RectF> bounds = calculateAllBounds(this.mPaintText);

        //Make sure we're on a page that still exists
        if (this.mCurrentPage >= bounds.size())
			setCurrentItem(bounds.size()-1);

        final int countMinusOne = count - 1;
        final float halfWidth = getWidth() / 2f;
        final int left = getLeft();
        final float leftClip = left + this.mClipPadding;
        final int width = getWidth();
        final int height = getHeight();
        final int right = left + width;
        final float rightClip = right - this.mClipPadding;

        int page = this.mCurrentPage;
        float offsetPercent;
        if (this.mCurrentOffset <= halfWidth)
			offsetPercent = 1.0f * this.mCurrentOffset / width;
		else {
            page += 1;
            offsetPercent = 1.0f * (width - this.mCurrentOffset) / width;
        }
        final boolean currentSelected = (offsetPercent <= SELECTION_FADE_PERCENTAGE);
        final boolean currentBold = (offsetPercent <= BOLD_FADE_PERCENTAGE);
        final float selectedPercent = (SELECTION_FADE_PERCENTAGE - offsetPercent) / SELECTION_FADE_PERCENTAGE;

        //Verify if the current view must be clipped to the screen
        RectF curPageBound = bounds.get(this.mCurrentPage);
        float curPageWidth = curPageBound.right - curPageBound.left;
        if (curPageBound.left < leftClip)
			//Try to clip to the screen (left side)
            clipViewOnTheLeft(curPageBound, curPageWidth, left);
        if (curPageBound.right > rightClip)
			//Try to clip to the screen (right side)
            clipViewOnTheRight(curPageBound, curPageWidth, right);

        //Left views starting from the current position
        if (this.mCurrentPage > 0)
			for (int i = this.mCurrentPage - 1; i >= 0; i--) {
                RectF bound = bounds.get(i);
                //Is left side is outside the screen
                if (bound.left < leftClip) {
                    float w = bound.right - bound.left;
                    //Try to clip to the screen (left side)
                    clipViewOnTheLeft(bound, w, left);
                    //Except if there's an intersection with the right view
                    RectF rightBound = bounds.get(i + 1);
                    //Intersection
                    if (bound.right + this.mTitlePadding > rightBound.left) {
                        bound.left = rightBound.left - w - this.mTitlePadding;
                        bound.right = bound.left + w;
                    }
                }
            }
        //Right views starting from the current position
        if (this.mCurrentPage < countMinusOne)
			for (int i = this.mCurrentPage + 1 ; i < count; i++) {
                RectF bound = bounds.get(i);
                //If right side is outside the screen
                if (bound.right > rightClip) {
                    float w = bound.right - bound.left;
                    //Try to clip to the screen (right side)
                    clipViewOnTheRight(bound, w, right);
                    //Except if there's an intersection with the left view
                    RectF leftBound = bounds.get(i - 1);
                    //Intersection
                    if (bound.left - this.mTitlePadding < leftBound.right) {
                        bound.left = leftBound.right + this.mTitlePadding;
                        bound.right = bound.left + w;
                    }
                }
            }

        //Now draw views
        for (int i = 0; i < count; i++) {
            //Get the title
            RectF bound = bounds.get(i);
            //Only if one side is visible
            if ((bound.left > left && bound.left < right) || (bound.right > left && bound.right < right)) {
                final boolean currentPage = (i == page);
                //Only set bold if we are within bounds
                this.mPaintText.setFakeBoldText(currentPage && currentBold && this.mBoldText);

                //Draw text as unselected
                this.mPaintText.setColor(this.mColorText);
                canvas.drawText(this.mTitleProvider.getTitle(i), bound.left, bound.bottom + this.mTopPadding, this.mPaintText);

                //If we are within the selected bounds draw the selected text
                if (currentPage && currentSelected) {
                    this.mPaintText.setColor(this.mColorSelected);
                    this.mPaintText.setAlpha((int)((this.mColorSelected >>> 24) * selectedPercent));
                    canvas.drawText(this.mTitleProvider.getTitle(i), bound.left, bound.bottom + this.mTopPadding, this.mPaintText);
                }
            }
        }

        //Draw the footer line
        this.mPath = new Path();
        this.mPath.moveTo(0, height - this.mFooterLineHeight / 2f);
        this.mPath.lineTo(width, height - this.mFooterLineHeight / 2f);
        this.mPath.close();
        canvas.drawPath(this.mPath, this.mPaintFooterLine);

        switch (this.mFooterIndicatorStyle) {
            case Triangle:
                this.mPath = new Path();
                this.mPath.moveTo(halfWidth, height - this.mFooterLineHeight - this.mFooterIndicatorHeight);
                this.mPath.lineTo(halfWidth + this.mFooterIndicatorHeight, height - this.mFooterLineHeight);
                this.mPath.lineTo(halfWidth - this.mFooterIndicatorHeight, height - this.mFooterLineHeight);
                this.mPath.close();
                canvas.drawPath(this.mPath, this.mPaintFooterIndicator);
                break;

            case Underline:
                if (!currentSelected)
					break;

                RectF underlineBounds = bounds.get(page);
                this.mPath = new Path();
                this.mPath.moveTo(underlineBounds.left  - this.mFooterIndicatorUnderlinePadding, height - this.mFooterLineHeight);
                this.mPath.lineTo(underlineBounds.right + this.mFooterIndicatorUnderlinePadding, height - this.mFooterLineHeight);
                this.mPath.lineTo(underlineBounds.right + this.mFooterIndicatorUnderlinePadding, height - this.mFooterLineHeight - this.mFooterIndicatorHeight);
                this.mPath.lineTo(underlineBounds.left  - this.mFooterIndicatorUnderlinePadding, height - this.mFooterLineHeight - this.mFooterIndicatorHeight);
                this.mPath.close();

                this.mPaintFooterIndicator.setAlpha((int)(0xFF * selectedPercent));
                canvas.drawPath(this.mPath, this.mPaintFooterIndicator);
                this.mPaintFooterIndicator.setAlpha(0xFF);
                break;
        }
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
                    final float leftThird = halfWidth - sixthWidth;
                    final float rightThird = halfWidth + sixthWidth;
                    final float eventX = ev.getX();

                    if (eventX < leftThird) {
                        if (this.mCurrentPage > 0) {
                            this.mViewPager.setCurrentItem(this.mCurrentPage - 1);
                            return true;
                        }
                    } else if (eventX > rightThird) {
                        if (this.mCurrentPage < count - 1) {
                            this.mViewPager.setCurrentItem(this.mCurrentPage + 1);
                            return true;
                        }
                    }
					else //Middle third
					if (this.mCenterItemClickListener != null)
						this.mCenterItemClickListener.onCenterItemClick(this.mCurrentPage);
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

    /**
     * Set bounds for the right textView including clip padding.
     *
     * @param curViewBound
     *            current bounds.
     * @param curViewWidth
     *            width of the view.
     */
    private void clipViewOnTheRight(final RectF curViewBound, final float curViewWidth, final int right) {
        curViewBound.right = right - this.mClipPadding;
        curViewBound.left = curViewBound.right - curViewWidth;
    }

    /**
     * Set bounds for the left textView including clip padding.
     *
     * @param curViewBound
     *            current bounds.
     * @param curViewWidth
     *            width of the view.
     */
    private void clipViewOnTheLeft(final RectF curViewBound, final float curViewWidth, final int left) {
        curViewBound.left = left + this.mClipPadding;
        curViewBound.right = this.mClipPadding + curViewWidth;
    }

    /**
     * Calculate views bounds and scroll them according to the current index
     *
     * @param paint
     * @param currentIndex
     * @return
     */
    private ArrayList<RectF> calculateAllBounds(final Paint paint) {
        ArrayList<RectF> list = new ArrayList<RectF>();
        //For each views (If no values then add a fake one)
        final int count = this.mViewPager.getAdapter().getCount();
        final int width = getWidth();
        final int halfWidth = width / 2;
        for (int i = 0; i < count; i++) {
            RectF bounds = calcBounds(i, paint);
            float w = (bounds.right - bounds.left);
            float h = (bounds.bottom - bounds.top);
            bounds.left = (halfWidth) - (w / 2) - this.mCurrentOffset + ((i - this.mCurrentPage) * width);
            bounds.right = bounds.left + w;
            bounds.top = 0;
            bounds.bottom = h;
            list.add(bounds);
        }

        return list;
    }

    /**
     * Calculate the bounds for a view's title
     *
     * @param index
     * @param paint
     * @return
     */
    private RectF calcBounds(final int index, final Paint paint) {
        //Calculate the text bounds
        RectF bounds = new RectF();
        bounds.right = paint.measureText(this.mTitleProvider.getTitle(index));
        bounds.bottom = paint.descent() - paint.ascent();
        return bounds;
    }

    @Override
    public void setViewPager(final ViewPager view) {
        final PagerAdapter adapter = view.getAdapter();
        if (adapter == null)
			throw new IllegalStateException("ViewPager does not have adapter instance.");
        if (!(adapter instanceof TitleProvider))
			throw new IllegalStateException("ViewPager adapter must implement TitleProvider to be used with TitlePageIndicator.");
        this.mViewPager = view;
        this.mViewPager.setOnPageChangeListener(this);
        this.mTitleProvider = (TitleProvider)adapter;
        invalidate();
    }

    @Override
    public void setViewPager(final ViewPager view, final int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void notifyDataSetChanged() {
        invalidate();
    }

    /**
     * Set a callback listener for the center item click.
     *
     * @param listener Callback instance.
     */
    public void setOnCenterItemClickListener(final OnCenterItemClickListener listener) {
        this.mCenterItemClickListener = listener;
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
    public void onPageScrollStateChanged(final int state) {
        this.mScrollState = state;

        if (this.mListener != null)
			this.mListener.onPageScrollStateChanged(state);
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
        this.mCurrentPage = position;
        this.mCurrentOffset = positionOffsetPixels;
        invalidate();

        if (this.mListener != null)
			this.mListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(final int position) {
        if (this.mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            this.mCurrentPage = position;
            invalidate();
        }

        if (this.mListener != null)
			this.mListener.onPageSelected(position);
    }

    @Override
    public void setOnPageChangeListener(final ViewPager.OnPageChangeListener listener) {
        this.mListener = listener;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        //Measure our width in whatever mode specified
        final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        //Determine our height
        float height = 0;
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightSpecMode == MeasureSpec.EXACTLY)
			//We were told how big to be
            height = MeasureSpec.getSize(heightMeasureSpec);
		else {
            //Calculate the text bounds
            RectF bounds = new RectF();
            bounds.bottom = this.mPaintText.descent()-this.mPaintText.ascent();
            height = bounds.bottom - bounds.top + this.mFooterLineHeight + this.mFooterPadding + this.mTopPadding;
            if (this.mFooterIndicatorStyle != IndicatorStyle.None)
				height += this.mFooterIndicatorHeight;
        }
        final int measuredHeight = (int)height;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mCurrentPage = savedState.currentPage;
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
