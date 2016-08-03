package com.example.qiyue.simpledraglayout_two;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.nineoldandroids.view.ViewHelper;

/**
 * Created by qiyue on 2016/7/29 0029.
 */
public class DragLayout extends ViewGroup {


    private int mScreenWidth;
    private int mScreenHeight;
    private Scroller mScroller;
    private boolean isOpen;
    private ViewGroup mMenu;
    private ViewGroup mContent;
    private int mMenuWidth;
    private int mContentWidth;
    private int mMenuRightPadding;
    private float scale;

    private int mLastX;
    private int mLastY;
    private int mLastXIntercept;
    private int mLastYIntercept;

    public DragLayout(Context context) {
        this(context,null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        /**
         * 获取屏幕的宽和高
         */
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScroller = new Scroller(context);
        isOpen = false;
        mMenuRightPadding = convertToDp(context,100);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        /**
         * 摆放Menu的位置，强制摆放到刚好全部出去，然后调用ScrollBy移动整个布局
         *
         */
        mMenu.layout(-mMenuWidth, 0, 0, mScreenHeight);
        //摆放Content的位置
        mContent.layout(0, 0, mScreenWidth, mScreenHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMenu = (ViewGroup) getChildAt(0);
        mContent = (ViewGroup) getChildAt(1);
        /**
         * 必须测量child,否则直接layout 会不显示，因此自定义ViewGroup一定注意的(自定义LinearLayout就不需要)，
         * 测量后，再layout
         */
        /**
         * 通过传入父类参数，来影响子类孩子测量，实际就是让父类的wrap_content 和match_parent对孩子起作用
         * 但是如果不调用setmeasureDimension ,效果是孩子起作用，但自身布局不起作用
         *
         * 总结，就是一定通过父类的参数来测量孩子，并且通过调用setMeasuredDimension来实现自身尺寸测量
         */
        measureChild(mMenu,widthMeasureSpec,heightMeasureSpec);
        measureChild(mContent, widthMeasureSpec, heightMeasureSpec);
       // mMenuWidth = mMenu.getLayoutParams().width = mScreenWidth - mMenuRightPadding;
        mMenuWidth = mMenu.getLayoutParams().width = mScreenWidth - mMenuRightPadding;
        mContentWidth = mContent.getLayoutParams().width = mScreenWidth;
        Log.i("qiyue",""+mMenu.getWidth());
        Log.i("qiyue","mMenuWidth="+mMenuWidth);
        Log.i("qiyue","mContentWidth="+mContentWidth);

        /***
         * 设置自身的测量
         */
        setMeasuredDimension(mMenuWidth+mContentWidth, mScreenHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastX = (int) event.getX();
                mLastY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int currentX = (int) event.getX();
                int currentY = (int) event.getY();
                /**
                 *  拿到x方向的偏移量
                 */
                int dx = currentX - mLastX;
                if (dx < 0){/**向左滑动**/
                    /**
                     * 边界控制，如果Menu已经完全显示，再滑动的话 Menu左侧就会出现白边了,进行边界控制
                     */
                    Log.i("qiyue","getScrollX()="+getScrollX());
                    if (getScrollX() + Math.abs(dx) >= 0) {
                        //直接移动到（0，0）位置，不会出现白边
                        scrollTo(0, 0);
                        mMenu.setTranslationX(0);
                    } else {
                        /**
                         *  其实这里dx还是-dx，大家不用刻意去记
                         大家可以先使用dx，然后运行一下，发现
                         移动的方向是相反的，那么果断这里加个负号就可以了
                         */
                        scrollBy(-dx, 0);
                        /**
                         * TranslationX 和margin 效果一样，这里动态的改变这个值，
                         * 一开始显示0，当滑动等于宽度时变成三分之二，也就刚好吻合，
                         */
                        mMenu.setTranslationX(2*(mMenuWidth+getScrollX())/3);
                        /**mContent垂直方向，根据百分比缩放**/
                        scrollScaleY();
                    }

                }else{/**向右滑动**/
                    /**边界控制，如果Content已经完全显示，再滑动的话*/
                    Log.i("qiyue","向右滑动getScrollX()="+getScrollX());
                    if (getScrollX() - dx <= -mMenuWidth) {
                        //直接移动到（-mMenuWidth,0）位置，不会出现白边
                        scrollTo(-mMenuWidth, 0);
                        mMenu.setTranslationX(0);

                    } else {
                        /**
                         * 整体移动，同时移动改变Menu位置，本身dx是个矢量带方向，因此这里不用变
                         */
                        scrollBy(-dx, 0);
                        /**
                         * TranslationX 和margin 效果一样，这里动态的改变这个值，
                         * 一开始显示三分之二，当滑动等于宽度时变成0，也就刚好吻合，
                         * 这样有一种并非一起动，而是覆盖的感觉
                         */
                        mMenu.setTranslationX(2*(mMenuWidth+getScrollX())/3);
                        /**mContent垂直方向，根据百分比缩放**/
                        scrollScaleY();
                    }

                }
                mLastX = currentX;
                mLastY = currentY;
                scale = Math.abs((float)getScrollX()) / (float) mMenuWidth;
                break;

            case MotionEvent.ACTION_UP:
                Log.i("qiyue","getScrollX()="+getScrollX()+"mMenuWidth="+mMenuWidth);
               if (!isOpen) {
                    if (getScrollX() < -mMenuWidth / 4) {//打开Menu
                      /**
                         * 调用startScroll方法移动，-mMenuWidth-getScrollx() 是剩余滑动距离
                         */
                        mScroller.startScroll(getScrollX(), 0, -mMenuWidth - getScrollX(), 0, 300);
                        /**设置一个已经打开的标识，当实现点击开关自动打开关闭功能时会用到**/
                        isOpen = true;
                        /**一定不要忘了调用这个方法重绘，否则没有动画效果**/
                        scrollEndScaleY();
                        invalidate();
                    } else {
                        /**
                         * 条件未达到，回弹
                         */
                        mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 300);
                        isOpen = false;
                        scaleReset();
                        invalidate();
                    }
                }else{
                    if (getScrollX() < (-mMenuWidth / 1)+(mMenuWidth/4)) {
                        mScroller.startScroll(getScrollX(), 0, -mMenuWidth - getScrollX(), 0, 300);
                        isOpen = true;
                        scrollEndScaleY();
                        invalidate();
                    } else {
                        /**
                         * 条件未达到，回弹
                         */
                        mScroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 300);
                        isOpen = false;
                        scaleReset();
                        invalidate();
                    }
                }

                break;
        }
        return true;
    }
    /**
     * computeScroll：主要功能是计算拖动的位移量、更新背景、设置要显示的屏幕。
     重写computeScroll()的原因
     调用startScroll()是不会有滚动效果的，只有在computeScroll()获取滚动情况，做出滚动的响应
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()){
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            Log.i("qiyue","computeScroll");
            mMenu.setTranslationX(2*(mMenuWidth+getScrollX())/3);
            invalidate();
        }
    }

    private void scrollScaleY(){
        float scrollXfloat = (float) getScrollX();
        float mMenuWidthfloat = (float)mMenuWidth;
        float percent = Math.abs((scrollXfloat/mMenuWidthfloat));
        Log.i("qiyue","percent="+percent);
        float f1 = (1 - (percent/4));
        ViewHelper.setScaleY(mContent, f1);
    }

    private void scrollEndScaleY(){
        ViewHelper.setScaleY(mContent, 0.75f);
    }

    private void scaleReset(){
        ViewHelper.setScaleY(mContent, 1);
    }

    /**
     * 将传进来的数转化为dp
     */
    private int convertToDp(Context context , int num){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,num,context.getResources().getDisplayMetrics());
    }

    /**
     * 关闭menu
     */
    private void closeMenu() {
        //也是使用startScroll方法，dx和dy的计算方法一样
        mScroller.startScroll(getScrollX(),0,-getScrollX(),0,500);
        invalidate();
        isOpen = false;
    }

    /**
     * 打开menu
     */
    private void openMenu() {
        mScroller.startScroll(getScrollX(),0,-mMenuWidth-getScrollX(),0,500);
        invalidate();
        isOpen = true;
    }


}
