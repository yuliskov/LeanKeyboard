package com.liskovsoft.inputchooser;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckedTextView;
import com.liskovsoft.keyboardaddons.KeyboardInfo;
import com.liskovsoft.keyboardaddons.reslangfactory.ResKeyboardInfo;
import com.liskovsoft.leankeykeyboard.R;

import java.util.ArrayList;
import java.util.List;

public class ChooseKeyboardDialog implements OnClickListener {
    private final View mInputView;
    private final Context mContext;
    private final List<KeyboardInfo> mInfos;
    private AlertDialog alertDialog;
    private ArrayList<CheckedTextView> mLangViews;

    /**
     * Main constructor. Use it in most of the cases.
     * @param ctx context
     */
    public ChooseKeyboardDialog(Context ctx) {
        this(ctx, null);
    }

    /**
     * Special constructor. Useful when others didn't work. E.g. when running dialog within input method.
     * @param ctx context
     * @param inputView view to get token
     */
    public ChooseKeyboardDialog(Context ctx, View inputView) {
        mContext = ctx;
        mInfos = ResKeyboardInfo.getAllKeyboardInfos(ctx);
        mInputView = inputView;
    }

    public void run() {
        showDialog();
    }

    @TargetApi(17)
    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        alertDialog = builder
                .setTitle(R.string.language_dialog_title)
                .setView(buildView(builder.getContext()))
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        ResKeyboardInfo.updateAllKeyboardInfos(mContext, mInfos);
                    }
                })
                .create();
        if (mInputView != null) {
            initDialog(); // prepare to run from IME
        }
        alertDialog.show();
    }

    private void initDialog() {
        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = mInputView.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    @SuppressLint("InflateParams")
    private View buildView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.lang_selection_dialog, null);
        ViewGroup root = view.findViewById(R.id.root);

        TypedArray attributeArray = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        int selectableItemBackgroundResourceId = attributeArray.getResourceId(0, 0);
        attributeArray.recycle();

        mLangViews = new ArrayList<>();

        for (KeyboardInfo info : mInfos) {
            CheckedTextView langView = (CheckedTextView) inflater.inflate(android.R.layout.simple_list_item_multiple_choice, root, false);
            langView.setBackgroundResource(selectableItemBackgroundResourceId);
            langView.setText(info.getLangName());

            langView.setFocusable(true);
            langView.setTag(info); // our TAG
            langView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.text_size_dp));
            langView.setOnClickListener(this);
            mLangViews.add(langView);
            root.addView(langView);
        }

        updateViews();

        return view;
    }

    private void updateViews() {
        for (CheckedTextView view : mLangViews) {
            KeyboardInfo kbd = (KeyboardInfo) view.getTag();
            if (kbd.isEnabled()) {
                view.setChecked(true);
            }
        }
    }

    @Override
    public void onClick(View view) {
        KeyboardInfo kbd = (KeyboardInfo) view.getTag();
        // todo
        CheckedTextView checkedView = (CheckedTextView) view;
        boolean checked = checkedView.isChecked();
        kbd.setEnabled(!checked);
        checkedView.setChecked(!checked);
    }
}
