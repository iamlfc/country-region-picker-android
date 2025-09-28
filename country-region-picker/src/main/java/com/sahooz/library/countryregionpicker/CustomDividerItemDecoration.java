package com.sahooz.library.countryregionpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;

public class CustomDividerItemDecoration extends RecyclerView.ItemDecoration {

    private final int leftPadding;
    private final int rightPadding;
    private final int dividerHeight;
    private final Paint paint;

    public CustomDividerItemDecoration(Context context, int leftPaddingDp, int rightPaddingDp, double dividerHeightDp, @ColorInt int dividerColor) {
        this.leftPadding = dp2px(context, leftPaddingDp);
        this.rightPadding = dp2px(context, rightPaddingDp);
        this.dividerHeight = dp2px(context, dividerHeightDp);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(dividerColor);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int childCount = parent.getChildCount();
        int left = parent.getPaddingLeft() + leftPadding;
        int right = parent.getWidth() - parent.getPaddingRight() - rightPadding;

        for (int i = 0; i < childCount - 1; i++) { // 最后一项不画分割线
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + dividerHeight;

            c.drawRect(left, top, right, bottom, paint);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) < parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = dividerHeight;
        }
    }

    private int dp2px(Context context, double dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
